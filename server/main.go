package main

import (
	"bufio"
	"flag"
	"fmt"
	"log"
	"net"
	"net/http"

	"github.com/gorilla/mux"
	"github.com/gorilla/websocket"
	"olympos.io/encoding/edn"
)

type PreplResult struct {
	Tag       edn.Keyword `edn:"tag"       json:"tag"`
	Val       string      `edn:"val"       json:"val"`
	Ns        string      `edn:"ns"        json:"ns"`
	Ms        int64       `edn:"ms"        json:"ms"`
	Form      string      `edn:"form"      json:"form"`
	Exception bool        `edn:"exception" json:"exception"`
}

type ReplConn struct {
	conn net.Conn
	addr string
}

// openReplConn allows us to communicate with the clojure socket server

func openReplConn(port string) (ReplConn, error) {
	address := fmt.Sprintf("localhost:%s", port)
	conn, err := net.Dial("tcp", address)
	if err != nil {
		return ReplConn{}, err
	}
	return ReplConn{conn: conn, addr: address}, nil
}

func readFromRepl(ws *websocket.Conn, repl ReplConn) {
	log.Println("Starting READ from server...")
	// TODO: needed to set a larger buffer size so that
	// I could read the metadata from clojure.core :|
	// This is a hack as anything hardcoded (size) will fail
	// then the input is larger...
	scanner := bufio.NewScanner(repl.conn)
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
	}
}

func writeToRepl(repl ReplConn, ws *websocket.Conn) {
	log.Println("Starting WRITE to server...")
	for {
		log.Println("about to read message from ws")
		_, message, err := ws.ReadMessage()
		if err != nil {
			log.Println("read:", err)
			break
		}
		log.Printf("recv: '%s' with bytes %+v\n", string(message), message)
		sentBytes, err := fmt.Fprintf(repl.conn, "%s\n",string(message))
		if err != nil {
			log.Println("prepl write:", err)
			break
		}
		log.Printf("wrote %d bytes to prepl", sentBytes)
	}
}

func connectToRepl(ws *websocket.Conn, port string) {
	log.Printf("connectToRepl...")
	repl, err := openReplConn(port)
	if err != nil {
		log.Printf("Error %+v\n", err)
		return
	}
	defer func(conn net.Conn) {
		log.Println("defer close in connectToRepl")
		err := conn.Close()
		if err != nil {
			log.Printf("Error '%+v' when closing repl connection\n", err)
		}
	}(repl.conn)
	go readFromRepl(ws, repl)
	writeToRepl(repl, ws)
}

type ReplService struct {
	upgrader  websocket.Upgrader
}

func initializeUpgrader(anyOrigin bool) *ReplService {
	var upgrader = websocket.Upgrader{} // use default options as base
	if anyOrigin {
		upgrader.CheckOrigin = func(r *http.Request) bool { return true }
	}
	return &ReplService{upgrader}
}

func (s *ReplService) HandlePreplWebsocket(w http.ResponseWriter, r *http.Request) {
	vars := mux.Vars(r)
	port := vars["port"]
	ws, err := s.upgrader.Upgrade(w, r, nil)
	if err != nil {
		log.Print("upgrade:", err)
		return
	}
	defer func(ws *websocket.Conn) {
		log.Println("defer close in HandlePreplWebsocket")
		err := ws.Close()
		if err != nil {
			log.Printf("Error '%+v' when closing websocket\n", err)
		}
	}(ws)
	connectToRepl(ws, port)
}

func main() {
	port := flag.String("port", "8080", "web server port")
	wsAnyOrigin := flag.Bool("ws-any-origin", false, "allow connections from different hosts/ports")
	serveFrom := flag.String("serve-from", "public", "directory to serve static files from")

	flag.Parse()
	addr := fmt.Sprintf("localhost:%s", *port)

	replService := initializeUpgrader(*wsAnyOrigin)

	fmt.Printf("Serving static files from '%s' on '%s'\n", *serveFrom, addr)
	fmt.Printf("Allow websocket connections when 'origin' is not '%s': %t\n", addr, *wsAnyOrigin)
	fmt.Printf("%s/prepl/{port} to establish websocket connection to a prepl instance\n", addr)

	myRouter := mux.NewRouter()
	myRouter.HandleFunc("/prepl/{port}", replService.HandlePreplWebsocket)
	myRouter.PathPrefix("/").Handler(http.StripPrefix("/", http.FileServer(http.Dir(*serveFrom))))
	log.Fatal(http.ListenAndServe(addr, myRouter))
}
