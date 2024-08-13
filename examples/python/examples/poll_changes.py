import argparse
import time

from sqlalchemy import create_engine, text


# Function to get changes from logical replication slot
def get_logical_slot_changes(engine, slot_name, limit=None):
    query = "SELECT * FROM pg_logical_slot_get_changes(:slot_name, NULL, :limit);"
    with engine.connect() as connection:
        result = connection.execute(text(query), {"slot_name": slot_name, "limit": limit})
        changes = result.fetchall()
    return changes


def main():
    parser = argparse.ArgumentParser(description="Fetch changes from PostgreSQL logical replication slot.")
    parser.add_argument("--host", type=str, default="localhost", help="Database host")
    parser.add_argument("--port", type=int, default=5432, help="Database port")
    parser.add_argument("--dbname", type=str, default="dhis", help="Database name")
    parser.add_argument("--user", type=str, default="dhis", help="Database user")
    parser.add_argument("--password", type=str, default="dhis", help="Database password")
    parser.add_argument("--slot_name", type=str, required=True, help="Logical replication slot name")
    parser.add_argument("--limit", type=int, default=10, help="Limit the number of changes fetched")

    args = parser.parse_args()

    # Database connection URI
    DATABASE_URI = f"postgresql+psycopg2://{args.user}:{args.password}@{args.host}:{args.port}/{args.dbname}"

    # Create SQLAlchemy engine
    engine = create_engine(DATABASE_URI)

    print("Starting to monitor changes...")

    try:
        while True:
            # Fetch changes from the logical slot
            changes = get_logical_slot_changes(engine, args.slot_name, limit=args.limit)

            if changes:
                for change in changes:
                    print(change)
            else:
                time.sleep(1)  # Wait before retrying

    except KeyboardInterrupt:
        print("\nMonitoring stopped by user. Exiting gracefully.")
    finally:
        engine.dispose()  # Ensure the database connection is properly closed


if __name__ == "__main__":
    main()
