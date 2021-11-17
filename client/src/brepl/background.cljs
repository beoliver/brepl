(ns brepl.background
  (:require [brepl.backend :as backend]
            [reagent.core :as r]
            [cljs.pprint :refer [cl-format]]
            [cljs.tools.reader.edn :as edn]
            [clojure.string :as str]))

(defn eval-clj-file [path backend callback]
  ;; (read-clojure (fn [data] (println data)))
  (let [client (new js/XMLHttpRequest)]
    (.open client "GET" path)
    (.addEventListener client "load"
                       (fn [_event]
                         (let [code (.-responseText client)
                               do-code (cl-format nil "(do ~a)" code)]
                           (backend/send backend do-code callback))))
    (.send client)))

;;; --------------------------------------------------------------------------------

(defmulti handle-result :task)

(defn handle-task-result [data]
  (handle-result (edn/read-string data)))

;;; --------------------------------------------------------------------------------

(defonce all-ns-names (r/atom nil))

(defn list-all-ns-names [backend]
  (let [task (str `(brepl.tasks/handle {:task :list-all-ns-names}))]
    (backend/send backend task handle-task-result)))

(defmethod handle-result :list-all-ns-names
  [{:keys [result]}]
  (->> result
       (map name)
       sort
       (reset! all-ns-names)))

(defn all-ns-names-component [backend]
  [:div
   [:input {:type "button"
            :value "RELOAD ALL NS NAMES"
            :on-click (fn [_] (list-all-ns-names backend))}]
   (into [:div {:style {:background-color "pink"}}]
         (map-indexed (fn [i x] ^{:key i} [:div x]) @all-ns-names))])

;;; --------------------------------------------------------------------------------

(defn split-namespaces [namespaces]
  (map #(str/split % #"\.") namespaces))

(defn ns-tree
  ([namespaces]
   (ns-tree [] (split-namespaces namespaces)))
  ([path paths]
   (->> paths
        (group-by first)
        (map (fn [[prefix paths]]
               (let [updated-path (conj path prefix)
                     remaining (keep (comp seq rest) paths)
                     tree (ns-tree (conj path prefix) remaining)]
                 [prefix (if (= (count remaining) (count paths))
                           tree
                           (assoc tree :ns (str/join "." updated-path)))])))
        (into {}))))

(defn display-tree
  ([tree] (display-tree 0 tree))
  ([depth {:keys [ns] :as tree}]
   (cond
     (and (= 1 (count tree)) ns)
     [:div {:style {:padding-left (str (* depth 10) "px") :background-color "pink"}} ns]
     (and (= 1 (count tree)) (nil? ns))
     (let [[prefix children] (first tree)]
       (->> children
            (map (fn [[k v]] [(if (= :ns k) k (str/join "." [prefix k])) v]))
            (into {})
            (display-tree (inc depth))))
     :else (->> tree
                (sort-by (fn [[k _]] (if (= :ns k) "" k)))
                (reduce (fn [acc [k v]]
                          (cond (= k :ns) [:div {:style {:padding-left (str (* depth 10) "px") :background-color "purple"}} ns]
                                :else     (conj acc ^{:key k}[:div k (display-tree (inc depth) v)])))
                        [:div {:style {:padding-left (str (* depth 10) "px") :background-color "orange"}}])))))

(defn ns-tree-component [backend]
  [:div
   [:input {:type "button"
            :value "RELOAD ALL NS NAMES"
            :on-click (fn [_] (list-all-ns-names backend))}]
   (display-tree (ns-tree @all-ns-names))])
