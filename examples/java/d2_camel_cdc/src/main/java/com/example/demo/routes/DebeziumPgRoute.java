package com.example.demo.routes;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.debezium.DebeziumConstants;
import org.apache.kafka.connect.data.Struct;
import org.springframework.stereotype.Component;

@Component
public class DebeziumPgRoute extends RouteBuilder {

  @Override
  public void configure() throws Exception {
    from("debezium-postgres:localhost?"
            + "databaseHostname=localhost"
            + "&databasePort=15432"
            + "&databaseUser=dhis"
            + "&databasePassword=dhis"
            + "&databaseDbname=dhis"
            + "&schemaIncludeList=public"
            + "&tableIncludeList=public.organisationunit"
            + "&columnIncludeList=.*\\.uid"
            + "&pluginName=pgoutput"
            + "&slotName=slot1"
            + "&topicPrefix=prefix1"
            + "&offsetStorageFileName=/tmp/offset.dat")
        .process(
            ex -> {
              @SuppressWarnings("unchecked")
              Map<String, ?> headerSourceMetadata =
                  ex.getIn().getHeader(DebeziumConstants.HEADER_SOURCE_METADATA, Map.class);

              String db = (String) headerSourceMetadata.get("db");
              String schema = (String) headerSourceMetadata.get("schema");
              String table = (String) headerSourceMetadata.get("table");
              String operation =
                  ex.getIn().getHeader(DebeziumConstants.HEADER_OPERATION, String.class);
              Struct key = ex.getIn().getHeader(DebeziumConstants.HEADER_KEY, Struct.class);
              Struct value = ex.getIn().getBody(Struct.class);

              DebeziumChangeEvent event =
                  new DebeziumChangeEvent(db, schema, table, operation, key, value);
              ex.getIn().setBody(event);
            })
        .marshal()
        .json(DebeziumChangeEvent.class)
        .log("${body}");
  }
}

// TODO there should be something similar to this in kafka already
class StructSerializer extends JsonSerializer<Struct> {
  @Override
  public void serialize(Struct struct, JsonGenerator gen, SerializerProvider serializers)
      throws IOException {
    Map<String, Object> map = new HashMap<>();
    struct.schema().fields().forEach(field -> map.put(field.name(), struct.get(field)));
    gen.writeObject(map);
  }
}

@Data
@AllArgsConstructor
class DebeziumChangeEvent {
  private String database;
  private String schema;
  private String table;
  private String operation;

  @JsonSerialize(using = StructSerializer.class)
  private Struct key;

  @JsonSerialize(using = StructSerializer.class)
  private Struct value;
}
