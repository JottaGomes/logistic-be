# Build stage
FROM maven:3.9-eclipse-temurin-17-alpine AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -q
COPY src ./src
RUN mvn clean package -DskipTests -q

# Runtime stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/logistics-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8333
ENV SERVER_PORT=8333
ENTRYPOINT ["java", "-jar", "app.jar"]
