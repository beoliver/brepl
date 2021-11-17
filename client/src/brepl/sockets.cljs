(ns brepl.sockets
  (:require [cljs.pprint :refer [cl-format]]))

(defn- format-websocket-url [proxy repl]
  (cl-format nil "ws://~a:~a/~a/~a:~a"
             (or (:hostname proxy) "localhost")
             (:port proxy)
             (name (:type repl))
             (or (:hostname repl) "localhost")
             (:port repl)))

(defn socket
  [proxy-addr repl-addr {:keys [onopen onerror onclose onmessage]}]
  (let [sock (js/WebSocket. (format-websocket-url proxy-addr repl-addr))]
    (js/console.log (.-url sock))
    (letfn [(on-open [ev] (when onopen (onopen sock ev)))
            (on-error [ev] (when onerror (onerror sock ev)))
            (on-close [ev] (when onclose (onclose sock ev)))
            (on-message [ev]
              (js/console.log (.-data ev))
              (when onmessage (onmessage sock ev)))]
      (.addEventListener sock "open"    on-open)
      (.addEventListener sock "error"   on-error)
      (.addEventListener sock "close"   on-close)
      (.addEventListener sock "message" on-message)
      sock)))

(defn status [socket]
  (case (.-readyState socket)
    0 :connecting
    1 :open
    2 :closing
    3 :closed))
