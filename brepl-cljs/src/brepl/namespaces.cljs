(ns ^:figwheel-hooks brepl.namespaces
  (:require
   [cljs.pprint :refer [cl-format]]
   [brepl.ws :as ws]
   [cljs.tools.reader.edn :as edn]
   [reagent.core :as r]))

(defonce namespaces-ws (atom nil))

(def empty-namespaces {:ns {:name nil :publics nil}
                       :names nil})

(def state (r/atom empty-namespaces))

(defn list-namespace-names! [sock]
  (->> {:task :list-namespace-names :result '(mapv ns-name (all-ns))}
       str
       (.send sock)))

(defn fetch-ns-publics! [sock ns-name]
  (->> ns-name
       (cl-format nil "{:task :ns-publics :result (->> '~a ns-publics vals (map meta) (map #(select-keys % [:arglists :doc :line :column :file :name :macro])))}")
       (.send sock)))

(defmulti handle-task :task)

(defmethod handle-task :list-namespace-names
  [{:keys [result]}]
  (->> result
       (map name)
       sort
       (swap! state assoc :names)))

(defmethod handle-task :ns-publics
  [{:keys [result]}]
  (->> result
       (sort-by :name)
       (swap! state assoc-in [:ns :publics])))

(defn init! [hostname ws-port prepl-port]
  (let [sock (ws/create! hostname ws-port prepl-port)]
    ;; set the socket
    (reset! namespaces-ws sock)
    (.addEventListener sock "open" (fn [_]
                                     (swap! state assoc-in [:ns :name] "user")
                                     (fetch-ns-publics! sock "user")
                                     (list-namespace-names! sock)))
    (.addEventListener sock "close" (fn [_]
                                      (reset! namespaces-ws nil)
                                      (reset! state empty-namespaces)))
    (.addEventListener sock "message"
                       (fn [event]
                         (let [task (->> (.-data event)
                                         edn/read-string
                                         :val
                                         edn/read-string)]
                           #_(println task)
                           (handle-task task))))
    (.addEventListener sock "error"
                       (fn [event]
                         (swap! state assoc :error event)))))


(defn namespace-thingy []
  (let [{:keys [ns names]} @state]
    (when (seq names)
      [:div
       [:span "namespace:" [:select
                            {:value (:name ns)
                             :onChange (fn [x]
                                         (let [selected-ns (-> x .-target .-value)]
                                           (swap! state assoc-in [:ns :name] selected-ns)
                                           (fetch-ns-publics! @namespaces-ws selected-ns)))}
                            (->> names
                                 (map-indexed (fn [i ns]
                                                ^{:key i} [:option {:value ns} ns])))]]])))



#_(defn maybe-format-val [s]
    (try (-> (edn/read-string s)
             pprint/pprint
             with-out-str)
         (catch js/Error _ s)))



(defn response-div [ns-public-meta]
  #_(println (:arglists ns-public-meta))
  #_(println (type (:arglists ns-public-meta)))
  [:div {:style {:overflow-x "auto"
                 :padding "0 0"
                 :margin "0 0"}}
   [:pre [:b (:name ns-public-meta)]]
   [:pre
    (str (:arglists ns-public-meta))]
   [:pre
    (:doc ns-public-meta)]
   ])


(defn ns-publics-thingy []
  [:div {:style {:font-size "1em"}}
   (doall (map-indexed
           (fn [i msg]
             ^{:key i} [:div
                        {:style {:padding "1em 1em" :margin "0 0"
                                 :background-color "#fafafa"
                                 :color "black"
                                 :border-bottom "1px solid black"}}
                        [response-div msg]
                        ])
           (get-in @state [:ns :publics])))])


(defn namespace-info []
  [:<>
   [namespace-thingy]
   [ns-publics-thingy]
   ])
