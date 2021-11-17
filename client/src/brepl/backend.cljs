(ns brepl.backend)

;;; --------------------------------------------------------------------------------

(defprotocol Backend
  (connect [backend])
  (connection-status [backend] "should return a r/atom that components can listen to")
  (send [backend data callback])
  (close [backend]))

;;; --------------------------------------------------------------------------------

(defprotocol WithHistory
  (history [this] "should return a r/atom that components can listen to"))

(defprotocol Completions
  (complete [this prefix])
  (active-completions [this]))
