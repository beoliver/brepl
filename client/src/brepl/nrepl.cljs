(ns brepl.nrepl
  (:require [brepl.bencode :refer [encode decode]]
            [brepl.sockets :as sockets]
            [reagent.core :as r]))

(def sock-name :nrepl/background)

(defn connect! [ws-addr repl-addr]
  (sockets/new-named-socket! sock-name {:ws {:address ws-addr}
                                        :repl {:address repl-addr :type "nrepl"}}))

(defn close! [] (sockets/socket-close! sock-name))

;;;

(defn handle-nrepl-message
  [bencode]
  (js/console.log "got bencode" bencode)
  (let [data (decode bencode)]
    (js/console.log data)))

;;; nrepl specific things
(defonce session (r/atom nil))     ;; 24e79a60-b2d0-49c8-8e5f-4db69580d698
(defonce new-session (r/atom nil)) ;; 00482346-066e-4b47-8535-3ac5243c3c2d

(defonce connected? (r/atom false))
(defonce error? (r/atom false))
(defonce current-namespace (r/atom nil))


(defmethod sockets/on-socket-open sock-name [_ _]
  (let [msg (encode {:op "clone"})]
    (js/console.log msg)
    (sockets/socket-write! sock-name msg)))

(defmethod sockets/on-socket-close sock-name
  [_ _] (reset! connected? false))
(defmethod sockets/on-socket-error sock-name [_ _]
  (reset! connected? false)
  (reset! error? true))

(defmethod sockets/on-socket-message sock-name [_ event]
  (js/console.log event)
  (js/console.log (.-data event))
  (handle-nrepl-message (.-data event)))
