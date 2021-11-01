(ns ^:figwheel-hooks brepl.core
  (:require [brepl.config :as config]
            [brepl.sockets :as sockets]
            [brepl.repl :as repl]
            [brepl.prepl-browser :as browser]
            [brepl.components :as components]
            [reagent.core :as r]
            [reagent.dom :as dom]))

;;; SOCKET CONNECTOR -----------------------------------------------------

(defn connect! [info]
  (let [repl-type (get-in info [:repl :type])]
    ;; `repl-type` is one of "prepl" "nrepl" etc
    ;; creates:
    ;;  One socket for the interactive repl
    ;;  One socket for the browser/navigation interface
    (repl/connect! info)
    (sockets/new-named-socket! (keyword :browser repl-type) info)))

;;; -----------------------------------------------------------------------

(defn repl-status-color []
  (cond @repl/connected? (:green @config/config)
        @repl/error?     (:red @config/config)
        :else              (:white-2 @config/config)))

(defn browser-status-color []
  (cond @browser/connected? (:green @config/config)
        @browser/error?     (:red @config/config)
        :else                 (:white-2 @config/config)))

(defn repl-and-browser-connected? []
  (and @repl/connected? @browser/connected?))


;;; SOCKET CONNECTOR -----------------------------------------------------

;;; sub components

(defn connected-info [websocket-port repl-type repl-port]
  [:div "ws://localhost:" websocket-port " -> " repl-type "://localhost:" repl-port])

;;;

(defn socket-connector-component []
  (let [ws-port   (r/atom (str (:ws-port @config/config)))
        repl-port (r/atom (str (:repl-port @config/config)))
        repl-type (r/atom (:repl-type @config/config))]
    (fn []
      [:div {:style {:font-family "'JetBrains Mono', monospace"
                     :font-size "0.8em"
                     :display "flex"
                     :align-items "center"
                     :height "2em"
                     :background-color (:yellow-light @config/config)}}
       [:span "REPL"] [components/circle (repl-status-color)] "|"
       [:span "NS-BROWSER"] [components/circle (browser-status-color)] "|"

       (if (repl-and-browser-connected?)
         [connected-info @ws-port @repl-type @repl-port]

         [:div
          [:span "ws://localhost:"
           [components/port-input {:font-family "'JetBrains Mono', monospace"
                                   :font-size "1em"} ws-port]]
          [:span " -> "
           [:select {:value @repl-type :on-click #(->> % .-target .-value (reset! repl-type))}
            (->> (:supported-repls @config/config)
                 (map (fn [value] ^{:key value}[:option {:value value} value])))]
           "://localhost:"
           [components/port-input {:font-family "'JetBrains Mono', monospace" :font-size "1em"} repl-port]]
          [:input {:type "button"
                   :value "Connect"
                   :on-click (fn [_]
                               (swap! config/config assoc :ws-port ws-port)
                               (swap! config/config assoc :repl-port repl-port)
                               (swap! config/config assoc :repl-type repl-type)
                               (connect! {:ws {:hostname "localhost" :port @ws-port}
                                          :repl {:type @repl-type :hostname "localhost" :port @repl-port}}))}]])])))

;;; MAIN -----------------------------------------------------------------

(defn main-component []
  [:div {:style {
                 ;;:background-color "orange"
                 :width "100%"
                 :height "100vh"}}
   ;; HEADER
   [:div
    [socket-connector-component]]
   ;; CONTENT
   [:div {:style {:height "calc(100vh - 2em)"
                  :display "flex"}}
    ;; LEFT
    [:div {:style {:resize "horizontal"
                   ;; :background-color "pink"
                   :min-width  "20em"
                   :width      "40em"
                   :overflow-y "scroll"
                   :height     "calc(100vh-2em)"
                   ;; :max-height     "calc(100vh-2em)"
                   }}
     [browser/ns-component]
     [browser/apropos-component]
     [browser/ns-publics-metadata-component]
     ]
    ;; RIGHT
    [:div {:style {;; :resize "horizontal"
                   :width "100%"
                   :height "calc(100vh-2em)"
                   :overflow-y "scroll"
                   :background-color (:black-2 @config/config)}}
     [:div
      [repl/input-component]]
     [:div {:style {;; :resize "horizontal"
                    :width "100%"
                    :height "calc(100vh-2em)"
                    :background-color (:black-2 @config/config)}}
      [repl/output-component]]
     ]
    ]
   ]
  )

;;; ENTRY ----------------------------------------------------------------

(defn mount []
  (config/load!)
  (dom/render [main-component] (js/document.getElementById "app")))

;; and this is what figwheel calls after each save
(defn ^:after-load ^:export main []
  (mount))


;; this only gets called once
(defonce start-up (do (mount) true))
