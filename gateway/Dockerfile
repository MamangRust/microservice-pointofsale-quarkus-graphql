FROM maven:3.9.6-eclipse-temurin-21 AS builder
WORKDIR /build

COPY . .

RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /build/gateway/target/quarkus-app/ ./

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "quarkus-run.jar"]
