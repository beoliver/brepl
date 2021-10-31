(ns brepl.components)

(defn circle [color]
  [:div {:style {:background-color color
                 :border-radius "50%"
                 :height "0.8em"
                 :width "0.8em"
                 :margin-right "0.2em"}}])


(defn port-input [style port-atom]
  [:input {:style style
           :type "text"
           :placeholder "PORT"
           :maxLength "5"             ; 65535
           :size "5"                  ; 65535
           :value @port-atom
           :on-change #(reset! port-atom (-> % .-target .-value))}])
