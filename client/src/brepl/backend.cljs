(ns brepl.backend
  (:require [brepl.sockets :as sockets]))

;;; --------------------------------------------------------------------------------

(defprotocol Connection
  (connect [server callbacks])
  (connection-status [this] "should return a r/atom that components can listen to")
  (send [server data])
  (close [server]))

;;; --------------------------------------------------------------------------------

(defrecord SocketConnection [config socket-atom connection-state-atom message-encode-fn message-decode-fn]
  Connection
  (connect [this {:keys [open close error message] :as _event-callbacks}]
    (let [decode (or message-decode-fn identity)]
      (->>
       {:open    (fn [e]
                   (swap! connection-state-atom assoc :open? true)
                   (when open (-> e .-data open)))
        :close   (fn [e]
                   (swap! connection-state-atom dissoc :open?)
                   (when close (-> e .-data close)))
        :error   (fn [e]
                   (swap! connection-state-atom assoc :error? true)
                   (when error (-> e .-data error)))
        :message (fn [e] (when message
                           (try (-> e .-data decode message)
                                (catch js/Error err (js/console.log err)))))}
       (sockets/socket (:proxy config) (:repl config))
       (reset! socket-atom)))
    this)
  (connection-status [_] connection-state-atom)
  (send [_ data] (sockets/write! @socket-atom (if message-encode-fn
                                                (message-encode-fn data)
                                                data)))
  (close [_] (sockets/close! @socket-atom)))

;;; --------------------------------------------------------------------------------

(defprotocol WithHistory
  (history [this] "should return a r/atom that components can listen to"))

(defprotocol Completions
  (complete [this prefix])
  (active-completions [this]))
