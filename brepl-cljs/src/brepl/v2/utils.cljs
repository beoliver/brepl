(ns brepl.v2.utils
  (:require [cljs.tools.reader.edn :as edn]
            [clojure.string :as str]))

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


(defn namespace-as-segments [s] (str/split s #"\."))

(defn- longest-common-prefix [xs]
  (loop [prefix []
         xs xs]
    (let [p-set (set (map first xs))]
      (cond
        (not= (count p-set) 1) prefix
        (= p-set #{nil})       prefix
        :else                  (recur (into prefix p-set) (map rest xs))))))

(defn- strip-common-prefix [segment-vectors]
  (if (= 1 (count segment-vectors))
    {:prefix (first segment-vectors) :remaining []}
    (let [common-prefix (longest-common-prefix segment-vectors)
          n             (count common-prefix)]
      {:prefix    common-prefix
       :remaining (map #(subvec % n) segment-vectors)})))

(defn- common-prefix-tree [leaf-key segment-vectors]
  (let [seq-of-segment-vectors (vals (group-by first segment-vectors))]
    (reduce (fn [acc segment-vectors]
              (let [{:keys [prefix remaining]} (strip-common-prefix segment-vectors)]
                (cond
                  (= prefix [])    (assoc acc leaf-key true)
                  (= remaining []) (assoc acc (str/join "." prefix) {leaf-key true})
                  :else (assoc acc (str/join "." prefix) (common-prefix-tree leaf-key remaining)))))
            {} seq-of-segment-vectors)))

(defn- add-paths-to-prefix-tree [leaf-key separator path tree]
  (reduce-kv (fn [acc prefix subtree]
               (if (= prefix leaf-key)
                 (assoc acc prefix (str/join separator path))
                 (assoc acc prefix (add-paths-to-prefix-tree leaf-key separator (conj path prefix) subtree))))
             {} tree))

(defn prefix-tree [leaf-key separator segments]
  (->> segments
       (common-prefix-tree leaf-key)
       (add-paths-to-prefix-tree leaf-key separator [])))
