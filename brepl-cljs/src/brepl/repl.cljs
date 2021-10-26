(ns brepl.repl)

;;;
;;; A repl in a DIV

(defn repl []
  [:div {:content-editable true
         :on-key-down (fn [event] (js/console.log event))
         :on-input (fn [input-event] (js/console.log input-event))
         :on-key-up (fn [event] (js/console.log event))
         :style {:width "100%"
                 :height "100%"
                 :background-color "grey"
                 :color "black"}}])
