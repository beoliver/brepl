(ns ^:figwheel-hooks brepl.namespaces
  (:require
   [cljs.pprint :refer [cl-format]]
   [brepl.ws :as ws]
   [cljs.tools.reader.edn :as edn]
   [reagent.core :as r]))

(defonce namespaces-ws (atom nil))

(def empty-namespaces {:ns {:name nil :publics nil}
                       :names nil})

(def apropos-state (r/atom {:term nil :result nil}))

(def state (r/atom empty-namespaces))

(defn list-namespace-names! [sock]
  (->> {:task :list-namespace-names :result '(mapv ns-name (all-ns))}
       str
       (.send sock)))

(defn fetch-ns-publics! [sock ns-name]
  (->> ns-name
       (cl-format nil "{:task :ns-publics :result (->> '~a ns-publics vals (map meta) (map #(select-keys % [:arglists :doc :line :column :file :name :macro])))}")
       (.send sock)))

(defn search-for-apropos! [sock regex]
  (->> (cl-format nil "{:task :apropos :input \"~a\" :result (clojure.repl/apropos #\"~a\")}" regex regex)
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

(defmethod handle-task :apropos
  [{:keys [result input] :as data}]
  (println data)
  (reset! apropos-state {:term input :result result}))

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
                           (println (:task task))
                           (handle-task task))))
    (.addEventListener sock "error"
                       (fn [event]
                         (swap! state assoc :error event)))))



(defn namespace-thingy []
  (let [{:keys [ns names]} @state]
    (when (seq names)
      [:div
       [:span "namespace:"
        [:select
         {:value (:name ns)
          :onChange (fn [x]
                      (let [selected-ns (-> x .-target .-value)]
                        (swap! state assoc-in [:ns :name] selected-ns)
                        (fetch-ns-publics! @namespaces-ws selected-ns)))}
         (->> names
              (map-indexed (fn [i ns]
                             ^{:key i} [:option {:value ns} ns])))]]])))

(defn expr-input [partial-expr]
  [:input {:spellcheck false
           :style {:font-family "monospace"
                   :font-size "1em"
                   :width "100%"}
           :value @partial-expr
           :on-change #(reset! partial-expr (-> % .-target .-value))}])

(defn apropos []
  (let [query (r/atom "")]
    (fn []
      [:div {:style {
                     :height "3em"
                     :font-family "monospace"
                     :display "flex"
                     :align-items "center"}}
       [expr-input query]
       [:input {:type "button"
                :value "Search regex"
                :on-click (fn [_] (search-for-apropos! @namespaces-ws @query))}]])))


(defn response-div [ns-public-meta]
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

(defn apropos-thingy []
  [:div {:style {:font-size "1em" :x-overflow "hidden"}}
   (doall (map-indexed
           (fn [i msg]
             ^{:key i} [:div
                        {:style {:padding-left "1em" :margin "0 0"
                                 :font-family "monospace"
                                 :background-color "#fafafa"
                                 :color "black"
                                 :border-bottom "1px solid black"}}
                        [:span {:style {:color "blue"}} (namespace msg)]
                        "/"
                        [:span [:b (name msg)]]
                        ])
           (get-in @apropos-state [:result])))])

(defn namespace-info []
  [:<>
   [:div
    [apropos]
    [apropos-thingy]]
   #_[namespace-thingy]
   #_[ns-publics-thingy]
   ])
