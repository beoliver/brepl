package main

import (
	"bufio"
	"encoding/json"
	"flag"
	"fmt"
	"github.com/gorilla/mux"
	"github.com/gorilla/websocket"
	"log"
	"net"
	"net/http"
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

func openReplConn (port string) (ReplConn, error) {
	address := fmt.Sprintf("localhost:%s", port)
	conn, err := net.Dial("tcp", address)
	if err != nil {
		return ReplConn{}, err
	}
	return ReplConn{conn: conn, addr: address}, nil
}

func readFromRepl(ws *websocket.Conn, repl ReplConn) {
	scanner := bufio.NewScanner(repl.conn)
	for scanner.Scan() {
		log.Printf("Scanner Scanned")
		bytes := scanner.Bytes()
		var result PreplResult
		err := edn.Unmarshal(bytes, &result)
		if err != nil {
			log.Printf("Error unmarshalling data: %+v", bytes)
			return
		}
		b, err := json.Marshal(result)
		if err != nil {
			log.Printf("Error marshalling data: %+v", err)
			return
		}
		err = ws.WriteMessage(websocket.TextMessage, b)
		if err != nil {
			return
		}
	}
}

func writeToRepl(repl ReplConn, ws *websocket.Conn) {
	for {
		mt, message, err := ws.ReadMessage()
		if err != nil {
			log.Println("read:", err)
			break
		}
		log.Printf("recv: %s with message type: %d", message, mt)
		message = append(message, '\n')
		sentBytes, err := fmt.Fprintf(repl.conn, string(message))
		log.Printf("wrote %d bytes to prepl", sentBytes)
		if err != nil {
			log.Println("prepl write:", err)
			break
		}
	}
}

func connectToRepl (ws *websocket.Conn, port string) {
	repl, err := openReplConn(port)
	if err != nil {
		return
	}
	defer repl.conn.Close()
	go readFromRepl(ws, repl)
	writeToRepl(repl, ws)
}


var upgrader = websocket.Upgrader{} // use default options


func HandlePreplWebsocket(w http.ResponseWriter, r *http.Request) {
	vars := mux.Vars(r)
	port := vars["port"]
	ws, err := upgrader.Upgrade(w, r, nil)
	if err != nil {
		log.Print("upgrade:", err)
		return
	}
	defer ws.Close()
	connectToRepl(ws, port)
}


func main() {
	port := flag.String("port", "8080", "web server port")
	serveFrom := flag.String("serve-from", "public", "directory to serve static files from")
	flag.Parse()
	addr := fmt.Sprintf("localhost:%s", *port)

	fmt.Printf("Serving static files from '%s' on '%s'\n", *serveFrom, addr)
	fmt.Printf("%s/prepl/{port} to establish websocket connection to a prepl instance\n", addr)

	myRouter := mux.NewRouter()
	myRouter.HandleFunc("/prepl/{port}", HandlePreplWebsocket)
	myRouter.PathPrefix("/").Handler(http.StripPrefix("/", http.FileServer(http.Dir(*serveFrom))))
	log.Fatal(http.ListenAndServe(addr, myRouter))
}
