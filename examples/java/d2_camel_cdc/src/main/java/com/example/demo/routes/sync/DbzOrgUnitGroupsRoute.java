package com.example.demo.routes.sync;

import org.apache.avro.generic.GenericRecord;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.kafka.KafkaConstants;
import org.springframework.stereotype.Component;

@Component
public class DbzOrgUnitGroupsRoute extends RouteBuilder {

  @Override
  public void configure() throws Exception {
    from("kafka:dbz.public.attribute,dbz.public.organisationunit,dbz.public.orgunitgroup?"
            + "brokers=localhost:9092"
            + "&groupId=myGroupId"
            + "&keyDeserializer=org.apache.kafka.common.serialization.StringDeserializer"
            + "&valueDeserializer=io.confluent.kafka.serializers.KafkaAvroDeserializer"
            + "&autoOffsetReset=earliest"
            + "&additionalProperties.schema.registry.url=http://localhost:8081")
        .process(
            x -> {
              String topic = x.getIn().getHeader(KafkaConstants.TOPIC, String.class);
              GenericRecord record = x.getIn().getBody(GenericRecord.class);
              System.err.println("Topic: " + topic + ", Message: " + record);
            });
  }
}
