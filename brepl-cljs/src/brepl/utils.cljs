(ns brepl.utils)

(defn read-clojure [path callback-fn]
  ;; (read-clojure (fn [data] (println data)))
  (let [client (new js/XMLHttpRequest)]
    (.open client "GET" path)
    (.addEventListener client "load"
                       (fn [_event]
                         (println "event is happening")
                         (callback-fn (.-responseText client))))
    (.send client)))
