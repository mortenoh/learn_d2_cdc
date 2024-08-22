package com.example.demo.routes;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jackson.JacksonDataFormat;
import org.springframework.stereotype.Component;

enum ChaneType {
  CREATE,
  UPDATE
}

@Component
public class PgReplicationSlotRoute extends RouteBuilder {

  @Override
  public void configure() throws Exception {
    from("pg-replication-slot://localhost:15432/dhis/test_slot4:wal2json"
            + "?user=dhis"
            + "&password=dhis"
            + "&slotOptions.include-xids=true"
            + "&slotOptions.add-tables=public.organisationunit")
        .routeId("replication-slot")
        .filter()
        .jsonpath("$.change[?(@.length() > 0)]")
        .unmarshal(new JacksonDataFormat(Changes.class))
        .process(
            exchange -> {
              Changes changes = exchange.getIn().getBody(Changes.class);
              exchange.getIn().setBody(findChangeIds(changes));
            })
        .log("${body}");
  }

  private List<ChangeInfo> findChangeIds(Changes changes) {
    List<ChangeInfo> changeInfos = new ArrayList<>();

    for (Change change : changes.getChange()) {
      ChangeInfo changeInfo = new ChangeInfo();

      if (change.getKind().equals("insert")) {
        changeInfo.setType(ChaneType.CREATE);
      } else if (change.getKind().equals("update")) {
        changeInfo.setType(ChaneType.UPDATE);
      } else {
        continue;
      }

      int idIndex =
          change.getColumnnames().stream()
              .filter("uid"::equals)
              .findFirst()
              .map(change.getColumnnames()::indexOf)
              .orElse(-1);

      if (idIndex != -1) {
        changeInfo.setId(change.getColumnvalues().get(idIndex));
        changeInfos.add(changeInfo);
      }
    }

    return changeInfos;
  }
}

@Data
class ChangeInfo {
  private ChaneType type;
  private String id;
}

@Data
class Change {
  private String kind;
  private String schema;
  private String table;
  private List<String> columnnames = new ArrayList<>();
  private List<String> columntypes = new ArrayList<>();
  private List<String> columnvalues = new ArrayList<>();
  private OldKeys oldkeys;
}

@Data
class OldKeys {
  private List<String> keynames = new ArrayList<>();
  private List<String> keytypes = new ArrayList<>();
  private List<Object> keyvalues = new ArrayList<>();
}

@Data
class Changes {
  private Integer xid;
  private List<Change> change;
}
