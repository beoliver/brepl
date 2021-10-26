(ns ^:figwheel-hooks brepl.tasks-components
  (:require [brepl.tasks :as tasks]
            [reagent.core :as r]
            [brepl.state :as state]))

;;; COMPONENTS

(defn recursive-ns-tree-component [display-config tree depth]
  (let [inner (dissoc tree :leaf)]
    (reduce (fn [acc [prefix children]]
              (cond
                (and (:leaf children) (= 1 (count children)))
                (conj acc [:div {:style {:color "blue" :background-color "#fafafa"}
                                 :on-click (fn [] (tasks/set-ns-name! @tasks/ws (:leaf children)))}
                           [:i {:href (:leaf children)} (if (:ns-tree-path @(:config state/state))
                                                          (:leaf children)
                                                          prefix)]])
                ;;
                (:leaf children)
                (conj acc [:details {;; :open (< depth (:ns-tree-open-depth @(:config state/state)))
                                     :style {:background ((:depth->color display-config) depth)}}
                           [:summary [:b [:a {:href (:leaf children)} (:leaf children)]]]
                           (recursive-ns-tree-component display-config (dissoc children :leaf) (inc depth))])
                ;;
                :else
                (conj acc [:details {;; :open (< depth (:ns-tree-open-depth @(:config state/state)))
                                     :style {:background ((:depth->color display-config) depth)}}
                           [:summary prefix]
                           (recursive-ns-tree-component display-config children (inc depth))])))
            [:div {:style {:background "#fafafa" :padding-left (if (= depth 0) "0px" "10px") :margin-left "2px" :padding-top "5px" :padding-bottom "5px"}}]
            (sort-by first inner))))


(defn ns-tree-component [display-config]
  (let [ns-names (get @tasks/state :all-ns-names)
        ns-re-filter-fn (->> (re-pattern (:ns-re-filter @(:config state/state)))
                             (partial re-find))
        ns-re-remove-fn (if-let [p (:ns-re-remove @(:config state/state))]
                          (partial re-find (re-pattern p))
                          (constantly false))
        tree (->> ns-names
                  (filter ns-re-filter-fn)
                  (remove ns-re-remove-fn)
                  tasks/ns-name-tree)]
    (when (seq ns-names)
      [:div
       {:style {:font-family "monospace"}}
       [recursive-ns-tree-component display-config tree 0]])))

;;; NAMESPACE REGEX

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
                                :pattern (:ns-re-filter @(:config state/state))})
        remove-pattern (r/atom {:key :ns-re-remove
                                :error? false
                                :pattern (:ns-re-remove @(:config state/state))})

        display-config (r/atom {:expand-leaves false
                                :depth->color {0 "gold" 1 "green" 2 "red" 3 "orange" 4 "brown" 5 "yellow"}})

        update-pattern-fn (fn [key pat]
                            (try (re-pattern pat)
                                 (case key
                                   :ns-re-filter (do (update state/state :config swap! assoc key pat)
                                                     (swap! filter-pattern merge {:error? false :pattern pat}))
                                   :ns-re-remove (do
                                                   (update state/state :config swap! assoc key (when-not (= pat "") pat))
                                                   (swap! remove-pattern merge {:error? false :pattern pat})))
                                 (catch js/Error _
                                   (case key
                                     :ns-re-filter (swap! filter-pattern merge {:error? true :pattern pat})
                                     :ns-re-remove (swap! remove-pattern merge {:error? true :pattern pat})))))]
    (fn []
      [:details
       [:summary {:style {:font-family "sans-serif" :padding "1em 1em" :background-color "#a1a1f1"}}
        [:b "Namespace Browser"] ]
       [:div {:style {:padding "1em 1em" :background-color "#fafafa"}}
        [:p
         "Use regular expressions to filter the tree of" [:em " visible "] "namsepaces. "
         "When searching for symbols, the displayed results will belong to a" [:i " visible "] "namespace."
         ]
        [:div {:style {:padding "0em 0em"}}
         [:span
          "Expand Leaf Paths"
          [:input {:type "checkbox"
                   :defaultChecked (:ns-tree-path @(:config state/state))
                   :on-click (fn [] (update state/state :config swap! update :ns-tree-path not))}]]
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
  (let [all-ns-names (get-in @tasks/state [:all-ns-names])
        current-ns-name (get-in @tasks/state [:ns :name])]
    (when (and (seq all-ns-names) current-ns-name)
      [:div {:style {:background-color "orange"}}
       [:span "NS: "
        [:select
         {:value current-ns-name
          :onChange (fn [x]
                      (let [selected-ns (-> x .-target .-value)]
                        (tasks/set-ns-name! @tasks/ws selected-ns)))}
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
            (get-in @tasks/state [:ns :publics])))]])



;;; APROPOS

(defn input-field [partial-expr]
  [:input {:spellCheck false
           :style {:font-family "monospace"
                   :font-size "1em"
                   :width "100%"}
           :value @partial-expr
           :on-change #(let [s (-> % .-target .-value)]
                         (tasks/apropos! @tasks/ws s)
                         (reset! partial-expr s))}])

(defn apropos-search-component []
  (let [query (r/atom "")]
    (fn []
      [:div {:style {
                     :height "3em"
                     :font-family "monospace"
                     :display "flex"
                     :align-items "center"}}
       [input-field query]
       #_[:input {:type "button"
                  :value "Search regex"
                  :on-click (fn [_] (tasks/apropos! @tasks/ws @query))}]])))

(defn metadata-component [namespace what]
  (let [msg (get-in @tasks/state [:metadata namespace what])]
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
  (let [current-ns (get-in @tasks/state [:ns :name])
        ns-pattern (re-pattern (:ns-re-filter @(:config state/state)))
        ns-ignore-pattern (when-let [p (:ns-re-remove @(:config state/state))] (re-pattern p))]
    [:div {:style {:font-size "1em" :font-family "monospace"}}
     (doall (some->> (get-in @tasks/state [:apropos :results])
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
                                                       [:summary {:on-click (fn [_] (tasks/metadata-for-symbol! @tasks/ws ns-str s))} [:span [:b s]]]
                                                       [:div [metadata-component ns-str s]]]))))]]]))))]))

(defn apropos-component []
  [:details
   [:summary {:style {:font-family "sans-serif" :padding "1em 1em" :background-color "#f1f1f1"}}
    [:b "apropos..."] ]
   [:div {:style {:padding "1em 1em" :background-color "#fafafa"}}
    [apropos-search-component]
    [apropos-grouped-results-component]]])
