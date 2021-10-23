(ns ^:figwheel-hooks brepl.core
  (:require
   [cljs.pprint :as pprint :refer [cl-format]]
   [cljs.tools.reader.edn :as edn]
   [reagent.core :as r]
   [reagent.dom :as dom]))

;; http://localhost:9500/

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


(def colors (r/atom {:red "#fe6f5e"
                     :pink "#fde2e4"
                     :blue "#ace5ee"
                     :green1 "#ace1af"
                     :green2 "#e2ece9"}))

(defn msg->colour [msg]
  (cond (:exception msg)    {:foreground "black"
                             :background (:pink @colors)}      ; error
        (= :ret (:tag msg)) {:foreground "black"
                             :background (:green2 @colors)} ;;"green" ;;"#fafafa"   ; value
        (= :out (:tag msg)) {:foreground "white"
                             :background "black"}    ; side effect
        :else {:foreground "black"
               :background "white"}))



(defn maybe-format-val [s]
  (try (-> (edn/read-string s)
           pprint/pprint
           with-out-str)
       (catch js/Error _ s)))


(defn response-div [msg]
  (let [result (maybe-format-val (:val msg))]
    [:div
     (if (:exception msg)
       [:details
        [:summary "ERROR"]
        [:pre {:style {:padding "0 0" :margin "0 0"}} result]]
       [:pre {:style {:padding "0 0" :margin "0 0"}}
        (maybe-format-val (:val msg))])]))


(defn output-thingy []
  [:div {:style {:font-size "1em"}}
   (map-indexed
    (fn [i msg]
      ^{:key i} [:div
                 {:style {:padding "1em 1em" :margin "0 0"
                          :background-color (:background (msg->colour msg))
                          :color (:foreground (msg->colour msg))
                          :border-bottom "1px solid black"}}
                 [response-div msg]
                 ])
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
