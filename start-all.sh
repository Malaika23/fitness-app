#!/bin/bash

# Boot Config Server
echo "Starting Config Server..."
java -jar configserver.jar &

while ! nc -z localhost 8888; do
  echo "Waiting for Config Server on port 8888..."
  sleep 2
done

# Boot Eureka Server
echo "Starting Eureka Server..."
java -jar eureka.jar &

while ! nc -z localhost 8761; do
  echo "Waiting for Eureka on port 8761..."
  sleep 2
done

# Boot other services in background
echo "Starting User Service..."
java -jar userservice.jar &

echo "Starting Activity Service..."
java -jar activityservice.jar &

echo "Starting AI Service..."
java -jar aiservice.jar &

# Boot Gateway in foreground to keep container running
echo "Starting API Gateway..."
exec java -jar gateway.jar
