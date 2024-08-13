#!/bin/bash
set -e

wait_for_postgres() {
    echo "Waiting for PostgreSQL to start..."
    until pg_isready -U dhis -d dhis; do
        echo "Postgres is unavailable - sleeping"
        sleep 1
    done
    echo "Postgres is up and running."
}

# Initial wait for PostgreSQL to be ready
wait_for_postgres

echo "Configuring PostgreSQL for replication..."

# Configure replication settings
cat >>/var/lib/postgresql/data/postgresql.conf <<EOF
wal_level = logical
max_wal_senders = 10
max_replication_slots = 10
max_wal_size = 1GB
min_wal_size = 80MB
EOF

echo "Replication setup complete."

# Restart PostgreSQL to apply changes
echo "Restarting PostgreSQL to apply configuration changes..."
pg_ctl -D /var/lib/postgresql/data -m fast -w restart

# Wait for PostgreSQL to be ready again after the restart
wait_for_postgres

echo "Importing database..."
zcat /docker-entrypoint-initdb.d/dhis.tgz | psql -U dhis -d dhis
echo "Database import complete."
