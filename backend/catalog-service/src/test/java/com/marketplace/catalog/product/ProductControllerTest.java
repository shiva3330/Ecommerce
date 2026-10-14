package com.marketplace.catalog.product;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductController.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductService productService;

    @Test
    void listsProductsAtTheVersionedBoundary() throws Exception {
        UUID productId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        given(productService.listActiveProducts()).willReturn(List.of(
                new ProductResponse(productId, "Mechanical Keyboard", "Hot-swappable", new BigDecimal("89.99"), "USD")
        ));

        mockMvc.perform(get("/api/v1/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(productId.toString()))
                .andExpect(jsonPath("$[0].name").value("Mechanical Keyboard"))
                .andExpect(jsonPath("$[0].price").value(89.99));
    }

    @Test
    void returnsTheCompleteStorefrontProductShape() throws Exception {
        UUID productId = UUID.fromString("22222222-2222-4222-8222-222222222222");
        UUID sellerId = UUID.fromString("33333333-3333-4333-8333-333333333333");
        given(productService.listActiveProducts()).willReturn(List.of(
                new ProductResponse(
                        productId,
                        "Linen Throw",
                        "Textured and lightweight",
                        "Home",
                        new BigDecimal("86.00"),
                        "USD",
                        new BigDecimal("4.90"),
                        128,
                        14,
                        new SellerSummaryResponse(sellerId, "Harbor & Loom", new BigDecimal("4.90")),
                        List.of("linen", "giftable"),
                        true
                )
        ));

        mockMvc.perform(get("/api/v1/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(productId.toString()))
                .andExpect(jsonPath("$[0].name").value("Linen Throw"))
                .andExpect(jsonPath("$[0].description").value("Textured and lightweight"))
                .andExpect(jsonPath("$[0].category").value("Home"))
                .andExpect(jsonPath("$[0].price").value(86.00))
                .andExpect(jsonPath("$[0].currency").value("USD"))
                .andExpect(jsonPath("$[0].rating").value(4.90))
                .andExpect(jsonPath("$[0].reviewCount").value(128))
                .andExpect(jsonPath("$[0].inventory").value(14))
                .andExpect(jsonPath("$[0].seller.id").value(sellerId.toString()))
                .andExpect(jsonPath("$[0].seller.name").value("Harbor & Loom"))
                .andExpect(jsonPath("$[0].seller.rating").value(4.90))
                .andExpect(jsonPath("$[0].tags[0]").value("linen"))
                .andExpect(jsonPath("$[0].tags[1]").value("giftable"))
                .andExpect(jsonPath("$[0].featured").value(true));
    }
}
