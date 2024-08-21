import argparse
import time

import psycopg


# Function to get changes from logical replication slot
def get_logical_slot_changes(connection, slot_name, limit=None):
    query = "SELECT * FROM pg_logical_slot_get_changes(%s, NULL, %s);"
    with connection.cursor() as cursor:
        cursor.execute(query, (slot_name, limit))
        changes = cursor.fetchall()

    # Filter out changes with empty change lists
    filtered_changes = [change for change in changes if change[2] != '{"change":[]}']

    return filtered_changes


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
    DATABASE_URI = f"postgresql://{args.user}:{args.password}@{args.host}:{args.port}/{args.dbname}"

    print("Starting to monitor changes...")

    try:
        # Connect to the PostgreSQL database
        with psycopg.connect(DATABASE_URI) as conn:
            while True:
                # Fetch changes from the logical slot
                changes = get_logical_slot_changes(conn, args.slot_name, limit=args.limit)

                if changes:
                    for change in changes:
                        print(change)
                else:
                    time.sleep(1)  # Wait before retrying

    except KeyboardInterrupt:
        print("\nMonitoring stopped by user. Exiting gracefully.")


if __name__ == "__main__":
    main()
