package main

import (
	"bufio"
	"flag"
	"fmt"
	"io"
	"log"
	"net"
	"net/http"

	"github.com/gorilla/mux"
	"github.com/gorilla/websocket"
)


func readFromSock(ws *websocket.Conn, sock net.Conn) {
	wa := ws.RemoteAddr().String()
	ra := sock.RemoteAddr().String()

	// log.Println("Starting READ from server...")
	// TODO: needed to set a larger buffer size so that
	// I could read the metadata from clojure.core :|
	// This is a hack as anything hardcoded (size) will fail
	// then the input is larger...
	scanner := bufio.NewScanner(sock)
	buf := make([]byte, 0, 64*1024)
	scanner.Buffer(buf, 1024*1024)
	for scanner.Scan() {
		log.Printf("Response from server...\n")
		bytes := scanner.Bytes()
		// log.Printf("Scanner read as string: %s\n", string(bytes))
		err := ws.WriteMessage(websocket.TextMessage, bytes)
		if err != nil {
			return
		}
		log.Printf("%s <--[%dB]-- %s\n", wa, len(scanner.Bytes()), ra)
	}
}

func writeToSock(sock net.Conn, ws *websocket.Conn) {
	wa := ws.RemoteAddr().String()
	ra := sock.RemoteAddr().String()
	for {
		_, r, err := ws.NextReader()
		if err != nil {
			log.Printf("error %+v\n", err)
			break
		}
		bytesWritten, err := io.Copy(sock, r)
		if err != nil {
			log.Printf("error %+v\n", err)
			break
		}
		log.Printf("%s --[%dB]--> %s\n", wa, bytesWritten, ra)
	}
}

func connectToTcpSocket(ws *websocket.Conn, address string) {
	sock, err := net.Dial("tcp", address)
	if err != nil {
		log.Printf("Error %+v\n", err)
		return
	}
	defer sock.Close()
	go readFromSock(ws, sock)
	writeToSock(sock, ws)
}

type TcpProxyService struct {
	upgrader  websocket.Upgrader
}

func tcpProxyService(anyOrigin bool) *TcpProxyService {
	var upgrader = websocket.Upgrader{} // use default options as base
	if anyOrigin {
		upgrader.CheckOrigin = func(r *http.Request) bool { return true }
	}
	return &TcpProxyService{upgrader}
}

func (s *TcpProxyService) HandleWebsocket(w http.ResponseWriter, r *http.Request) {
	vars := mux.Vars(r)
	address := vars["address"]
	ws, err := s.upgrader.Upgrade(w, r, nil)
	if err != nil {
		log.Print("upgrade:", err)
		return
	}
	defer func(ws *websocket.Conn) {
		log.Println("defer close in HandleWebsocket")
		err := ws.Close()
		if err != nil {
			log.Printf("Error '%+v' when closing websocket\n", err)
		}
	}(ws)
	connectToTcpSocket(ws, address)
}

func main() {
	port := flag.String("port", "8080", "web server port")
	wsAnyOrigin := flag.Bool("ws-any-origin", false, "allow connections from different hosts/ports")
	serveFrom := flag.String("serve-from", "public", "directory to serve static files from")

	flag.Parse()
	addr := fmt.Sprintf("localhost:%s", *port)

	serivce := tcpProxyService(*wsAnyOrigin)

	fmt.Printf("Serving static files from '%s' on '%s'\n", *serveFrom, addr)
	fmt.Printf("Allow websocket connections when 'origin' is not '%s': %t\n", addr, *wsAnyOrigin)
	fmt.Printf("%s/tcp/{address} to establish websocket connection to a remote repl socket\n", addr)

	myRouter := mux.NewRouter()
	myRouter.HandleFunc("/tcp/{address}", serivce.HandleWebsocket)
	myRouter.PathPrefix("/").Handler(http.StripPrefix("/", http.FileServer(http.Dir(*serveFrom))))
	log.Fatal(http.ListenAndServe(addr, myRouter))
}
