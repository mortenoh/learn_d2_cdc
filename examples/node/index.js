const { Client } = require("pg");
const yargs = require("yargs");
const sleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms));

// Function to get changes from logical replication slot
async function getLogicalSlotChanges(client, slotName, limit) {
  const query = `SELECT * FROM pg_logical_slot_get_changes($1, NULL, $2);`;
  const res = await client.query(query, [slotName, limit]);
  return res.rows;
}

// Function to check if a change is empty (i.e., has an empty "change" array in "data")
function isEmptyChange(change) {
  try {
    const data = JSON.parse(change.data);
    return Array.isArray(data.change) && data.change.length === 0;
  } catch (e) {
    console.error("Failed to parse change data:", e);
    return false;
  }
}

async function main() {
  const argv = yargs
    .option("host", {
      describe: "Database host",
      type: "string",
      default: "localhost",
    })
    .option("port", {
      describe: "Database port",
      type: "number",
      default: 5432,
    })
    .option("dbname", {
      describe: "Database name",
      type: "string",
      default: "dhis",
      demandOption: true,
    })
    .option("user", {
      describe: "Database user",
      type: "string",
      default: "dhis",
      demandOption: true,
    })
    .option("password", {
      describe: "Database password",
      type: "string",
      default: "dhis",
      demandOption: true,
    })
    .option("slot_name", {
      describe: "Logical replication slot name",
      type: "string",
      demandOption: true,
    })
    .option("limit", {
      describe: "Limit the number of changes fetched",
      type: "number",
      default: 10,
    })
    .option("interval", {
      describe: "Polling interval in milliseconds",
      type: "number",
      default: 1000,
    })
    .help().argv;

  const client = new Client({
    host: argv.host,
    port: argv.port,
    database: argv.dbname,
    user: argv.user,
    password: argv.password,
  });

  try {
    await client.connect();

    console.log("Starting to monitor changes...");
    while (true) {
      const changes = await getLogicalSlotChanges(
        client,
        argv.slot_name,
        argv.limit
      );

      changes.forEach((change) => {
        if (!isEmptyChange(change)) {
          console.log(change);
        }
      });

      if (changes.length === 0 || changes.every(isEmptyChange)) {
        // console.log("No relevant changes detected. Waiting...");
      }

      await sleep(argv.interval);
    }
  } catch (err) {
    console.error("An error occurred:", err);
  } finally {
    await client.end();
  }
}

if (require.main === module) {
  main();
}
