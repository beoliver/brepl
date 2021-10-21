package main

import (
	"bufio"
	"encoding/json"
	"fmt"
	"flag"
	"github.com/gorilla/mux"
	"github.com/gorilla/websocket"
	"log"
	"net"
	"net/http"
	"olympos.io/encoding/edn"
)



var upgrader = websocket.Upgrader{} // use default options

type PreplResult struct {
	Tag       edn.Keyword `edn:"tag"       json:"tag"`
	Val       string      `edn:"val"       json:"val"`
	Ns        string      `edn:"ns"        json:"ns"`
	Ms        int64       `edn:"ms"        json:"ms"`
	Form      string      `edn:"form"      json:"form"`
	Exception bool        `edn:"exception" json:"exception"`
}

func replResponses(prepl Prepl, ws *websocket.Conn) {
	scanner := bufio.NewScanner(prepl.conn)
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
		ws.WriteMessage(websocket.TextMessage, b)
	}
}

func prepl(w http.ResponseWriter, r *http.Request) {
	vars := mux.Vars(r)
	port := vars["port"]
	prepl, err := ConnectToRepl(port)
	if err != nil {
		log.Printf("error connecting to port: %s", port)
		return
	}
	defer prepl.conn.Close()

	c, err := upgrader.Upgrade(w, r, nil)
	if err != nil {
		log.Print("upgrade:", err)
		return
	}
	defer c.Close()

	// send responses async
	go replResponses(prepl, c)

	for {
		mt, message, err := c.ReadMessage()
		if err != nil {
			log.Println("read:", err)
			break
		}

		log.Printf("recv: %s with message type: %d", message, mt)
		message = append(message, '\n')

		sentBytes, err := fmt.Fprintf(prepl.conn, string(message))

		log.Printf("wrote %d bytes to prepl", sentBytes)

		if err != nil {
			log.Println("prepl write:", err)
			break
		}
	}
}

type Service struct {
}

type Prepl struct {
	conn net.Conn
	addr string
}

func ConnectToRepl(port string) (Prepl, error) {
	address := fmt.Sprintf("localhost:%s", port)
	conn, err := net.Dial("tcp", address)
	if err != nil {
		return Prepl{}, err
	}
	return Prepl{conn: conn, addr: address}, nil
}

func main() {
	port := flag.String("port", "8080", "web server port")
	flag.Parse()
	// handle func to open a repl websocket
	// ws://localhost:7777/prepl?port=8888
	// I want it to be ws://localhost:7777/prepl/{PORT}
	// as the prepl port is required - but does NOT need to be known by the server
	addr := fmt.Sprintf(":%s", *port)
	myRouter := mux.NewRouter()
	myRouter.HandleFunc("/prepl/{port}", prepl)
	myRouter.PathPrefix("/").Handler(http.StripPrefix("/", http.FileServer(http.Dir("public"))))
	log.Fatal(http.ListenAndServe(addr, myRouter))
}
