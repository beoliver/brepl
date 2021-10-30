binary=bin/brepl

default: vet test build

clean:
	rm -rf bin
	rm -rf public
	mkdir public
	echo "this file exists to avoid the warning - 'cannot embed directory public: contains no embeddable files'" > public/hello.txt

vet:
	go vet ./...

test:
	go test -v ./...

dev: vet
	go run main.go --port 8080 --root ./public --ws-any-origin

build: main.go
	mkdir -p bin
	go build -ldflags "-s -w" -a -installsuffix cgo -o $(binary) $^
