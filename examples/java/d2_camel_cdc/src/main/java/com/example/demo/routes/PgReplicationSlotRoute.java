package com.example.demo.routes;

import java.util.List;
import java.util.Map;
import lombok.Data;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jackson.JacksonDataFormat;
import org.springframework.stereotype.Component;

@Component
public class PgReplicationSlotRoute extends RouteBuilder {

    @Override
    public void configure() throws Exception {
        from("pg-replication-slot://localhost:15432/dhis/test_slot2:wal2json"
            + "?user=dhis"
            + "&password=dhis"
            + "&slotOptions.include-xids=true"
            + "&slotOptions.add-tables=public.organisationunit")
            .routeId("replication-slot")
            .filter().jsonpath("$.change[?(@.length() > 0)]")
            // .log("Received event: ${body}") // debug
            .unmarshal(new JacksonDataFormat(Changes.class))
            .process(exchange -> {
              Changes payload = exchange.getIn().getBody(Changes.class);
              System.out.println("Deserialized Payload: " + payload);
            });
    }
}

@Data
class Change {
  private String kind;
  private String schema;
  private String table;
  private List<String> columnnames;
  private List<String> columntypes;
  private List<String> columnvalues;
  private OldKeys oldkeys;
}

@Data
class OldKeys {
  private List<String> keynames;
  private List<String> keytypes;
  private List<Object> keyvalues;
}

@Data
class Changes {
  private Integer xid;
  private List<Change> change;
}
