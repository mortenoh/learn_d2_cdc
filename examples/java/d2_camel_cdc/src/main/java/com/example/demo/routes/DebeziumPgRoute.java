package com.example.demo.routes;

import org.apache.camel.builder.RouteBuilder;
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
        .log(
            "Database: ${headers.CamelDebeziumSourceMetadata[db]},"
                + " Table: ${headers.CamelDebeziumSourceMetadata[table]},"
                + " Operation: ${headers.CamelDebeziumOperation},"
                + " Key: ${headers.CamelDebeziumKey},"
                + " Change: ${body}");
  }
}
