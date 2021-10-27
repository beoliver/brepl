(ns ^:figwheel-hooks brepl.v2.core
  (:require [brepl.v2.config :as config]
            [brepl.v2.repl :as repl]
            [brepl.v2.browser :as browser]
            [reagent.core :as r]
            [reagent.dom :as dom]))

;;; SOCKET CONNECTOR -----------------------------------------------------

(defn color-circle [color]
  [:div {:style {:background-color color
                 :border-radius "50%"
                 :height "0.8em"
                 :width "0.8em"
                 :margin-right "0.2em"
                 }}])

(defn port-input-component [port-atom]
  [:input {:font-family "'JetBrains Mono', monospace"
           :font-size "1em"
           :type "text"
           :placeholder "PORT"
           :maxLength "5" ; 65535
           :size "5" ; 65535
           :value @port-atom
           :on-change #(reset! port-atom (-> % .-target .-value))}])

(defn socket-connector-component []
  (let [ws-port        (r/atom (str (:ws-port @config/config)))
        prepl-port     (r/atom (str (:prepl-port @config/config)))]
    (fn []
      [:div {:style {:font-family "'JetBrains Mono', monospace"
                     :font-size "0.8em"
                     :display "flex"
                     :align-items "center"
                     :height "2em"
                     :background-color (:yellow-light @config/config)}}
       ;; one dot for the "repl"
       [color-circle (cond @repl/connected? (:green @config/config)
                           @repl/error?     (:red @config/config)
                           :else            (:white-2 @config/config))]
       [:span "REPL"]
       "|"
       ;; one dot for the "helper"
       [color-circle (cond @browser/connected? (:green @config/config)
                           @browser/error?     (:red @config/config)
                           :else               (:white-2 @config/config))]
       [:span "NS-BROWSER"]
       "|"
       (if (and @repl/connected? @browser/connected?)
         ;; connected header
         [:div "ws://localhost:" @ws-port " -> localhost:" @prepl-port
          #_[:input {:type "button"
                     :value "CLOSE!"
                     :on-click (fn [_] (repl/close!))}]]
         ;; connect header
         [:div
          [:span "ws://localhost:"
           [port-input-component ws-port]]
          [:span " -> localhost:"
           [port-input-component prepl-port]]
          [:input {:type "button"
                   :value "Connect"
                   :on-click (fn [_]
                               (let [conn-info {:ws    {:hostname "localhost" :port @ws-port}
                                                :prepl {:hostname "localhost" :port @prepl-port}}]
                                 (swap! config/config assoc :ws-port ws-port)
                                 (swap! config/config assoc :prepl-port prepl-port)
                                 (repl/connect! conn-info)
                                 (browser/connect! conn-info)))}]])])))

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
