import argparse
import json
import time

from pydantic import BaseModel
from sqlalchemy import create_engine, text


class ChangeItem(BaseModel):
    class OldKeys(BaseModel):
        keynames: list[str] = []
        keytypes: list[str] = []
        keyvalues: list[int | str] = []

    kind: str
    table: str
    columnnames: list[str] = None
    columntypes: list[str] = None
    columnvalues: list[int | str] = None
    oldkeys: OldKeys = None


# Function to get changes from logical replication slot
def get_logical_slot_changes(connection, slot_name, limit=None) -> list[ChangeItem]:
    change_items: list[ChangeItem] = []

    query = text("SELECT * FROM pg_logical_slot_get_changes(:slot_name, NULL, :limit);")
    result = connection.execute(query, {"slot_name": slot_name, "limit": limit})

    sql_result = result.fetchall()

    for result in sql_result:
        changes = json.loads(result[2])
        print(changes)

        for change in changes["change"]:
            change_items.append(ChangeItem(**change))

    return change_items


# Function to continuously poll for changes
def continuously_poll_changes(engine, slot_name, interval=5, limit=10):
    print("Starting to poll for changes...")
    while True:
        try:
            with engine.connect() as connection:
                changes = get_logical_slot_changes(connection, slot_name, limit)
                for change in changes:
                    # print(change.model_dump_json(indent=2))
                    print(change.model_dump_json())

                # If there are no changes, sleep for the interval period
                if not changes:
                    time.sleep(interval)
        except Exception as e:
            print(f"An error occurred: {e}")
            time.sleep(interval)  # Sleep before retrying


def main():
    parser = argparse.ArgumentParser(
        description="Continuously poll and process changes from a PostgreSQL logical replication slot."
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
