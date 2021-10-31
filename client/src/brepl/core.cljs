(ns ^:figwheel-hooks brepl.core
  (:require [brepl.config :as config]
            [brepl.repl :as repl]
            [brepl.nrepl :as nrepl]
            [brepl.browser :as browser]
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
  (let [ws-port   (r/atom (str (:ws-port @config/config)))
        repl-port (r/atom (str (:repl-port @config/config)))
        repl-type (r/atom (str (:repl-type @config/config)))]
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
         [:div "ws://localhost:" @ws-port " -> " @repl-type "://localhost:" @repl-port]
         ;; connect header
         [:div
          [:span "ws://localhost:"
           [port-input-component ws-port]]
          [:span " -> "
           [:select {:value @repl-type :on-click (fn [event] (->> event .-target .-value (reset! repl-type)))}
            [:option {:value "nrepl"} "nrepl"]
            [:option {:value "prepl"} "prepl"]]
           "://localhost:"
           [port-input-component repl-port]]
          [:input {:type "button"
                   :value "Connect"
                   :on-click (fn [_]
                               (let [ws-addr   (str "localhost:" @ws-port)
                                     repl-addr (str "localhost:" @repl-port)]
                                 (swap! config/config assoc :ws-port ws-port)
                                 (swap! config/config assoc :repl-port repl-port)
                                 (swap! config/config assoc :repl-type repl-type)
                                 (if (= @repl-type "prepl")
                                   (do (repl/connect! ws-addr repl-addr)
                                       (browser/connect! ws-addr repl-addr))
                                   (nrepl/connect! ws-addr repl-addr))))}]])])))

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
