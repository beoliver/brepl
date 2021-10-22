package main

import (
	"bufio"
	"encoding/json"
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

func (s *ReplService) readFromRepl(ws *websocket.Conn, repl ReplConn) {
	scanner := bufio.NewScanner(repl.conn)
	for scanner.Scan() {
		log.Printf("Scanner Scanned")
		bytes := scanner.Bytes()

		if s.ednToJson {
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
		} else {
			err := ws.WriteMessage(websocket.TextMessage, bytes)
			if err != nil {
				return
			}
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

func (s *ReplService) connectToRepl(ws *websocket.Conn, port string) {
	repl, err := openReplConn(port)
	if err != nil {
		return
	}
	defer repl.conn.Close()
	go s.readFromRepl(ws, repl)
	writeToRepl(repl, ws)
}

type ReplService struct {
	upgrader  websocket.Upgrader
	ednToJson bool
}

func initializeUpgrader(anyOrigin bool, ednToJson bool) *ReplService {
	var upgrader = websocket.Upgrader{} // use default options as base
	if anyOrigin {
		upgrader.CheckOrigin = func(r *http.Request) bool { return true }
	}
	return &ReplService{upgrader, ednToJson}
}

func (s *ReplService) HandlePreplWebsocket(w http.ResponseWriter, r *http.Request) {
	vars := mux.Vars(r)
	port := vars["port"]
	ws, err := s.upgrader.Upgrade(w, r, nil)
	if err != nil {
		log.Print("upgrade:", err)
		return
	}
	defer ws.Close()
	s.connectToRepl(ws, port)
}

func main() {
	port := flag.String("port", "8080", "web server port")
	wsAnyOrigin := flag.Bool("ws-any-origin", false, "allow connections from different hosts/ports")
	ednToJson := flag.Bool("edn-to-json", false, "convert repl responses to JSON (top level only)")
	serveFrom := flag.String("serve-from", "public", "directory to serve static files from")

	flag.Parse()
	addr := fmt.Sprintf("localhost:%s", *port)

	replService := initializeUpgrader(*wsAnyOrigin, *ednToJson)

	fmt.Printf("Serving static files from '%s' on '%s'\n", *serveFrom, addr)
	fmt.Printf("Convert EDN to JSON: '%t'\n", *ednToJson)
	fmt.Printf("Allow websocket connections when 'origin' is not '%s': %t\n", addr, *wsAnyOrigin)
	fmt.Printf("%s/prepl/{port} to establish websocket connection to a prepl instance\n", addr)

	myRouter := mux.NewRouter()
	myRouter.HandleFunc("/prepl/{port}", replService.HandlePreplWebsocket)
	myRouter.PathPrefix("/").Handler(http.StripPrefix("/", http.FileServer(http.Dir(*serveFrom))))
	log.Fatal(http.ListenAndServe(addr, myRouter))
}
