package main

import (
	"bufio"
	"embed"
	"flag"
	"fmt"
	"io"
	"io/fs"
	"log"
	"net"
	"net/http"

	"github.com/gorilla/mux"
	"github.com/gorilla/websocket"
)


func readFromSock(ws *websocket.Conn, sock net.Conn) {
	wa := ws.RemoteAddr().String()
	ra := sock.RemoteAddr().String()

	scanner := bufio.NewScanner(sock)

	for scanner.Scan() {
		w, err := ws.NextWriter(websocket.TextMessage)
		if err != nil {
			log.Printf("error %+v\n", err)
			break
		}
		bytesWritten, err := w.Write(scanner.Bytes())
		if err != nil {
			log.Printf("error %+v\n", err)
			break
		}
		err = w.Close()
		if err != nil {
			log.Printf("error %+v\n", err)
			break
		}
		log.Printf("[prepl:%s]--[%dB]-->[ws:%s]\n", ra, bytesWritten, wa)
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
		log.Printf("[ws:%s]--[%dB]-->[prepl:%s]\n", wa, bytesWritten, ra)
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

// content holds our static web server content.

//go:embed public
var public embed.FS

func main() {

	var port string
	flag.StringVar(&port, "p", "8080", "web server port")
	flag.StringVar(&port, "port", "8080", "web server port")

	var root string
	flag.StringVar(&root, "r", "", "root directory to serve static files from")
	flag.StringVar(&root, "root", "", "root directory to serve static files from")

	var wsAnyOrigin bool
	flag.BoolVar(&wsAnyOrigin, "ws-any-origin", false, "allow connections from different hosts/ports")

	flag.Parse()

	addr := fmt.Sprintf("localhost:%s", port)
	service := tcpProxyService(wsAnyOrigin)

	fmt.Printf("Allow websocket connections when 'origin' is not '%s': %t\n", addr, wsAnyOrigin)
	fmt.Printf("%s/prepl/{address} to establish websocket connection to a remote (prepl) socket\n", addr)

	muxRouter := mux.NewRouter()

	muxRouter.HandleFunc("/prepl/{address}", service.HandleWebsocket)

	// the content that we serve is embedded in the binary.
	// this means that no matter where the binary is located, all static dependencies are present

	if root != "" {
		fmt.Printf("Serving static files from '%s'\n", root)
		muxRouter.PathPrefix("/").Handler(http.StripPrefix("/", http.FileServer(http.Dir(root))))
	} else {
		// needed to use fs.Sub...
		// found this out via https://blog.carlmjohnson.net/post/2021/how-to-use-go-embed/
		fsys, err := fs.Sub(public, "public")
		if err != nil {
			log.Fatal(err)
		}
		muxRouter.PathPrefix("/").Handler(http.StripPrefix("/", http.FileServer(http.FS(fsys))))
	}

	log.Fatal(http.ListenAndServe(addr, muxRouter))
}
