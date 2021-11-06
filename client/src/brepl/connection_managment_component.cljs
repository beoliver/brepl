(ns brepl.connection-managment-component
  (:require [brepl.backend :as backend]
            [brepl.prepl :as prepl]
            [brepl.nrepl :as nrepl]
            [reagent.core :as r]))

;;; -----------------------------------------------------------------------

(defn status-color [{:keys [open? error?]}]
  (cond open?  "green"
        error? "red"
        :else  "grey"))

;;; SOCKET CONNECTOR -----------------------------------------------------


(defn manage-active-connection [backend-atom]
  (let [{:keys [repl]} @backend-atom
        status @(backend/connection-status repl)]
    [:div {:style {:background-color (status-color status)}}
     "CONNECTION"
     [:input {:type "button"
              :value "close"
              :on-click #(do (backend/close repl)
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


(defn connect-backend [backend-atom]
  (let [proxy-hostname (r/atom "127.0.0.1")
        proxy-port     (r/atom "8080")
        repl-hostname  (r/atom "127.0.0.1")
        repl-port      (r/atom "8888")
        backend-name   (r/atom "prepl")]
    (fn mounted-connect-backend []
      (js/console.log "connect-backend (re)rendered")
      [:div
       "hostname"
       [hostname-input proxy-hostname]
       "port"
       [port-input proxy-port]
       "---->"
       [:select {:default-value @backend-name :on-click #(->> % .-target .-value (reset! backend-name))}
        (->> ["prepl" "nREPL"]
             (map (fn [value] ^{:key value}[:option {:value value} value])))]
       "hostname"
       [hostname-input repl-hostname]
       "port"
       [port-input repl-port]
       [:input {:type "button"
                :value "connect"
                :on-click (fn []
                            (let [connection-config {:proxy {:hostname @proxy-hostname
                                                             :port @proxy-port}
                                                     :repl {:hostname @repl-hostname
                                                            :port @repl-port}}
                                  backend (case @backend-name
                                            "nREPL" (nrepl/nREPL connection-config)
                                            "prepl" (prepl/prepl connection-config))]
                              (reset! backend-atom backend)
                              (backend/connect (:repl backend) {})))}]])))

(defn socket-connector-component [backend-atom]
  (fn mounted-socket-connector-component []
    (if @backend-atom
      [manage-active-connection backend-atom]
      [connect-backend backend-atom])))
