import argparse
import time

from sqlalchemy import create_engine, text


# Function to peek at changes from logical replication slot
def peek_logical_slot_changes(connection, slot_name, limit=None):
    query = text("SELECT * FROM pg_logical_slot_peek_changes(:slot_name, NULL, :limit);")
    result = connection.execute(query, {"slot_name": slot_name, "limit": limit})
    changes = result.fetchall()
    return changes


# Function to confirm changes from logical replication slot
def confirm_logical_slot_changes(connection, slot_name, upto_lsn):
    query = text("SELECT * FROM pg_logical_slot_get_changes(:slot_name, :upto_lsn, NULL);")
    connection.execute(query, {"slot_name": slot_name, "upto_lsn": upto_lsn})
    print(f"Confirmed up to LSN {upto_lsn} on slot {slot_name}")


# Function to continuously poll for changes and confirm them after processing
def continuously_poll_changes(engine, slot_name, interval=5, limit=10):
    print("Starting to poll for changes...")
    last_lsn = None
    while True:
        try:
            with engine.connect() as connection:
                # Peek at changes without advancing the slot
                changes = peek_logical_slot_changes(connection, slot_name, limit)
                if changes:
                    for change in changes:
                        print(f"Processing LSN {change[0]}")
                        time.sleep(1)  # Simulate processing time

                    # Confirm changes up to the latest LSN after processing
                    last_lsn = changes[-1][0]  # Assuming the LSN is the first field in the tuple
                    confirm_logical_slot_changes(connection, slot_name, last_lsn)
                else:
                    # If there are no changes, sleep for the interval period
                    time.sleep(interval)
        except Exception as e:
            print(f"An error occurred: {e}")
            time.sleep(interval)  # Sleep before retrying


def main():
    parser = argparse.ArgumentParser(
        description="Continuously poll and confirm changes from a PostgreSQL logical replication slot."
    )
    parser.add_argument("--host", type=str, default="localhost", help="Database host")
    parser.add_argument("--port", type=int, default=5432, help="Database port")
    parser.add_argument("--dbname", type=str, default="dhis", help="Database name")
    parser.add_argument("--user", type=str, default="dhis", help="Database user")
    parser.add_argument("--password", type=str, default="dhis", help="Database password")
    parser.add_argument("--slot_name", type=str, required=True, help="Logical replication slot name")
    parser.add_argument("--interval", type=int, default=5, help="Polling interval in seconds")
    parser.add_argument("--limit", type=int, default=10, help="Limit the number of changes fetched at a time")

    args = parser.parse_args()

    # Database connection URI
    DATABASE_URI = f"postgresql+psycopg2://{args.user}:{args.password}@{args.host}:{args.port}/{args.dbname}"

    # Create SQLAlchemy engine
    engine = create_engine(DATABASE_URI)

    # Start polling for changes
    continuously_poll_changes(engine, args.slot_name, interval=args.interval, limit=args.limit)


if __name__ == "__main__":
    main()
