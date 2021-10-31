(ns ^:figwheel-hooks brepl.browser
  (:require [brepl.sockets :as ws]
            [brepl.repl :as repl]
            [brepl.utils :as utils]
            [brepl.config :as config]
            [cljs.tools.reader.edn :as edn]
            [reagent.core :as r]))


(def sock-name :browser/prepl)

(defn close! [] (ws/socket-close! sock-name))

(defonce connected? (r/atom false))
(defonce error? (r/atom false))
(defonce current-namespace (r/atom nil))

;;;
(def state (r/atom {:ns-regex {:remove nil :filter nil} ;; match everything
                    :brepl-ns nil
                    :ns {:name nil :publics nil}
                    :all-ns-names nil
                    :apropos {:term nil :results nil}}))
;;;

(defmulti on-task-result :task)

;;;
(declare query-ns-name
         list-all-ns-names
         metadata-for-ns-publics)
;;;

(defn inject-clojure! []
  (let [path "/test.clj"]
    (letfn [(callback [data] (ws/socket-write! sock-name data))]
      (utils/read-clojure path callback))))

(defmethod on-task-result :inject-clojure
  [{:keys [result]}]
  (swap! state assoc :brepl-ns result)
  (query-ns-name)
  (list-all-ns-names))

(defn query-ns-name []
  (->> {:task :query-ns-name :result '(ns-name *ns*)}
       str
       (ws/socket-write! sock-name)))

(defmethod on-task-result :query-ns-name
  [{:keys [result]}]
  (let [ns-name-str (str result)]
    (metadata-for-ns-publics ns-name-str)
    (swap! state assoc-in [:ns :name] ns-name-str)))

(defn metadata-for-ns-publics [ns-name-str]
  (->> `(brepl.tasks/handle {:task :metadata-for-ns-publics :name ~ns-name-str})
       str
       (ws/socket-write! sock-name)))

(defmethod on-task-result :metadata-for-ns-publics
  [{:keys [result]}]
  (->> result
       (sort-by :name)
       (swap! state assoc-in [:ns :publics])))

(defn metadata-for-symbol [ns-name symbol-name]
  (->> `(brepl.tasks/handle {:task :metadata-for-symbol :namespace ~ns-name :name ~symbol-name})
       str
       (ws/socket-write! sock-name)))

(defmethod on-task-result :metadata-for-symbol
  [{:keys [result]}]
  (swap! state assoc-in [:metadata (name (:ns result)) (name (:name result))] result))


;;; list all namespace names

(defn list-all-ns-names []
  (->> `(brepl.tasks/handle {:task :list-all-ns-names})
       str
       (ws/socket-write! sock-name)))

(defmethod on-task-result :list-all-ns-names
  [{:keys [result]}]
  (let [ns-names (map name result)]
    (->> ns-names
         sort
         (swap! state assoc :all-ns-names))))

(defn ns-name-tree [ns-names]
  (->> ns-names
       (map utils/namespace-as-segments)
       (utils/prefix-tree :leaf ".")))


;;; apropos

(defn apropos [pattern]
  (swap! state assoc-in [:apropos :term] pattern)
  (->> `(brepl.tasks/handle {:task :apropos :pattern ~pattern})
       str
       (ws/socket-write! sock-name)))

(defmethod on-task-result :apropos
  [{:keys [result]}]
  (swap! state assoc-in [:apropos :results] result))




(defn set-ns-name! [nsname]
  (swap! state assoc-in [:ns :name] nsname)
  (repl/set-ns! nsname)
  (metadata-for-ns-publics nsname))



(defmethod ws/on-socket-open sock-name
  [_ _]
  (reset! connected? true)
  (reset! error? false)
  (inject-clojure!))

(defmethod ws/on-socket-close sock-name
  [_ _] (reset! connected? false))

(defmethod ws/on-socket-message sock-name
  [_ event] (try
              (let [task (->> (.-data event)
                              edn/read-string
                              :val
                              edn/read-string)]
                (println {:tasks (:task task)})
                (on-task-result task))
              (catch js/Error e (js/console.log e))))

(defmethod ws/on-socket-error sock-name
  [_ _]
  (reset! connected? false)
  (reset! error? true))


;;; COMPONENTS
;;; display a folding tree for namespaces

(defn recursive-ns-tree-component [display-config tree depth]
  ;; reduce over the elements of the tree.
  (->> tree
       (sort-by first)
       (reduce (fn [acc [prefix children]]
                 (cond
                   ;; case 1. There is EXACTLY one child and it is a `:leaf`
                   ;; {"b.c" {:leaf "a.b.c"}
                   (and (:leaf children) (= 1 (count children)))
                   ;; the current prefix is the last segment in a namespace path
                   ;; eg `clojure.main`
                   (let [full-ns-name (:leaf children)
                         display-value (if (:ns-tree-path @config/config) full-ns-name prefix)]
                     (conj acc [:div {:style {:margin-top "2px" :color "blue" :background-color "#fafafa"}
                                      :on-click (fn [] (set-ns-name! full-ns-name))}
                                [:b display-value]]))
                   ;; case 2. This is a `:leaf` in the children
                   ;; {"b.c" {:leaf "a.b.c", "d" {:leaf "a.b.c.d"}}}
                   (:leaf children)
                   ;; the current prefix is the last segment in a namespace path
                   ;; the current prefix is a prefix to some other namespace
                   ;; eg `clojure.core` and `clojure.core.server`
                   (let [full-ns-name (:leaf children)
                         display-value (if (:ns-tree-path @config/config) full-ns-name prefix)]
                     (conj acc [:details {:style {:margin-top "2px" :background ((:depth->color display-config) depth)}}
                                [:summary prefix]
                                [:div {:style {:margin-top "2px" :color "blue" :background-color "#fafafa"}
                                       :on-click (fn [] (set-ns-name! full-ns-name))}
                                 [:b display-value]]
                                (recursive-ns-tree-component display-config (dissoc children :leaf) (inc depth))]))
                   ;; case 3. No leaves here...
                   :else
                   (conj acc [:details {:style {:margin-top "2px" :background ((:depth->color display-config) depth)}}
                              [:summary prefix]
                              (recursive-ns-tree-component display-config children (inc depth))])))
               [:div {:style {:background "#fafafa"
                              :padding-left (if (= depth 0) "0px" "10px")
                              :margin-left "2px"
                              :padding-top "0.3em"
                              :padding-bottom "0.3em"}}])))




(defn ns-tree-component [display-config]
  (let [ns-names (get @state :all-ns-names)
        ns-re-filter-fn (->> (re-pattern (:ns-re-filter @config/config))
                             (partial re-find))
        ns-re-remove-fn (if-let [p (:ns-re-remove @config/config)]
                          (partial re-find (re-pattern p))
                          (constantly false))
        tree (->> ns-names
                  (filter ns-re-filter-fn)
                  (remove ns-re-remove-fn)
                  ns-name-tree)]
    (when (seq ns-names)
      [:div
       {:style {:font-family "'JetBrains Mono', monospace" :font-size "0.8em"}}
       [recursive-ns-tree-component display-config tree 0]])))



(defn ns-regex-input [{:keys [key error? pattern]} update-fn]
  [:input {:spellCheck false
           :style {:font-family "monospace"
                   :font-size "1em"
                   :color (if error? "red" "black")
                   :width "100%"}
           :value pattern
           :on-change #(->> % .-target .-value (update-fn key))}])



(defn ns-component []
  (let [filter-pattern (r/atom {:key :ns-re-filter
                                :error? false
                                :pattern (:ns-re-filter @config/config)})
        remove-pattern (r/atom {:key :ns-re-remove
                                :error? false
                                :pattern (:ns-re-remove @config/config)})

        display-config (r/atom {:expand-leaves false
                                :depth->color {0 "#ebebff" 1 "#d8d8ff" 2 "#c4c4ff" 3 "#b1b1ff" 4 "#9d9dff"} #_{0 "gold" 1 "green" 2 "red" 3 "orange" 4 "brown" 5 "yellow"}})

        update-pattern-fn (fn [key pat]
                            (try (re-pattern pat)
                                 (case key
                                   :ns-re-filter (do (swap! config/config assoc :ns-re-filter pat)
                                                     (swap! filter-pattern merge {:error? false :pattern pat}))
                                   :ns-re-remove (do
                                                   (swap! config/config assoc :ns-re-remove (when-not (= pat "") pat))
                                                   (swap! remove-pattern merge {:error? false :pattern pat})))
                                 (catch js/Error _
                                   (case key
                                     :ns-re-filter (swap! filter-pattern merge {:error? true :pattern pat})
                                     :ns-re-remove (swap! remove-pattern merge {:error? true :pattern pat})))))]
    (fn []
      [:details {:open true}
       [:summary {:style {:font-family "sans-serif" :padding "1em 1em" :background-color "#a1a1f1"}}
        [:b "loaded namespaces"] ]
       [:div {:style {:padding "1em 1em" :background-color "#fafafa"}}
        [:div {:style {:padding "0em 0em"}}
         [:span
          "Expand Leaf Paths"
          [:input {:type "checkbox"
                   :defaultChecked (:ns-tree-path @config/config)
                   :on-click (fn [] (swap! config/config update :ns-tree-path not))}]]
         [:input {:type "button"
                  :value "Reload Namespaces"
                  :on-click (fn [] (list-all-ns-names))}]
         [:div {:style {:display "flex" :align-items "center"}} [:div {:style {:width "5em"}} "filter"] [ns-regex-input
                                                                                                         @filter-pattern
                                                                                                         update-pattern-fn]]
         [:div {:style {:display "flex" :align-items "center"}} [:div {:style {:width "5em"}} "remove"] [ns-regex-input
                                                                                                         @remove-pattern
                                                                                                         update-pattern-fn]]
         [ns-tree-component @display-config]]
        ]]
      )))







(defn ns-selector-component []
  ;; a dropdown menu for "selecting" a current namespace
  ;; this does not actually move the repl into that ns
  ;; it is used so that context dependent results can be
  ;; returned to the user
  (let [all-ns-names (get @state :all-ns-names)
        current-ns-name (get-in @state [:ns :name])]
    (when (and (seq all-ns-names) current-ns-name)
      [:div {:style {:background-color "orange"}}
       [:span "NS: "
        [:select
         {:value current-ns-name
          :onChange (fn [x]
                      (let [selected-ns (-> x .-target .-value)]
                        (set-ns-name! selected-ns)))}
         (->> all-ns-names
              (map-indexed (fn [i ns]
                             ^{:key i} [:option {:value ns} ns])))]]])))


(defn ns-publics-metadata-component []
  ;; a list of all "documentation" for public vars in a given ns
  ;; will display the publics for the currently selected ns
  ;; using then `ns-selector-component`

  [:details
   [:summary {:style {:font-family "sans-serif" :padding "1em 1em" :background-color "salmon"}}
    [:b "ns-publics"] ]
   [:div {:style {:padding "1em 1em" :background-color "#fafafa"}}
    (doall (map-indexed
            (fn [i msg]
              ^{:key i} [:div
                         {:style {:padding "1em 1em" :margin "0 0"
                                  :background-color "#fafafa"
                                  :color "black"
                                  :border-bottom "1px solid black"}}
                         [:div {:style {:overflow-x "auto"
                                        :padding "0 0"
                                        :margin "0 0"}}
                          [:pre [:b (:name msg)]]
                          [:pre (str (:arglists msg))]
                          [:pre (:doc msg)]]])
            (get-in @state [:ns :publics])))]])



;;; APROPOS

(defn input-field [partial-expr]
  [:input {:spellCheck false
           :style {:width "100%"}
           :value @partial-expr
           :on-change #(let [s (-> % .-target .-value)]
                         (apropos s)
                         (reset! partial-expr s))}])

(defn apropos-search-component []
  (let [query (r/atom "")]
    (fn []
      [:div {:style {
                     :height "3em"
                     :display "flex"
                     :align-items "center"}}
       [input-field query]
       #_[:input {:type "button"
                  :value "Search regex"
                  :on-click (fn [_] (tasks/apropos! @tasks/ws @query))}]])))

(defn metadata-component [namespace what]
  (let [msg (get-in @state [:metadata namespace what])]
    [:div
     {:style {:margin-bottom "2em"
              :background-color "#fafafa"
              :color "black"
              }}
     [:div {:style {:overflow-x "auto"
                    :padding "0 0"
                    :margin "0 0"}}
      [:pre (str (:arglists msg))]
      [:pre (:doc msg)]]]))

(defn apropos-grouped-results-component []
  (let [current-ns (get-in @state [:ns :name])
        ns-pattern (re-pattern (:ns-re-filter @config/config))
        ns-ignore-pattern (when-let [p (:ns-re-remove @config/config)] (re-pattern p))]
    [:div {:style {:font-family "'JetBrains Mono', monospace" :font-size "0.8em"}}
     (doall (some->> (get-in @state [:apropos :results])
                     (group-by namespace)
                     (sort-by first) ; sort by the keys of the map (the namespaces)
                     (filter (fn [[ns-str _]] (and (re-find ns-pattern ns-str)
                                                   (if ns-ignore-pattern
                                                     (nil? (re-find ns-ignore-pattern ns-str))
                                                     true))))
                     (map (fn [[ns-str ns-results]] ^{:key ns-str}
                            [:details {:open true :style {:padding-top "1em"}}
                             [:summary [:span {:style {:color (if (= current-ns ns-str) "green" "blue")}} ns-str]]
                             [:div {:style {:overflow-x "auto" :padding-top "0.5em"}}
                              [:div {:style {:padding-left "0.5em"}}
                               (doall (some->> ns-results
                                               (map name) ;; 'clojure.core/conj becomes "conj"
                                               (map (fn [s] ^{:key s}
                                                      [:details
                                                       {:style {:margin-top "0.7em"
                                                                :margin-bottom "0.7em"
                                                                :padding-bottom "0.7em"
                                                                :padding-top "0.4em"
                                                                :border-bottom "0.5px solid black"
                                                                }}
                                                       [:summary {:on-click (fn [_] (metadata-for-symbol ns-str s))} [:span [:b s]]]
                                                       [:div [metadata-component ns-str s]]]))))]]]))))]))

(defn apropos-component []
  [:details
   [:summary {:style {:font-family "sans-serif" :padding "1em 1em" :background-color "#f1f1f1"}}
    [:b "apropos..."] ]
   [:div {:style {:padding "1em 1em" :background-color "#fafafa"}}
    [apropos-search-component]
    [apropos-grouped-results-component]]])
