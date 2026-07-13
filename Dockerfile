# STAGE 1: Build stage
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /app

# Copy all directories
COPY configserver ./configserver
COPY eureka ./eureka
COPY userservice ./userservice
COPY activityservice ./activityservice
COPY aiservice ./aiservice
COPY gateway ./gateway

# Build all projects sequentially
RUN mvn -f configserver/pom.xml clean package -DskipTests
RUN mvn -f eureka/pom.xml clean package -DskipTests
RUN mvn -f userservice/pom.xml clean package -DskipTests
RUN mvn -f activityservice/pom.xml clean package -DskipTests
RUN mvn -f aiservice/pom.xml clean package -DskipTests
RUN mvn -f gateway/pom.xml clean package -DskipTests

# STAGE 2: Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Install bash and netcat for startup script polling
RUN apk add --no-cache bash

# Copy the built JAR files
COPY --from=build /app/configserver/target/*.jar configserver.jar
COPY --from=build /app/eureka/target/*.jar eureka.jar
COPY --from=build /app/userservice/target/*.jar userservice.jar
COPY --from=build /app/activityservice/target/*.jar activityservice.jar
COPY --from=build /app/aiservice/target/*.jar aiservice.jar
COPY --from=build /app/gateway/target/*.jar gateway.jar

# Copy startup script
COPY start-all.sh .
RUN chmod +x start-all.sh

# Expose microservices ports
EXPOSE 8888 8761 8081 8082 8083 8080

ENTRYPOINT ["./start-all.sh"]
