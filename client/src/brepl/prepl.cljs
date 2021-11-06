(ns brepl.prepl
  (:require
   [brepl.backend :as backend]
   [brepl.views :refer [DisplayHTML]]
   [cljs.tools.reader.edn :as edn]

   [reagent.core :as r]))

(defrecord PreplRet [val form ns ms exception]
  DisplayHTML
  (as-html [_]
    (if exception
      [:div {:style {:display "flex"}}
       [:div {:style {:padding-right "1em" :color "#ff4d4d"}} [:b "E"]]
       [:div {:style {:background-color "#E6BAAC"}}
        [:span (:cause (edn/read-string val))]
        [:div
         [:span "trace:"  val]
         [:span "input: " form]]]]

      [:div {:style {:display "flex"}}
       [:div {:style {:padding-right "1em" :color "#68e266"}} [:b "R"]]
       [:div {:style {:background-color "#ade5ac"}}
        [:div
         [:span {:style {:padding-left "1em"
                         :padding-right "1em"}} val]
         [:span " <-" ms "ms—" [:span {:style {:padding-left "1em"
                                               :padding-right "1em"}} form]]]]])))

(defrecord PreplOut [val]
  DisplayHTML
  (as-html [_]
    [:div {:style {:display "flex"}}
     [:div {:style {:padding-right "1em" :color "#59a6ff"}} [:b "O"]]
     [:div {:style {:background-color "#ADD8E6"}}
      [:div
       [:span {:style {:padding-left "1em"
                       :padding-right "1em"}} val]]]]))

(defrecord PreplErr [val]
  DisplayHTML
  (as-html [_]
    [:div {:style {:background-color "red"}}
     [:span [:b "E"] val]
     [:span "error: " val]]))

(defrecord PreplUnhandled []
  DisplayHTML
  (as-html [_]
    [:div "UNHANDLED DATA"]))

(defmulti prepl-response->record :tag)
(defmethod prepl-response->record :ret [data] (map->PreplRet data))
(defmethod prepl-response->record :out [data] (map->PreplOut data))
(defmethod prepl-response->record :err [data] (map->PreplErr data))
(defmethod prepl-response->record :default [data] (map->PreplUnhandled data))

(defrecord PreplRepl [connection history-atom]
  backend/WithHistory
  (backend/history [_] history-atom)
  backend/Connection
  (backend/send [_ data] (backend/send connection data))
  (backend/close [_] (backend/close connection))
  (backend/connect [_ {:keys [message] :as callbacks}]
    (->> {:message (fn [data]
                     (->> data
                          prepl-response->record
                          (swap! history-atom conj))
                     (when message (message data)))}
         (merge callbacks)
         (backend/connect connection)))
  (connection-status [_] (backend/connection-status connection)))

;;; --------------------------------------------------------------------------------

(defn prepl-connection [config]
  (backend/map->SocketConnection
   {:message-encode-fn identity
    :message-decode-fn edn/read-string
    :config (update config :repl assoc :type :prepl)
    :socket-atom (atom nil)
    :connection-state-atom (r/atom {})}))

(defn prepl [{:keys [proxy repl] :as config}]
  {:repl (map->PreplRepl
          {:history-atom (r/atom nil)
           :connection (prepl-connection config)})})
