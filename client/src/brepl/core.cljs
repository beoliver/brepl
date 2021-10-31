(ns ^:figwheel-hooks brepl.core
  (:require [brepl.config :as config]
            [brepl.repl :as repl]
            [brepl.nrepl :as nrepl]
            [brepl.browser :as browser]
            [brepl.components :as components]
            [reagent.core :as r]
            [reagent.dom :as dom]))

;;; SOCKET CONNECTOR -----------------------------------------------------
;;; HELPERS

(defn connect! [info]
  (let [ws-addr   (let [{:keys [hostname port]} (:ws info)] (str hostname ":" port))
        repl-addr (let [{:keys [hostname port]} (:repl info)] (str hostname ":" port))]
    (if (= "prepl" (get-in info [:repl :type]))
      (do (repl/connect! ws-addr repl-addr)
          (browser/connect! ws-addr repl-addr))
      (nrepl/connect! ws-addr repl-addr))))

(defn status-color [connected? error?]
  (cond connected? (:green @config/config)
        error?     (:red @config/config)
        :else      (:white-2 @config/config)))

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
       [:span "REPL"] [components/circle (status-color @repl/connected? @repl/error?)] "|"
       [:span "NS-BROWSER"] [components/circle (status-color @browser/connected? @browser/error?)] "|"

       (if (repl-and-browser-connected?)
         [connected-info @ws-port @repl-type @repl-port]

         [:div
          [:span "ws://localhost:"
           [components/port-input {:font-family "'JetBrains Mono', monospace"
                                   :font-size "1em"} ws-port]]
          [:span " -> "
           [:select {:value @repl-type :on-click (fn [event] (->> event .-target .-value (reset! repl-type)))}
            [:option {:value "nrepl"} "nrepl"]
            [:option {:value "nrepl+edn"} "nrepl+edn"]
            [:option {:value "prepl"} "prepl"]
            ]
           "://localhost:"
           [components/port-input {:font-family "'JetBrains Mono', monospace"
                                   :font-size "1em"} repl-port]]
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
      [repl/repl-input-component]]
     [:div {:style {;; :resize "horizontal"
                    :width "100%"
                    :height "calc(100vh-2em)"
                    :background-color (:black-2 @config/config)}}
      [repl/repl-output-component]]
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
