# Local-File-Batch-Source with HTTP Connector

## Introduction<br>
- A batch source plugin that reads files from local system.<br>
- If file is csv it can also generate schema.<br>
- If file is not csv it'll use DEFAULT schema.<br>
- Includes connector which runs on HTTP, run the http server to mount file system which can be used by HTTP connector,<br>
that http connector fetches file paths and passes it to plugin.<br>
- Connector can browse and sample upto 1000 records.<br>

## Build<br>
- `mvn clean pacakge -DskipTests`<br>

## Starting the node server<br>
- Move to node-http-server directory<br>
- `npm install`<br>
- `npm start`<br>
- Now you should be able to use connection through Wrangler connection management.
