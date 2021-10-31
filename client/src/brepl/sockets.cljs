(ns ^:figwheel-hooks brepl.sockets
  (:require [cljs.pprint :refer [cl-format]]))

(defonce ^:private sockets (atom {}))

(defn websocket!
  [{:keys [ws repl] :as _connection-info}]
  (js/WebSocket. (cl-format nil "ws://~a/~a/~a" (:address ws) (:type repl) (:address repl))))

(defn socket-write! [sock-name expr] (.send (get @sockets sock-name) expr))
(defn socket-close! [sock-name] (.close (get @sockets sock-name)))

(defmulti on-socket-open    (fn [ws-name _event] ws-name))
(defmulti on-socket-close   (fn [ws-name _event] ws-name))
(defmulti on-socket-message (fn [ws-name _event] ws-name))
(defmulti on-socket-error   (fn [ws-name _event] ws-name))

(defn new-named-socket! [ws-name connection-info]
  (let [socket (websocket! connection-info)]
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
