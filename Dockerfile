# ===== STAGE 1: build =====
FROM maven:3.9.6-eclipse-temurin-21 AS build

WORKDIR /app
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .
RUN ./mvnw -B -q dependency:go-offline

COPY src src
RUN ./mvnw -B -q clean package -DskipTests

# ===== STAGE 2: runtime =====
FROM eclipse-temurin:21-jre

WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
