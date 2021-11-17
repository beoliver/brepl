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
   (let [active (r/atom #{})
         expr   (r/atom "")]
     (fn []
       [:textarea {:auto-capitalize "none"
                   :auto-complete "off"
                   :auto-correct "off"
                   :spell-check "false"
                   :on-key-down (fn [event]
                                  (let [k (.-key event)]
                                    (js/console.log k)
                                    (cond (and (= k "Tab") (@active "Alt"))
                                          (do (.preventDefault event)
                                              (js/console.log event)
                                              (let [n (->> event .-target .-selectionEnd)]
                                                (when-let [prefix (first-backwards-prefix (.slice @expr 0 n))]
                                                  (backend/complete repl prefix))))

                                          (= k "Tab")
                                          (.preventDefault event)

                                          (and (= k "Enter") (@active "Shift"))
                                          (do (.preventDefault event) (backend/send repl @expr nil))

                                          :else (swap! active conj k))))
                   :on-key-up (fn [event] (swap! active disj (.-key event)))
                   :on-input (fn [event] (->> event .-target .-value (reset! expr)))
                   ;; white-space "pre-wrap" is vital or newlines and spaces cause errors!!!
                   :style (merge {:white-space "pre-wrap"} style)}]))))

(defn repl-completions
  ([repl] (repl-completions {} repl))
  ([style repl]
   (into [:div {:style style}]
         (map-indexed (fn [i {:keys [candidate type]}] ^{:key i}
                        [:div {:style {:display "flex"
                                       :background-color "pink"
                                       :justify-content "space-between"
                                       :height "1.6em" }}
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
