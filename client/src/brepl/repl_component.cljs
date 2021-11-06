(ns brepl.repl-component
  (:require [brepl.backend :as backend]
            [brepl.views :as views]
            [reagent.core :as r]))

(def backwards-regex #"^.*[\{\},\s\(\)\[\]]+([\!\?\'=a-zA-Z0-9\*\+_-]+)$")

(defn first-backwards-prefix [s]
  (second (re-matches backwards-regex s)))


(defn repl-input
  "
  Lets the user eval the content using `Shift+Enter`
  "
  ([repl] (repl-input {} repl))
  ([style repl]
   (let [shift-active (r/atom false)
         expr         (r/atom "")]
     (fn []
       [:textarea {:resize "none"
                   :auto-capitalize "none"
                   :auto-complete "off"
                   :auto-correct "off"
                   :spell-check "false"
                   :on-key-down (fn [event]
                                  #_(js/console.log event)
                                  (let [k (.-key event)]
                                    (cond (= k "Tab") (do (.preventDefault event)
                                                          (js/console.log event)
                                                          (let [n (->> event .-target .-selectionEnd)]
                                                            (when-let [prefix (first-backwards-prefix (.slice @expr 0 n))]
                                                              (backend/complete repl prefix))))
                                          (= k "Shift") (reset! shift-active true)
                                          (and (= k "Enter") @shift-active) (do (.preventDefault event)
                                                                                (backend/send repl @expr)
                                                                                (reset! shift-active false))
                                          :else nil)))
                   :on-key-up (fn [event]
                                #_(js/console.log event)
                                (when (= "Shift" (.-key event))
                                  (reset! shift-active false)))
                   :on-input (fn [event]
                               #_(js/console.log event)
                               (->> event .-target .-value (reset! expr)))
                   ;; white-space "pre-wrap" is vital or newlines and spaces cause errors!!!
                   :style (merge {:white-space "pre-wrap"} style)}]))))

(defn repl-completions
  ([repl] (repl-completions {} repl))
  ([style repl]
   (into [:div {:style style}]
         (map-indexed (fn [i {:keys [candidate type]}] ^{:key i}
                        [:div {:style {:display "flex" :height "1.6em" }}
                         [:div candidate]
                         [:div type]])
                      (->> repl backend/active-completions deref)))))

(defn repl-output
  ([repl] (repl-output {} repl))
  ([style repl]
   (let [html-elems (->> repl
                         backend/history
                         deref
                         (keep views/as-html))]
     (into [:div {:style style}]
           (map-indexed (fn [i x] ^{:key i}
                          [:div {:style {:height "1.6em" :overflow-x "hidden"}} x])
                        html-elems)))))


(defn repl-component [global-style input-style output-style backend]
  [:div {:style global-style}
   [repl-input
    input-style
    (:repl @backend)]
   [repl-completions
    (:repl @backend)]
   [repl-output
    output-style
    (:repl @backend)]])
