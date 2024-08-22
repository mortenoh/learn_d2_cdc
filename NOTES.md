# PG Logical Replication

```bash
pg_recvlogical -h localhost -p 15432 -U dhis -d dhis --slot test_slot --create-slot -P wal2json -W
pg_recvlogical -h localhost -p 15432 -U dhis -d dhis --slot test_slot --start -o pretty-print=1 -o add-msg-prefixes=wal2json -f -
pg_recvlogical -h localhost -p 15432 -U dhis -d dhis --slot test_slot --drop-slot
```

```sql
SELECT * FROM pg_create_logical_replication_slot('test_slot', 'wal2json');
SELECT pg_drop_replication_slot('test_slot');
SELECT * FROM pg_replication_slots;

SELECT * FROM pg_logical_slot_get_changes('test_slot', NULL, 2, 'include-xids', '0');
SELECT * FROM pg_logical_slot_peek_changes('test_slot', NULL, NULL, 'include-xids', '0');
```

```sql
BEGIN;
CREATE TABLE fruit (name TEXT);
INSERT INTO fruit (name) VALUES('Apple');
INSERT INTO fruit (name) VALUES('Orange');
INSERT INTO fruit (name) VALUES('Kiwi');
DROP TABLE fruit;
COMMIT;
```

## Links

- https://github.com/toluaina/pgsync/blob/main/pgsync/base.py#L903
- https://techcommunity.microsoft.com/t5/azure-database-for-postgresql/change-data-capture-in-postgres-how-to-use-logical-decoding-and/ba-p/1396421
- https://www.linkedin.com/pulse/unlocking-power-wal2json-ubuntu-postgresql-ashok-sana
