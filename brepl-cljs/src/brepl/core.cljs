(ns ^:figwheel-hooks brepl.core
  (:require
   [cljs.pprint :as pprint :refer [cl-format]]
   [cljs.tools.reader.edn :as edn]
   [reagent.core :as r]
   [reagent.dom :as dom]))



;;; should ONLY be used to dev debugging on localhost...
(enable-console-print!)

(defn- create-ws!
  [hostname ws-port repl-port]
  (js/WebSocket. (cl-format nil "ws://~a:~a/prepl/~a" hostname ws-port repl-port)))

(defonce repl (atom nil))

(defonce state (r/atom {:repl-connected? false
                        :ws-error      nil
                        :repl-messages nil}))

(defprotocol REPL
  (write [repl expr])
  (close [repl]))

(defrecord Repl [conn]
  REPL
  (close [_] (.close conn))
  (write [_ expr] (.send conn expr)))


(defn connect-to-repl!
  ([repl-port]
   (connect-to-repl! js/window.location.port repl-port))
  ([ws-port repl-port]
   (connect-to-repl! js/window.location.hostname ws-port repl-port))
  ([hostname ws-port repl-port]
   (let [sock (create-ws! hostname ws-port repl-port)]
     (.addEventListener sock "open"
                        (fn [_]
                          (swap! state assoc :repl-connected? true)))
     (.addEventListener sock "close"
                        (fn [_]
                          (swap! state assoc :repl-connected? false)))
     (.addEventListener sock "message"
                        (fn [event]
                          (let [data (.-data event)]
                            (println data)
                            (->> data
                                 edn/read-string
                                 (swap! state update :repl-messages conj))
                            )))
     (.addEventListener sock "error"
                        (fn [event]
                          (swap! state assoc :ws-error event)))
     (reset! repl (->Repl sock)))))



;; (comment
;;   (connect-to-repl! "8080" "8888")
;;   )

(defn noop [])

(defn port-input [partial-port]
  [:input {:type "text"
           :value @partial-port
           :on-change #(reset! partial-port (-> % .-target .-value))}])

(defn connecty-thing []
  (let [port (r/atom "")]
    (fn []
      [:div
       [:p "connected: " (str (:repl-connected? @state))]
       [:span "connect to prepl on port: " [port-input port]]
       [:input {:type "button"
                :value "Connect to PREPL"
                :on-click (fn [_] (connect-to-repl! "8080" @port))}]])))

(defn expr-input [partial-expr]
  [:input {:type "text"
           :value @partial-expr
           :on-change #(reset! partial-expr (-> % .-target .-value))}])


(defn sendy-thing []
  (let [expr (r/atom "")]
    (fn []
      [:div
       [:span "expr: " [expr-input expr]]
       [:input {:type "button"
                :value "Eval"
                :on-click (fn [_] (write @repl @expr))}]])))

(defn msg->colour [msg]
  (cond (:exception msg)    "pink"         ; error
        (= :ret (:tag msg)) "#fafafa"   ; value
        (= :out (:tag msg)) "yellow"    ; side effect
        :else "white"))


(defn maybe-format-val [s]
  (try (-> (edn/read-string s)
           pprint/pprint
           with-out-str)
       (catch js/Error _ s)))


(defn output-thingy []
  [:div (map-indexed
         (fn [i msg] ^{:key i} [:div
                                {:style {:background-color (msg->colour msg)}}
                                [:hr]
                                [:pre (maybe-format-val (:val msg))]])
         (:repl-messages @state))])


(defn brepl []
  [:div
   [connecty-thing]
   [sendy-thing]
   [output-thingy]])

;; ;; this is what you call for the first mount
(defn mount []
  (dom/render [brepl] (js/document.getElementById "app")))

;; and this is what figwheel calls after each save
(defn ^:after-load ^:export main []
  (mount))

;; this only gets called once
(defonce start-up (do (mount) true))
