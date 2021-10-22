
# Develop

Stick html/css/js in the `public` dir in the parent directory.

If you just need the websocket, but have your own local server - for example 
when developing a frontend with hot reloading, then you need to disable strict 
origin checks using the flag `-ws-any-origin`. This is enabled by default if you 
use `make dev`. 

```shell
go run main.go -port 8080 -serve-from $PWD/../public
```
This _should_ be the same as running

```shell
make dev
```

# Build

```shell
make build
```

```shell
./bin/brepl-server -port 8080 -serve-from $PWD/../public
```
