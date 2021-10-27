(ns ^:figwheel-hooks brepl.repl
  (:require [brepl.ws :as ws]
            [cljs.tools.reader.edn :as edn]
            [reagent.core :as r]))


(defprotocol Repl
  (open!  [this])
  (write! [this expr])
  (close! [this]))

(defrecord PreplAdapter [connection-info socket history connected? error?]
  Repl
  (write! [_ expr] (.send @socket expr))
  (close! [this] (do (.close @socket)
                     (reset! connected? false)
                     this))
  (open! [this]
    (reset! socket (ws/create! (get-in connection-info [:ws :hostname])
                               (get-in connection-info [:ws :port])
                               (get-in connection-info [:prepl :port])))
    (.addEventListener @socket "open"
                       (fn [_]
                         (reset! connected? true)
                         #_(on-open this)))
    (.addEventListener @socket "close"
                       (fn [_]
                         (reset! connected? true)))
    (.addEventListener @socket "message"
                       (fn [event]
                         (let [data (.-data event)]
                           (->> data
                                edn/read-string
                                (swap! history conj)))))
    (.addEventListener @socket "error"
                       (fn [_]
                         (reset! error? true)
                         (reset! connected? false)))
    this))


(defn prepl-adapter [ws-addr prepl-addr]
  (map->PreplAdapter {:connection-info {:ws    ws-addr
                                        :prepl prepl-addr}
                      :socket (atom nil) ;; just a normal atom
                      :history (r/atom nil)
                      :connected? (r/atom false)
                      :error? (r/atom false)}))


(comment
  (def my-repl (prepl-adapter {:hostname "localhost" :port "8080"} {:hostname "localhost" :port "8888"}))
  (open! my-repl)
  )


(defn repl-component [state send-fn]
  [:div {:content-editable true
         :on-key-down (fn [event]
                        (let [k (.-key event)]
                          (if (and (= k "Enter")
                                   (= (:keys @state) #{"Shift"}))
                            (send-fn)
                            (swap! state update :keys conj (.-key event)))))
         :on-key-up (fn [event]
                      (swap! state update :keys disj (.-key event)))
         :on-input #(->> % .-target .-innerText (swap! state assoc :expr))
         :style {:white-space "pre-wrap" ;; this is vital or newlines and spaces cause errors!!!
                 :padding "1em 1em"
                 :width "40em"
                 :height "50vh"
                 :background-color "#000000"
                 :font-family "'JetBrains Mono', monospace"
                 :font-size "0.8em"
                 :color "#fafafa"}}])


(defn repl-component-v2 [state repl]
  [:div {:content-editable true
         :on-key-down (fn [event]
                        (let [k (.-key event)]
                          (if (and (= k "Enter")
                                   (= (:keys-down @state) #{"Shift"}))
                            (write! repl (:expr @state))
                            (swap! state update :keys-down conj (.-key event)))))
         :on-key-up (fn [event]
                      (swap! state update :keys-down disj (.-key event)))
         :on-input #(->> % .-target .-innerText (swap! state assoc :expr))
         :style {:white-space "pre-wrap" ;; this is vital or newlines and spaces cause errors!!!
                 :padding "1em 1em"
                 :width "40em"
                 :height "50vh"
                 :background-color "#000000"
                 :font-family "'JetBrains Mono', monospace"
                 :font-size "0.8em"
                 :color "#fafafa"}}])
