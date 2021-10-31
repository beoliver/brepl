(ns brepl.repl
  (:require [brepl.sockets :as sockets]
            [reagent.core :as r]))

(def repl-impl-name (r/atom nil))


(def connected? (r/atom false))
(def error? (r/atom false))


(def history (r/atom nil))
(defmulti remote-eval! (fn [_data] @repl-impl-name))
(defmulti history-item->component (fn [_data] @repl-impl-name))


(defn connect! [info]
  (let [repl-type (get-in info [:repl :type])
        k (keyword :user-repl repl-type)]
    (reset! repl-impl-name k)
    (sockets/new-named-socket! k info)))



(defn input-component
  "
  Lets the user eval the content using `Shift+Enter`
  "
  []
  (let [shift-active (r/atom false)
        expr         (r/atom "")]
    (fn []
      [:div {:content-editable @connected?
             :on-key-down (fn [event]
                            (let [k (.-key event)]
                              (cond (= k "Tab") (.preventDefault event) ;; should also insert white space...
                                    (= k "Shift")                     (reset! shift-active true)
                                    (and (= k "Enter") @shift-active) (do (remote-eval! @expr)
                                                                          ;; avoid spamming the server?
                                                                          (reset! shift-active false))
                                    :else                             nil)))
             :on-key-up (fn [event]
                          (when (= "Shift" (.-key event))
                            (reset! shift-active false)))
             :on-input #(->> % .-target .-innerText (reset! expr))
             :style {:white-space "pre-wrap" ;; this is vital or newlines and spaces cause errors!!!
                     ;;:padding "1em 1em"
                     :width "100%"
                     :max-height "30vh"
                     :min-height "30vh"
                     :overflow-y "auto"
                     :background-color (if @connected? "#000000" "grey")
                     :font-family "'JetBrains Mono', monospace"
                     :font-size "0.8em"
                     :color (if @connected? "#fafafa" "LightGray")}}])))


(defn output-component
  []
  (into [:div {:style {:height "100%"
                       :padding "1em 1em"
                       :font-family "'JetBrains Mono', monospace"
                       :font-size "0.8em"}}]
        (map-indexed (fn [i x] ^{:key i} [:div {:style {:height "1.6em"
                                                        :overflow-x "hidden"}}
                                          [history-item->component x]]) @history)))
