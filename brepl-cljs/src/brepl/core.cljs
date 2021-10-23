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
  (read [repl expr])
  (close [repl]))

(extend-type js/WebSocket REPL
             (read [ws expr] (.send ws expr))
             (close [ws] (.close ws)))


#_(defrecord Repl [conn]
    REPL
    (close [_] (.close conn))
    (read [_ expr] (.send conn expr)))


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
     sock)))


(defn port-input [partial-port]
  [:input {:type "text"
           :placeholder "PORT"
           :maxLength "5" ; 65535
           :size "4"
           :value @partial-port
           :on-change #(reset! partial-port (-> % .-target .-value))}])


(defn connecty-thing []
  (let [port (r/atom "")]
    (fn []
      [:div {:style {
                     :height "3em"
                     :font-family "monospace"
                     :background-color "white"
                     :display "flex"
                     :align-items "center"}}
       [:span {:style {:height "1em"
                       :margin-left "1em"
                       :margin-right "1em"
                       :width "1em"
                       :background-color (if (@state :repl-connected?) "#32CD32" "grey")
                       :border-radius "50%"
                       :display "inline-block"} }]
       (if-not (@state :repl-connected?)
         [:<>
          [:span "Connect to prepl on localhost:"]
          [port-input port]
          [:input {:style {:margin-left "1em"}
                   :type "button"
                   :value "Connect"
                   :on-click (fn [_] (->> (connect-to-repl! "8080" @port)
                                          (reset! repl)))}]]
         [:<>
          [:span "Connected to prepl on localhost:" @port]
          [:input {:style {:margin-left "1em"}
                   :type "button"
                   :value "Disconnect"
                   :on-click (fn [_] (close @repl))}]])])))

(defn expr-input [partial-expr]
  [:textarea {:style {:width "50%"}
              :value @partial-expr
              :on-change #(reset! partial-expr (-> % .-target .-value))}])


(defn sendy-thing []
  (let [expr (r/atom "")]
    (fn []
      [:div
       [expr-input expr]
       [:input {:type "button"
                :value "Eval"
                :on-click (fn [_] (read @repl @expr))}]])))


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
        [:summary {:style {:font-family "sans-serif"}} [:b "Exception"]]
        [:pre {:style {:overflow-x "auto"
                       :padding "0 0"
                       :margin "0 0"}} result]]
       [:pre {:style {:overflow-x "auto"
                      :padding "0 0"
                      :margin "0 0"}}
        (maybe-format-val (:val msg))])]))


(defn output-thingy []
  [:div {:style {:font-size "1em"}}
   (doall (map-indexed
           (fn [i msg]
             ^{:key i} [:div
                        {:style {:padding "1em 1em" :margin "0 0"
                                 :background-color (:background (msg->colour msg))
                                 :color (:foreground (msg->colour msg))
                                 :border-bottom "1px solid black"}}
                        [response-div msg]
                        ])
           (:repl-messages @state)))])



(defn brepl []
  [:div
   [connecty-thing]
   [:div {:style {:width "50%"}}
    [sendy-thing]
    [output-thingy]]])

;; ;; this is what you call for the first mount
(defn mount []
  (dom/render [brepl] (js/document.getElementById "app")))

;; and this is what figwheel calls after each save
(defn ^:after-load ^:export main []
  (mount))

;; this only gets called once
(defonce start-up (do (mount) true))



;; (comment
;;   (connect-to-repl! "8080" "8888")
;;   )
