
# Develop

Stick html/css/js in the `public` dir in the parent directory

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
.bin/brepl-server -port 8080 -serve-from $PWD/../public
```
