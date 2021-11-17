(ns brepl.views)

(defprotocol DisplayHTML
  (as-html [x]))

;;; --------------------------------------------------------------------------------

(defn flex [style & rest]
  (into [:div {:style (merge style {:display "flex"})}] rest))

;;; --------------------------------------------------------------------------------
(def history-styles
  {"R" {:icon-color "#68e266" :background-color "#ade5ac" :text-color "black"}
   "O" {:icon-color "#59a6ff" :background-color "#add8e6" :text-color "black"}
   "E" {:icon-color "#ff4d4d" :background-color "#e6baac" :text-color "black"}
   "X" {:icon-color "#ff4d4d" :background-color "#e6baac" :text-color "black"}
   ">" {:icon-color "#ffffff" :background-color "black"   :text-color "#ffffff"}})

(defn indicator [label]
  [:div
   {:style
    {:padding-right "1em" :color (get-in history-styles [label :icon-color])}}
   [:b label]])

(defn history-item-html [label value]
  (flex {} (indicator label)
        [:div
         {:style
          {:width "100%"
           :color (get-in history-styles [label :text-color])
           :background-color (get-in history-styles [label :background-color])}}
         value]))
