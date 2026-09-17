# Stage 1: Build & Package
FROM maven:3.9-eclipse-temurin-21-alpine AS builder
WORKDIR /workspace

# Copy root pom and all submodules
COPY pom.xml ./
COPY common/pom.xml common/
COPY producer/pom.xml producer/
COPY consumer/pom.xml consumer/

# Build dependencies
COPY common/src common/src
COPY producer/src producer/src

# Compile common and producer, producing fat jar
RUN mvn clean package -DskipTests -pl common,producer -am

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

COPY --from=builder /workspace/producer/target/*.jar app.jar

ENV JAVA_TOOL_OPTIONS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

ENTRYPOINT ["java", "-jar", "app.jar"]