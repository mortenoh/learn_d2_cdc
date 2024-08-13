package com.example.demo.routes;

import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

@Component
public class PgReplicationSlotRoute extends RouteBuilder {

    @Override
    public void configure() throws Exception {
        from("pg-replication-slot://localhost:15432/dhis/test_slot:wal2json"
            + "?user=dhis"
            + "&password=dhis")
            .routeId("replication-slot")
            .log("Received event: ${body}")
            .process(exchange -> {
                String event = exchange.getIn().getBody(String.class);
                System.out.println("Received event: " + event);
            });
    }
}
