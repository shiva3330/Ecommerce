package com.marketplace.order;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketplace.order.api.CreateOrderRequest;
import com.marketplace.order.api.OrderLineRequest;
import com.marketplace.order.application.OrderApplicationService;
import com.marketplace.order.messaging.OutboxDispatcher;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(classes = OrderServiceApplication.class)
@AutoConfigureMockMvc
class OrderServicePostgresKafkaIntegrationTest {

    private static final String TOPIC = "order.placed.integration.v1";
    private static final String BODY = "{\"customerId\":\"customer-integration\",\"items\":[{\"productId\":\"11111111-1111-1111-1111-111111111111\",\"quantity\":2}]}";

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("order_db")
            .withUsername("order")
            .withPassword("order");

    @Container
    static final KafkaContainer KAFKA = new KafkaContainer(
            DockerImageName.parse("apache/kafka:3.8.0")
    );

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OutboxDispatcher outboxDispatcher;

    @Autowired
    private OrderApplicationService orderApplicationService;

    @DynamicPropertySource
    static void registerContainerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
        registry.add("app.kafka.topics.order-placed", () -> TOPIC);
        registry.add("app.outbox.scheduler.enabled", () -> "false");
    }

    @BeforeAll
    static void createKafkaTopic() throws Exception {
        Properties properties = new Properties();
        properties.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers());
        try (AdminClient adminClient = AdminClient.create(properties)) {
            adminClient.createTopics(List.of(new NewTopic(TOPIC, 1, (short) 1))).all().get();
        }
    }

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.execute("TRUNCATE TABLE outbox_events, order_items, orders");
    }

    @Test
    void createsReadsAndReplaysTheSameDurableOrder() throws Exception {
        MvcResult created = create("replay-key", BODY);
        String orderId = json(created).get("orderId").asText();

        mockMvc.perform(get("/api/v1/orders/{id}", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(orderId))
                .andExpect(jsonPath("$.customerId").value("customer-integration"))
                .andExpect(jsonPath("$.items[0].quantity").value(2))
                .andExpect(jsonPath("$.createdAt").isNotEmpty());

        MvcResult replay = create("replay-key", BODY);
        assertThat(json(replay).get("orderId").asText()).isEqualTo(orderId);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM orders", Integer.class)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM outbox_events", Integer.class)).isEqualTo(1);
    }

    @Test
    void returnsAStableConflictForTheSameCustomerAndKeyWithDifferentPayload() throws Exception {
        create("conflict-key", BODY);
        String differentBody = BODY.replace("\"quantity\":2", "\"quantity\":3");

        mockMvc.perform(post("/api/v1/orders")
                        .header("Idempotency-Key", "conflict-key")
                        .contentType(APPLICATION_JSON)
                        .content(differentBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("IDEMPOTENCY_KEY_REUSE"));
    }

    @Test
    void concurrentCreatesWithOneCustomerKeyProduceOneOrder() throws Exception {
        int requestCount = 8;
        ExecutorService executor = Executors.newFixedThreadPool(requestCount);
        CountDownLatch ready = new CountDownLatch(requestCount);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Callable<MvcResult>> tasks = new ArrayList<>();
            for (int index = 0; index < requestCount; index++) {
                tasks.add(() -> {
                    ready.countDown();
                    start.await(10, TimeUnit.SECONDS);
                    return mockMvc.perform(post("/api/v1/orders")
                                    .header("Idempotency-Key", "concurrent-key")
                                    .contentType(APPLICATION_JSON)
                                    .content(BODY))
                            .andReturn();
                });
            }
            List<Future<MvcResult>> futures = new ArrayList<>();
            for (Callable<MvcResult> task : tasks) {
                futures.add(executor.submit(task));
            }
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            List<String> orderIds = new ArrayList<>();
            for (Future<MvcResult> future : futures) {
                MvcResult result = future.get(20, TimeUnit.SECONDS);
                assertThat(result.getResponse().getStatus()).isEqualTo(202);
                orderIds.add(json(result).get("orderId").asText());
            }
            assertThat(orderIds).containsOnly(orderIds.get(0));
            assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM orders", Integer.class)).isEqualTo(1);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void dispatchesThePersistedJsonAndUsesOrderIdAsKafkaKey() throws Exception {
        String orderId = json(create("delivery-key", BODY)).get("orderId").asText();
        try (KafkaConsumer<String, String> consumer = newConsumer()) {
            consumer.subscribe(List.of(TOPIC));
            List<ConsumerRecord<String, String>> records = new ArrayList<>();
            outboxDispatcher.dispatchOnce();

            long deadline = System.nanoTime() + Duration.ofSeconds(15).toNanos();
            while (records.isEmpty() && System.nanoTime() < deadline) {
                consumer.poll(Duration.ofMillis(250)).forEach(records::add);
            }

            assertThat(records).hasSize(1);
            assertThat(records.get(0).key()).isEqualTo(orderId);
            JsonNode event = objectMapper.readTree(records.get(0).value());
            assertThat(event.get("eventId").asText()).isNotBlank();
            assertThat(event.get("orderId").asText()).isEqualTo(orderId);
            assertThat(jdbcTemplate.queryForObject(
                    "SELECT published_at IS NOT NULL FROM outbox_events WHERE aggregate_id = ?",
                    Boolean.class,
                    UUID.fromString(orderId)
            )).isTrue();
        }
    }

    @Test
    void rollsBackTheOrderAndItemsWhenOutboxInsertFails() {
        jdbcTemplate.execute("""
                CREATE OR REPLACE FUNCTION fail_order_outbox_insert()
                RETURNS trigger
                LANGUAGE plpgsql
                AS $$
                BEGIN
                    RAISE EXCEPTION 'forced outbox failure';
                END;
                $$
                """);
        jdbcTemplate.execute("""
                CREATE TRIGGER fail_order_outbox_insert_trigger
                BEFORE INSERT ON outbox_events
                FOR EACH ROW EXECUTE FUNCTION fail_order_outbox_insert()
                """);
        try {
            CreateOrderRequest request = new CreateOrderRequest(
                    "customer-rollback",
                    List.of(new OrderLineRequest(
                            UUID.fromString("11111111-1111-1111-1111-111111111111"),
                            2
                    ))
            );
            assertThatThrownBy(() -> orderApplicationService.placeOrder(request, "rollback-key"))
                    .isInstanceOf(RuntimeException.class);
        } finally {
            jdbcTemplate.execute("DROP TRIGGER fail_order_outbox_insert_trigger ON outbox_events");
            jdbcTemplate.execute("DROP FUNCTION fail_order_outbox_insert()");
        }

        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM orders", Integer.class)).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM order_items", Integer.class)).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM outbox_events", Integer.class)).isZero();
    }

    private MvcResult create(String key, String body) throws Exception {
        return mockMvc.perform(post("/api/v1/orders")
                        .header("Idempotency-Key", key)
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isAccepted())
                .andReturn();
    }

    private JsonNode json(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private KafkaConsumer<String, String> newConsumer() {
        Properties properties = new Properties();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers());
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, "order-integration-" + UUID.randomUUID());
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        return new KafkaConsumer<>(properties);
    }
}
