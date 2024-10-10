package com.example.demo.routes;

import org.apache.avro.generic.GenericRecord;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

// @Component
public class KafkaDbzRoute extends RouteBuilder {

  @Override
  public void configure() throws Exception {
    from("kafka:dbz.public.organisationunit?"
        + "brokers=localhost:9092"
        + "&groupId=myGroupId"
        + "&keyDeserializer=org.apache.kafka.common.serialization.StringDeserializer"
        + "&valueDeserializer=io.confluent.kafka.serializers.KafkaAvroDeserializer"
        + "&autoOffsetReset=earliest"
        + "&additionalProperties.schema.registry.url=http://localhost:8081")
        .process(exchange -> {
          GenericRecord record = exchange.getIn().getBody(GenericRecord.class);
          System.out.println("Received Avro record: " + record);
        })
        .log("Message received from Kafka : ${body}")
        .log("    on the topic ${headers[kafka.TOPIC]}")
        .log("    on the partition ${headers[kafka.PARTITION]}")
        .log("    with the offset ${headers[kafka.OFFSET]}")
        .log("    with the key ${headers[kafka.KEY]}");
  }
}
