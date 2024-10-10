package com.example.demo.routes;

import java.util.Map;
import org.apache.avro.generic.GenericRecord;
import org.apache.camel.builder.RouteBuilder;
import org.hisp.dhis.api.model.v2_39_1.Metadata;
import org.springframework.stereotype.Component;

@Component
public class KafkaDbzEnrichRoute extends RouteBuilder {

  @Override
  public void configure() throws Exception {
    from("kafka:dbz.public.organisationunit?"
            + "brokers=localhost:9092"
            + "&groupId=myGroupId"
            + "&keyDeserializer=org.apache.kafka.common.serialization.StringDeserializer"
            + "&valueDeserializer=io.confluent.kafka.serializers.KafkaAvroDeserializer"
            + "&autoOffsetReset=earliest"
            + "&additionalProperties.schema.registry.url=http://localhost:8081")
        .process(
            x -> {
              GenericRecord record = x.getIn().getBody(GenericRecord.class);
              record = (GenericRecord) record.get("after");
              x.getIn().setBody(record.get("uid"));
            })
        .process(
            x -> {
              String body = x.getIn().getBody(String.class);
              Map<String, String> queryParams =
                  Map.of(
                      "attributes:filter",
                      "organisationUnitAttribute:eq:true",
                      "organisationUnits:filter",
                      "id:eq:" + body);
              x.getIn().setHeader("CamelDhis2.queryParams", queryParams);
            })
        .toD("dhis2://get/resource?path=metadata&client=#dhis2source")
        .unmarshal()
        .json(Metadata.class)
        .process(
            x -> {
              Metadata metadata = x.getIn().getBody(Metadata.class);
              x.getIn().setBody(metadata);
            })
        .to("dhis2://post/resource?path=metadata&inBody=resource&client=#dhis2target");
  }
}
