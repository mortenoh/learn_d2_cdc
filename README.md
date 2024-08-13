# Postgresql to D2 logical replication

## Configuration

```sh
wal_level = logical
max_wal_senders = 10
max_replication_slots = 10
```

## Examples

```sql
-- https://github.com/eulerto/wal2json?tab=readme-ov-file#parameters
-- https://www.postgresql.org/docs/current/functions-admin.html#FUNCTIONS-REPLICATION

SELECT * FROM pg_create_logical_replication_slot('test_slot', 'wal2json');

SELECT * FROM pg_logical_slot_peek_changes('test_slot', $upto_lsn, $upto_nchanges);
SELECT * FROM pg_logical_slot_peek_changes('regression_slot', $upto_lsn, $upto_nchanges, 'option-x', 'val');

SELECT * FROM pg_logical_slot_get_changes('test_slot', $upto_lsn, $upto_nchanges);
SELECT * FROM pg_logical_slot_get_changes('regression_slot', $upto_lsn, $upto_nchanges, 'option-x', 'val');
```
