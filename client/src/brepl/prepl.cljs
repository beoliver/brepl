(ns brepl.prepl
  (:require
   [brepl.backend :as backend]
   [brepl.views :as views :refer [DisplayHTML]]
   [cljs.tools.reader.edn :as edn]
   [reagent.core :as r]
   [brepl.sockets :as sockets]))

;;; --------------------------------------------------------------------------------

(defrecord PreplRet [val form ns ms exception]
  DisplayHTML
  (as-html [_]
    (if exception
      (views/history-item-html
       "X" [:span {:style {:padding-left "1em" :padding-right "1em"}}
            (:cause (edn/read-string val))])
      (views/history-item-html
       "R" [:span {:style {:padding-left "1em" :padding-right "1em"}}
            val]))))

(defrecord PreplOut [val]
  DisplayHTML
  (as-html [_]
    (views/history-item-html
     "O" [:span {:style {:padding-left "1em" :padding-right "1em"}}
          val])))

(defrecord PreplErr [val]
  DisplayHTML
  (as-html [_]
    (views/history-item-html
     "E" [:span {:style {:padding-left "1em" :padding-right "1em"}} val])))

(defrecord PreplUnhandled []
  DisplayHTML
  (as-html [_]
    [:div {:style {:background-color "orange"}} "UNHANDLED DATA"]))

(defn prepl-response->record [data]
  (case (:tag data)
    :ret (map->PreplRet data)
    :out (map->PreplOut data)
    :err (map->PreplErr data)
    (map->PreplUnhandled data)))

;;; --------------------------------------------------------------------------------

(defrecord PreplEvalRequest [code]
  DisplayHTML
  (as-html [_]
    (views/history-item-html
     ">" [:span {:style {:padding-left "0em" :padding-right "1em"}}
          code])))

;;; --------------------------------------------------------------------------------

(defn- create-prepl
  [proxy-addr prepl-addr on-data status-atom]
  (let [socket (sockets/socket
                proxy-addr
                (assoc prepl-addr :type "prepl")
                {:onopen (fn [s _]  (reset! status-atom (sockets/status s)))
                 :onerror (fn [s _] (reset! status-atom (sockets/status s)))
                 :onclose (fn [s _] (reset! status-atom (sockets/status s)))
                 :onmessage (fn [_ event]
                              (on-data
                               (edn/read-string (.-data event))))})]
    {:send  (fn send [data] (.send socket data))
     :close (fn close [] (.close socket))}))

;;; --------------------------------------------------------------------------------

(defrecord BackgroundPrepl [proxy-addr target-addr conn callbacks status]
  backend/Backend
  (backend/send
    [_ code callback]
    (when callback
      (swap! callbacks assoc code callback)
      ((:send @conn) code)))
  (backend/close [_] ((:close @conn)))
  (backend/connect [_]
    (let [on-data (fn [{:keys [tag form val]}]
                    (when (= tag :ret)
                      (when-let [callback (get @callbacks form)]
                        (when val
                          (swap! callbacks dissoc form)
                          (callback val)))))
          prepl-conn (create-prepl proxy-addr
                                   target-addr
                                   on-data
                                   status)]
      (reset! conn prepl-conn)))
  (backend/connection-status [_] status))

;;; --------------------------------------------------------------------------------

(defrecord Prepl [proxy-addr target-addr conn history-atom status-atom evaluated-atom]
  backend/WithHistory
  (history [_] history-atom)
  backend/Completions
  (backend/active-completions [_] (atom nil))
  (backend/complete [_ _prefix] nil)
  backend/Backend
  (backend/send
    [_ code _ignored-callback]
    (swap! history-atom conj (->PreplEvalRequest code))
    ((:send @conn) code))
  (backend/close [_] ((:close @conn)))
  (backend/connect [_]
    (let [nrepl-conn (create-prepl
                      proxy-addr
                      target-addr
                      (fn [data]
                        (->> data
                             prepl-response->record
                             (swap! history-atom conj)))
                      status-atom)]
      (reset! conn nrepl-conn)))
  (backend/connection-status [_] status-atom))

(defn prepl [{:keys [proxy repl]}]
  {:background
   (map->BackgroundPrepl
    {:status (r/atom nil)
     :callbacks (atom {})
     :conn (atom {:send (constantly nil) :close (constantly nil)})
     :proxy-addr proxy
     :target-addr repl})
   :repl
   (map->Prepl
    {:history-atom (r/atom nil)
     :status-atom (r/atom nil)
     :ports (atom nil)
     :conn (atom {:send (constantly nil) :close (constantly nil)})
     :proxy-addr proxy
     :target-addr repl})})
