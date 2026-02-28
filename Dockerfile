# ===== STAGE 1: build =====
FROM maven:3.9.6-eclipse-temurin-21 AS build

WORKDIR /app
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .
RUN ./mvnw -B -q dependency:go-offline

COPY src src
RUN ./mvnw -B -q clean package -DskipTests

FROM eclipse-temurin:21.0.6_7-jre-jammy

RUN groupadd -r appuser && useradd -r -g appuser appuser

WORKDIR /app
COPY --from=build --chown=appuser:appuser /app/target/*.jar app.jar

ENV JAVA_OPTS="-XX:+UseParallelGC -Xms256m -Xmx512m -Djava.security.egd=file:/dev/./urandom"

HEALTHCHECK --interval=30s --timeout=5s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

EXPOSE 8080
USER appuser

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
