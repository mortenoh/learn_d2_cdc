# PG Replication/WAL

```bash
# Create slot 'test_slot' on db 'postgres'
pg_recvlogical -d postgres --slot test_slot --create-slot -P wal2json -U dhis -W

# Start to listen on slot 'test_slot' on db 'postgres'
pg_recvlogical -d postgres --slot test_slot --start -o pretty-print=1 -o add-msg-prefixes=wal2json -f - -U dhis

# Drop slot 'test_slot' on db 'postgres'
pg_recvlogical -d postgres --slot test_slot --drop-slot
```

```sql
SELECT * FROM pg_create_logical_replication_slot('test_slot', 'test_decoding');
SELECT * FROM pg_create_logical_replication_slot('test_slot', 'wal2json');

SELECT * FROM pg_replication_slots;
SELECT pg_drop_replication_slot('test_slot');

SELECT * FROM pg_logical_slot_get_changes('test_slot', NULL, 2, 'include-xids', '0');
SELECT * FROM pg_logical_slot_peek_changes('test_slot', NULL, NULL, 'include-xids', '0');
```

```sql
CREATE PUBLICATION my_publication FOR ALL TABLES;
CREATE PUBLICATION my_publication FOR TABLE my_table;

CREATE SUBSCRIPTION my_subscription
CONNECTION 'host=localhost port=5432 dbname=postgres user=dhis password=dhis'
PUBLICATION my_publication;
```

```sql
CREATE TABLE table1_with_pk (a SERIAL, b VARCHAR(30), c TIMESTAMP NOT NULL, PRIMARY KEY(a, c));
CREATE TABLE table1_without_pk (a SERIAL, b NUMERIC(5,2), c TEXT);

BEGIN;
INSERT INTO table1_with_pk (b, c) VALUES('Backup and Restore', now());
INSERT INTO table1_with_pk (b, c) VALUES('Tuning', now());
INSERT INTO table1_with_pk (b, c) VALUES('Replication', now());
-- SELECT pg_logical_emit_message(true, 'wal2json', 'this message will be delivered');-
-- SELECT pg_logical_emit_message(true, 'pgoutput', 'this message will be filtered');
DELETE FROM table1_with_pk WHERE a < 3;
-- SELECT pg_logical_emit_message(false, 'wal2json', 'this non-transactional message will be delivered even if you rollback the transaction');

INSERT INTO table1_without_pk (b, c) VALUES(2.34, 'Tapir');
-- it is not added to stream because there isn't a pk or a replica identity
UPDATE table1_without_pk SET c = 'Anta' WHERE c = 'Tapir';
COMMIT;

DROP TABLE table1_with_pk;
DROP TABLE table1_without_pk;
```

```sql
SELECT * FROM pg_create_logical_replication_slot('test_slot', 'wal2json');
CREATE TABLE fruit (name TEXT);
INSERT INTO fruit (name) VALUES('Apple');
INSERT INTO fruit (name) VALUES('Orange');
INSERT INTO fruit (name) VALUES('Kiwi');
DELETE FROM fruit;
DROP TABLE fruit;
```

## Links

- https://github.com/toluaina/pgsync/blob/main/pgsync/base.py#L903
- https://techcommunity.microsoft.com/t5/azure-database-for-postgresql/change-data-capture-in-postgres-how-to-use-logical-decoding-and/ba-p/1396421
- https://www.linkedin.com/pulse/unlocking-power-wal2json-ubuntu-postgresql-ashok-sana
-
