(ns brepl.connection-managment-component
  (:require [brepl.backend :as backend]
            [brepl.background :as bg]
            [reagent.core :as r]))

;;; -----------------------------------------------------------------------

(defn status-color [status]
  (case status
    :connecting "yellow"
    :open       "green"
    :closing    "blue"
    :closed     "black"
    "grey"))

;;; SOCKET CONNECTOR -----------------------------------------------------

(defn manage-active-connection [backend-atom]
  (let [{:keys [repl background]} @backend-atom
        repl-status @(backend/connection-status repl)
        background-status (when background @(backend/connection-status background))]
    [:div
     [:div {:style {:background-color (status-color repl-status)}} "REPL CONNECTION"]
     [:div {:style {:background-color (status-color background-status)}} "BACKGROUND CONNECTION"]
     [:input {:type "button"
              :value "test"
              :on-click #(when background
                           (backend/send background "(+ 1 2 3)"
                                         (fn [data] (js/console.log "test1" data)))
                           (bg/eval-clj-file "/test.clj"
                                             background
                                             (fn [data] (js/console.log "test2" data))))}]
     [:input {:type "button"
              :value "close"
              :on-click #(do (backend/close repl)
                             (when background
                               (backend/close background))
                             (reset! backend-atom nil))}]]))

(defn port-input [port-atom]
  [:input {:style {}
           :type "text"
           :placeholder "PORT"
           :maxLength "5"             ; 65535
           :size "5"                  ; 65535
           :value @port-atom
           :on-change #(reset! port-atom (-> % .-target .-value))}])

(defn hostname-input [hostname-atom]
  [:input {:style {}
           :type "text"
           :size "9"
           :placeholder "HOSTNAME"
           :value @hostname-atom
           :on-change #(reset! hostname-atom (-> % .-target .-value))}])

(defn connect-backend [backend-atom backends]
  (let [proxy-port   (r/atom "8080")
        repl-port    (r/atom "8888")
        backend-name (r/atom (first (keys backends)))]
    (fn mounted-connect-backend []
      [:div
       "proxy port"
       [port-input proxy-port]
       [:select {:default-value @backend-name :on-click #(->> % .-target .-value (reset! backend-name))}
        (->> (keys backends) (map (fn [value] ^{:key value}[:option {:value value} value])))]
       "repl port"
       [port-input repl-port]
       [:input {:type "button"
                :value "connect"
                :on-click (fn []
                            (let [connection-config {:proxy {:hostname "localhost" :port @proxy-port}
                                                     :repl  {:hostname "localhost" :port @repl-port}}
                                  backend ((get backends @backend-name) connection-config)]
                              (reset! backend-atom backend)
                              (backend/connect (:repl backend))
                              (when (:background backend)
                                (backend/connect (:background backend)))))}]])))

(defn socket-connector-component
  ([backend-atom backends]
   (socket-connector-component {} backend-atom backends))
  ([style backend-atom backends]
   (fn mounted-socket-connector-component []
     [:div {:style style}
      (if @backend-atom
        [manage-active-connection backend-atom]
        [connect-backend backend-atom backends])])))
