binary=bin/brepl-server

default: vet test build

clean:
	rm -rf bin

vet:
	go vet ./...

test:
	go test -v ./...

dev: vet
	go run main.go -port 8080 -serve-from ../public

build: main.go
	mkdir -p bin
	go build -a -installsuffix cgo -o $(binary) $^
