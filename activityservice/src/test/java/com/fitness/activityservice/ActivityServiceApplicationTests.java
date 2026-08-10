package com.fitness.activityservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
    "spring.main.allow-bean-definition-overriding=true",
    "spring.cloud.config.enabled=false",
    "eureka.client.enabled=false",
    "rabbitmq.queue.name=dummy-queue",
    "rabbitmq.exchange.name=dummy-exchange",
    "rabbitmq.routing.key=dummy-routing",
    "spring.data.mongodb.uri=mongodb://localhost:27017/dummy"
})
class ActivityServiceApplicationTests {

    @Test
    void contextLoads() {
    }
}
