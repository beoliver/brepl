(ns brepl.nrepl
  (:require
   [brepl.bencode :as bencode]
   [brepl.backend :as backend]
   [brepl.views :refer [DisplayHTML]]
   [reagent.core :as r]))

(defn flex [style & rest]
  (into [:div {:style (merge style {:display "flex"})}] rest))

(defn indicator [color symbol]
  [:div {:style {:padding-right "1em" :color color}} [:b symbol]])

(defrecord NreplValue [value ns]
  DisplayHTML
  (as-html [_]
    (flex {}
          (indicator "#68e266" "R")
          [:div {:style {:width "100%" :background-color "#ade5ac"}}
           [:div
            [:span {:style {:padding-left "1em"
                            :padding-right "1em"}} value]]])))

(defrecord NreplOut [out]
  DisplayHTML
  (as-html [_]
    (flex {}
          (indicator "#59a6ff" "O")
          [:div {:style {:width "100%" :background-color "#ADD8E6"}}
           [:div
            [:span {:style {:padding-left "1em"
                            :padding-right "1em"}} out]]])))


(defrecord NreplErr [err]
  DisplayHTML
  (as-html [_]
    (flex {}
          (indicator "#ff4d4d" "E")
          [:div {:style {:width "100%" :background-color "#E6BAAC"}}
           [:div [:span {:style {:padding-left "1em" :padding-right "1em"}} err]]])))

(defrecord NreplEx [ex root-ex status]
  DisplayHTML
  (as-html [_]
    (flex {}
          (indicator "#ff4d4d" "Ex")
          [:div {:style {:width "100%" :background-color "#E6BAAC"}}
           [:span ex]
           [:div
            [:span root-ex]]])))

(defrecord NreplEvalRequest [op code id]
  DisplayHTML
  (as-html [_]
    (flex {}
          (indicator "white" ">")
          [:div {:style {:width "100%" :color "white"}}
           [:div [:span {:style {:padding-left "0em" :padding-right "1em"}} code]]])))

(defrecord NreplCompletionsRequest [op prefix id])


(defrecord NreplUnhandled [status]
  DisplayHTML
  (as-html [_]
    (when-not (= status ["done"])
      [:div {:style {:background-color "orange"}} "UNHANDLED DATA"])))

(defn- nrepl-response->record
  [data]
  (println data)
  (cond (= "eval" (:op data)) (map->NreplEvalRequest data)
        (:value data) (map->NreplValue data)
        (:out data)   (map->NreplOut data)
        (:err data)   (map->NreplErr data)
        ;; (:ex data)    (map->NreplEx data)
        :else         (map->NreplUnhandled data)))

(defn- nREPL-connection [config]
  (backend/map->SocketConnection
   {:message-encode-fn bencode/encode
    :message-decode-fn bencode/decode
    :config (update config :repl assoc :type :nrepl)
    :socket-atom (atom nil)
    :connection-state-atom (r/atom {})}))

(defrecord NreplWithSession [connection session-atom]
  backend/Connection
  (backend/send [_ data] (backend/send connection (merge {:session @session-atom} data)))
  (backend/close [_] (backend/close connection))
  (backend/connect [_ {:keys [message] :as callbacks}]
    (->> {:open   #(backend/send connection {:op "clone"})
          :message (fn [data]
                     (let [{:keys [new-session]} data]
                       (if new-session
                         (reset! session-atom new-session)
                         (when message (message data)))))}
         (merge callbacks)
         (backend/connect connection)))
  (connection-status [_] (backend/connection-status connection)))

(defn- nREPL-with-session [config]
  (map->NreplWithSession
   {:connection (nREPL-connection config)
    :session-atom (atom nil)}))

;;; --------------------------------------------------------------------------------
;;; https://nrepl.org/nrepl/ops.html

(defrecord NreplBackend [nREPL-with-session evaluated-atom]
  backend/Completions
  (backend/complete [_ prefix] (let [payload (map->NreplCompletionsRequest
                                              {:op "completions" :prefix prefix :id (gensym "brepl")})]
                                 (backend/send nREPL-with-session payload)))
  backend/Connection
  (backend/send [_ data] (let [id (gensym "brepl")
                               payload (map->NreplEvalRequest {:op "eval" :code data :id id})]
                           (swap! evaluated-atom assoc id data)
                           (backend/send nREPL-with-session payload)))
  (backend/close [_] (backend/close nREPL-with-session))
  (backend/connect [_ {:keys [message] :as callbacks}]
    (->> {:message (fn [data] (when message (message data)))}
         (merge callbacks)
         (backend/connect nREPL-with-session)))
  (backend/connection-status [_] (backend/connection-status nREPL-with-session)))

(defn nREPL-backend [config]
  {:repl (map->NreplBackend
          {:evaluated-atom (atom {})
           :nREPL-with-session (nREPL-with-session config)})})

;;; --------------------------------------------------------------------------------

(defrecord NreplRepl [nREPL-with-session history-atom evaluated-atom active-completions-atom]
  backend/WithHistory
  (history [_] history-atom)
  backend/Completions
  (backend/active-completions [_] active-completions-atom)
  (backend/complete [_ prefix] (let [payload (map->NreplCompletionsRequest
                                              {:op "completions" :prefix prefix :id (gensym "brepl")})]
                                 (backend/send nREPL-with-session payload)))
  backend/Connection
  (backend/send [_ data] (let [id (gensym "brepl")
                               payload (map->NreplEvalRequest {:op "eval" :code data :id id})]
                           (swap! evaluated-atom assoc id data)
                           (swap! history-atom conj payload)
                           (backend/send nREPL-with-session payload)))
  (backend/close [_] (backend/close nREPL-with-session))
  (backend/connect [_ {:keys [message] :as callbacks}]
    (->> {:message (fn [data]
                     (if (:completions data)
                       (reset! active-completions-atom (:completions data))
                       (->> data
                            nrepl-response->record
                            (swap! history-atom conj)))
                     (when message (message data)))}
         (merge callbacks)
         (backend/connect nREPL-with-session)))
  (backend/connection-status [_] (backend/connection-status nREPL-with-session)))


(defn nREPL [{:keys [proxy repl] :as config}]
  {:repl (map->NreplRepl
          {:history-atom (r/atom nil)
           :evaluated-atom (atom {})
           :active-completions-atom (r/atom nil)
           :nREPL-with-session (nREPL-with-session config)})})
