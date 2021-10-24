(ns brepl.ws
  (:require
   [cljs.pprint :refer [cl-format]]))

(defn create!
  [hostname ws-port repl-port]
  (js/WebSocket. (cl-format nil "ws://~a:~a/prepl/~a" hostname ws-port repl-port)))
