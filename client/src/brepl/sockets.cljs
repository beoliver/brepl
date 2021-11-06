(ns brepl.sockets
  (:require [cljs.pprint :refer [cl-format]]))

(defn format-websocket-url [proxy repl-server]
  (cl-format nil "ws://~a:~a/~a/~a:~a"
             (or (:hostname proxy) "localhost")
             (:port proxy)
             (name (:type repl-server))
             (or (:hostname repl-server) "localhost")
             (:port repl-server)))

(defn socket [proxy repl-server {:keys [open error message close] :as _callbacks}]
  (let [url (format-websocket-url proxy repl-server)
        _ (js/console.log url)
        s (js/WebSocket. url)]
    (.addEventListener s "open" open)
    (.addEventListener s "error" error)
    (.addEventListener s "message" message)
    (.addEventListener s "close" close)
    s))

(defn write! [socket data] (.send socket data))

(defn close! [socket] (.close socket))
