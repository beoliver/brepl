(ns brepl.utils
  (:require [cljs.tools.reader.edn :as edn]))

(defn read-clojure [path callback-fn]
  ;; (read-clojure (fn [data] (println data)))
  (let [client (new js/XMLHttpRequest)]
    (.open client "GET" path)
    (.addEventListener client "load"
                       (fn [_event]
                         (println "event is happening")
                         (callback-fn (.-responseText client))))
    (.send client)))

(defn- try-to-cast [x]
  (cond (re-find #"^[0-9]*(\.[0-9]+)*$" x) (edn/read-string x)
        (re-find #"^true|false$" x) (edn/read-string x)
        (re-find #"^:" x) (edn/read-string x)
        :else x))

(defn query-param-map [search]
  (let [url-params (new js/URLSearchParams search)]
    (reduce (fn [acc entry]
              (let [[k v] [(aget entry 0) (aget entry 1)]]
                (assoc acc (keyword k) (try-to-cast v)))) {} (.entries url-params))))
