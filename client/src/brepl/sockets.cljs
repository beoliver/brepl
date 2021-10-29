(ns ^:figwheel-hooks brepl.sockets
  (:require [cljs.pprint :refer [cl-format]]))

(defonce ^:private sockets (atom {}))

(defn websocket!
  [hostname ws-port repl-port]
  (js/WebSocket. (cl-format nil "ws://~a:~a/tcp/:~a" hostname ws-port repl-port)))

(defn socket-write! [sock-name expr] (.send (get @sockets sock-name) expr))
(defn socket-close! [sock-name] (.close (get @sockets sock-name)))

(defmulti on-socket-open    (fn [ws-name _event] ws-name))
(defmulti on-socket-close   (fn [ws-name _event] ws-name))
(defmulti on-socket-message (fn [ws-name _event] ws-name))
(defmulti on-socket-error   (fn [ws-name _event] ws-name))

(defn new-named-socket! [ws-name connection-info]
  (let [socket (websocket! (get-in connection-info [:ws :hostname])
                           (get-in connection-info [:ws :port])
                           (get-in connection-info [:prepl :port]))]

    (.addEventListener socket "open"
                       (fn [event]
                         (swap! sockets assoc ws-name socket)
                         (on-socket-open ws-name event)))
    (.addEventListener socket "close"
                       (fn [event]
                         (swap! sockets dissoc ws-name)
                         (on-socket-close ws-name event)))
    (.addEventListener socket "message"
                       (fn [event]
                         (on-socket-message ws-name event)))
    (.addEventListener socket "error"
                       (fn [event]
                         (swap! sockets dissoc ws-name)
                         (on-socket-error ws-name event)))
    ws-name))
