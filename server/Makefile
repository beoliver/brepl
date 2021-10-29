binary=bin/brepl

default: vet test build

clean:
	rm -rf bin
	rm -rf public
	mkdir public

vet:
	go vet ./...

test:
	go test -v ./...

dev: vet
	go run main.go -port 8080 -ws-any-origin -serve-from ./public

build: main.go
	mkdir -p bin
	go build -a -installsuffix cgo -o $(binary) $^
