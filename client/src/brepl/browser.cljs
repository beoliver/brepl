(ns brepl.browser
  (:require [brepl.sockets :as sockets]))

(def sock-name (atom nil))

(defn connect! [info]
  (->> (sockets/create! :browser info)
       (reset! sock-name)))
