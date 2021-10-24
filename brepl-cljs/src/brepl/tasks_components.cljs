(ns ^:figwheel-hooks brepl.tasks-components
  (:require [brepl.tasks :as tasks]
            [reagent.core :as r]))

;;; COMPONENTS


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
  [:details {:style {:padding "1em 1em"}}
   [:summary "ns-publics" ]
   [:div {:style {:font-size "1em"}}
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

(defn expr-input [partial-expr]
  [:input {:spellCheck false
           :style {:font-family "monospace"
                   :font-size "1em"
                   :width "100%"}
           :value @partial-expr
           :on-change #(reset! partial-expr (-> % .-target .-value))}])

(defn apropos-search-component []
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
  (let [current-ns (get-in @tasks/state [:ns :name])]
    [:div {:style {:font-size "1em" :font-family "monospace"}}
     (doall (some->> (get-in @tasks/state [:apropos :results])
                     (group-by namespace)
                     (sort-by first) ; sort by the keys of the map (the namespaces)
                     (map (fn [[ns-str ns-results]]
                            ^{:key ns-str} [:details {:open true :style {:padding-top "1em"}}
                                            [:summary [:span {:style {:color (if (= current-ns ns-str) "green" "blue")}} ns-str]]
                                            [:div {:style {:overflow-x "auto" :padding-top "0.5em"}}
                                             [:div {:style {:padding-left "0.5em"}}
                                              (doall (some->> ns-results
                                                              (map name) ;; 'clojure.core/conj becomes "conj"
                                                              (map (fn [s] ^{:key s} [:details {:style {
                                                                                                        ;; :background-color "pink"
                                                                                                        :margin-top "0.7em"
                                                                                                        :margin-bottom "0.7em"
                                                                                                        :padding-bottom "0.5em"
                                                                                                        :padding-top "0.5em"
                                                                                                        :border-bottom "0.5px solid black"
                                                                                                        }}
                                                                                      [:summary {:on-click (fn [_] (tasks/metadata-for-symbol! @tasks/ws ns-str s))} [:span [:b s]]]
                                                                                      [:div [metadata-component ns-str s]]]))))]]]))))]))

(defn apropos-component []
  [:details
   [:summary {:style {:font-family "sans-serif" :padding "1em 1em" :background-color "#f1f1f1"}}
    [:b "apropos"] ]
   [:div {:style {:padding "1em 1em" :background-color "#fafafa"}}
    [apropos-search-component]
    [apropos-grouped-results-component]]])
