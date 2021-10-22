
# brepl-server

A small GO server that accepts websocket requests and then "proxies" them to a running
instance of a clojure prepl socket server.

Reading/writing to/from the socket server is async.

The response maps (EDN) from the socket server are converted to JSON before being
send back over the websocket.

Optionally allows for files to be hosted. This means that a single server can both host
arbitrary front-ends and support websocket repl connections.


## Websocket PREPL Connections

`ws://localhost:$PORT/prepl/$PREPL_PORT`

## Develop

If you just need the websocket, but have your own local server - for example
when developing a frontend with hot reloading, then you need to disable strict
origin checks using the flag `-ws-any-origin`. This is enabled by default if you
use `make dev`.

You can also stick html/css/js in a configurable directory (default is the `public` dir in the parent directory).

```bash
go run main.go -port 8080 -ws-any-origin -serve-from $PWD/../public
```
This _should_ be the same as running

```bash
make dev
```

## Build

```bash
make build
```

```bash
./bin/brepl-server -port 8080 -serve-from $PWD/../public
```
