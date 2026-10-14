# Build one service image at a time:
# docker build --build-arg SERVICE_MODULE=catalog-service -t ecom/catalog-service ./backend
FROM maven:3.9-eclipse-temurin-21 AS build

ARG SERVICE_MODULE=catalog-service
WORKDIR /workspace

COPY pom.xml ./
COPY api-gateway/pom.xml api-gateway/pom.xml
COPY identity-service/pom.xml identity-service/pom.xml
COPY catalog-service/pom.xml catalog-service/pom.xml
COPY order-service/pom.xml order-service/pom.xml
COPY inventory-service/pom.xml inventory-service/pom.xml
COPY payment-service/pom.xml payment-service/pom.xml
COPY notification-service/pom.xml notification-service/pom.xml
RUN mvn -B -ntp -pl ${SERVICE_MODULE} -am dependency:go-offline

COPY . .
RUN mvn -B -ntp -pl ${SERVICE_MODULE} -am package -DskipTests

FROM eclipse-temurin:21-jre

ARG SERVICE_MODULE=catalog-service
WORKDIR /app
COPY --from=build /workspace/${SERVICE_MODULE}/target/${SERVICE_MODULE}-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
