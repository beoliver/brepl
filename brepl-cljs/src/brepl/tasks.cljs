(ns brepl.tasks
  (:require
   [brepl.ws :as websockets]
   [brepl.utils :as utils]
   [reagent.core :as r]
   [cljs.tools.reader.edn :as edn]
   [clojure.string :as str]))

;;; TODO once the structure is a bit clearer this could/should
;;; be pulled into separate namespaces

(defn- as-segments [s]
  (str/split s #"\."))

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


;;; We create our own websocket connection

(defonce ws (atom nil))

;;;

(def ^:private empty-tasks-state {:ns-regex {:remove nil :filter nil} ;; match everything
                                  :brepl-ns nil
                                  :ns {:name nil :publics nil}
                                  :all-ns-names nil
                                  :apropos {:term nil :results nil}})

;;;

(def state (r/atom empty-tasks-state))

;;;

;;; A task is the following
;;; {:task <keyword> :result <any> :input <any>}


(defmulti handle-task-result (fn [task-result _sock] (:task task-result)))

(defmethod handle-task-result :default [_ _] nil)

(declare metadata-for-ns-publics!
         ns-name!
         list-all-ns-names!)

;;; INJECT CLOJURE CODE :P
(defn inject-clojure! [path sock]
  (utils/read-clojure
   path
   (fn [data]
     (println sock)
     (println (type sock))
     (.send sock data))))

(defmethod handle-task-result :inject-clojure
  [{:keys [result]} sock]
  (swap! state assoc :brepl-ns result)
  (ns-name! sock)
  (list-all-ns-names! sock))

;;; get current ns
;;; it SHOULD be user - but we should check

(defn ns-name! [sock]
  (->> {:task :ns-name :result '(ns-name *ns*)}
       str
       (.send sock)))

(defmethod handle-task-result :ns-name
  [{:keys [result]} sock]
  (let [ns-name-str (str result)]
    (metadata-for-ns-publics! sock ns-name-str)
    (swap! state assoc-in [:ns :name] ns-name-str)))

(defn set-ns-name! [sock nsname]
  (swap! state assoc-in [:ns :name] nsname)
  (metadata-for-ns-publics! sock nsname))

;;; metadata for ns-publics

(defn metadata-for-ns-publics! [sock ns-name]
  (->> `(brepl.tasks/handle {:task :metadata-for-ns-publics :name ~ns-name})
       str
       (.send sock)))

(defmethod handle-task-result :metadata-for-ns-publics
  [{:keys [result]} _sock]
  (->> result
       (sort-by :name)
       (swap! state assoc-in [:ns :publics])))

;;; metadata for a symbol

(defn metadata-for-symbol! [sock ns-name symbol-name]
  (->> `(brepl.tasks/handle {:task :metadata-for-symbol :namespace ~ns-name :name ~symbol-name})
       str
       (.send sock)))

(defmethod handle-task-result :metadata-for-symbol
  [{:keys [result]} _sock]
  (swap! state assoc-in [:metadata (name (:ns result)) (name (:name result))] result))


;;; list all namespace names

(defn list-all-ns-names! [sock]
  (->> `(brepl.tasks/handle {:task :list-all-ns-names})
       str
       (.send sock)))

(defmethod handle-task-result :list-all-ns-names
  [{:keys [result]} _sock]
  (let [ns-names (map name result)]
    (->> ns-names
         sort
         (swap! state assoc :all-ns-names))))

(defn ns-name-tree [ns-names]
  (->> ns-names
       (map as-segments)
       (prefix-tree :leaf ".")))

;;; apropos

(defn apropos! [sock pattern]
  (swap! state assoc-in [:apropos :term] pattern)
  (->> `(brepl.tasks/handle {:task :apropos :pattern ~pattern})
       str
       (.send sock)))

(defmethod handle-task-result :apropos
  [{:keys [result]}]
  (swap! state assoc-in [:apropos :results] result))


;;; initialize the tasks websocket


(defn init! [hostname ws-port prepl-port]
  (let [sock (websockets/create! hostname ws-port prepl-port)]
    ;; set the socket
    (reset! ws sock)
    (.addEventListener sock "open" (fn [_]
                                     (inject-clojure! "/test.clj" sock)))
    (.addEventListener sock "close" (fn [_]
                                      (reset! ws nil)
                                      (reset! state empty-tasks-state)))
    (.addEventListener sock "message"
                       (fn [event]
                         (try
                           (let [task (->> (.-data event)
                                           edn/read-string
                                           :val
                                           edn/read-string)]
                             (println {:tasks (:task task)})
                             (handle-task-result task sock))
                           (catch js/Error _ nil))))
    (.addEventListener sock "error"
                       (fn [event]
                         (println event)
                         (reset! state empty-tasks-state)))))
