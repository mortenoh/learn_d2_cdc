import argparse
import json
import time
from contextlib import asynccontextmanager

import psycopg
from fastapi import FastAPI
from fastapi.responses import StreamingResponse

app = FastAPI()


# Function to get changes from logical replication slot
def get_logical_slot_changes(connection, slot_name, limit=None):
    query = "SELECT * FROM pg_logical_slot_get_changes(%s, NULL, %s);"
    with connection.cursor() as cursor:
        cursor.execute(query, (slot_name, limit))
        changes = cursor.fetchall()

    # Filter out changes with empty change lists
    filtered_changes = [change for change in changes if change[2] != '{"change":[]}']

    return filtered_changes


# Generator function to stream changes
def stream_changes(connection, slot_name, limit):
    try:
        while True:
            changes = get_logical_slot_changes(connection, slot_name, limit)

            if changes:
                for change in changes:
                    yield f"data: {json.dumps(change)}\n\n"
            else:
                time.sleep(1)  # Wait before retrying

    except psycopg.Error as e:
        yield f"data: Database error: {str(e)}\n\n"
    except KeyboardInterrupt:
        yield "data: Stream stopped by user.\n\n"


# Context manager to handle startup and shutdown of the database connection
@asynccontextmanager
async def lifespan(app: FastAPI):
    args = app.state.args

    # Database connection URI
    DATABASE_URI = f"postgresql://{args.user}:{args.password}@{args.host}:{args.port}/{args.dbname}"

    # Establish the database connection and store it in the app state
    conn = psycopg.connect(DATABASE_URI)
    app.state.conn = conn
    print("Database connection established.")

    # Yield control back to the application
    yield

    # Close the database connection when the application shuts down
    conn.close()
    print("Database connection closed.")


# FastAPI configuration to use the lifespan context manager
app = FastAPI(lifespan=lifespan)


# FastAPI endpoint to start streaming
@app.get("/stream")
async def stream(slot_name: str, limit: int = 10):
    conn = app.state.conn  # Reuse the connection stored in the app state
    return StreamingResponse(stream_changes(conn, slot_name, limit), media_type="text/event-stream")


def main():
    parser = argparse.ArgumentParser(
        description="Start SSE stream for changes from PostgreSQL logical replication slot."
    )
    parser.add_argument("--host", type=str, default="localhost", help="Database host")
    parser.add_argument("--port", type=int, default=5432, help="Database port")
    parser.add_argument("--dbname", type=str, default="dhis", help="Database name")
    parser.add_argument("--user", type=str, default="dhis", help="Database user")
    parser.add_argument("--password", type=str, default="dhis", help="Database password")

    args = parser.parse_args()

    # Store parsed arguments in FastAPI app state
    app.state.args = args

    import uvicorn

    uvicorn.run(app, host="0.0.0.0", port=8000)


if __name__ == "__main__":
    main()
