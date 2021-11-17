(ns brepl.nrepl
  (:require
   [brepl.bencode :as bencode]
   [brepl.backend :as backend]
   [brepl.views :as views :refer [DisplayHTML]]
   [reagent.core :as r]
   [brepl.sockets :as sockets]))

;;; --------------------------------------------------------------------------------

(defrecord NreplValue [value ns id]
  DisplayHTML
  (as-html [_]
    (views/history-item-html
     "R" [:span {:style {:padding-left "1em" :padding-right "1em"}} value])))

(defrecord NreplOut [out id]
  DisplayHTML
  (as-html [_]
    (views/history-item-html
     "O" [:span {:style {:padding-left "1em" :padding-right "1em"}} out])))

(defrecord NreplErr [err id]
  DisplayHTML
  (as-html [_]
    (views/history-item-html
     "E" [:span {:style {:padding-left "1em" :padding-right "1em"}} err])))

(defrecord NreplEx [ex root-ex status id]
  DisplayHTML
  (as-html [_]
    (views/history-item-html "X" [:<> [:span ex] [:div [:span root-ex]]])))

;;; --------------------------------------------------------------------------------

(defrecord NreplEvalRequest [op code id]
  DisplayHTML
  (as-html [_]
    (views/history-item-html
     ">" [:span {:style {:padding-left "0em" :padding-right "1em"}} code])))

;;; --------------------------------------------------------------------------------

(defrecord NreplCompletionsRequest [op prefix id])

;;; --------------------------------------------------------------------------------

(defrecord NreplUnhandled [status]
  DisplayHTML
  (as-html [_]
    (when-not (= status ["done"])
      [:div {:style {:background-color "orange"}} "UNHANDLED DATA"])))

;;; --------------------------------------------------------------------------------

(defn- nrepl-response->record
  [data]
  (println data)
  (cond (= "eval" (:op data)) (map->NreplEvalRequest data)
        (:value data)         (map->NreplValue data)
        (:out data)           (map->NreplOut data)
        (:err data)           (map->NreplErr data)
        (:ex data)            (map->NreplEx data)
        :else                 (map->NreplUnhandled data)))

;;; --------------------------------------------------------------------------------

(defn- create-nrepl
  [proxy-addr repl-addr on-data status]
  (let [session (atom nil)
        socket (sockets/socket
                proxy-addr
                (assoc repl-addr :type "nrepl")
                {:onopen (fn [s _]
                           (.send s (bencode/encode {:op "clone"})))
                 :onerror (fn [s _] (reset! status (sockets/status s)))
                 :onclose (fn [s _] (reset! status (sockets/status s)))
                 :onmessage (fn [s event]
                              (reset! status (sockets/status s))
                              (let [decoded (bencode/decode (.-data event))]
                                (if (:new-session decoded)
                                  (do (reset! session (:new-session decoded))
                                      (reset! status (sockets/status s)))
                                  (on-data decoded))))})]
    {:send  (fn send [data] (.send socket (bencode/encode data)))
     :close (fn close [] (.close socket))}))

;;; --------------------------------------------------------------------------------

(defrecord BackgroundNrepl [proxy-addr target-addr conn callbacks status]
  backend/Backend
  (backend/send
    [_ code callback] (let [payload {:op "eval" :code code :id (name (gensym "brepl"))}]
                        (when callback
                          (swap! callbacks assoc (:id payload) callback))
                        ((:send @conn) payload)))
  (backend/close [_] ((:close @conn)))
  (backend/connect [_]
    (let [on-data (fn [{:keys [id value]}]
                    (when-let [callback (get @callbacks id)]
                      (when value
                        (swap! callbacks dissoc id)
                        (callback value))))
          nrepl-conn (create-nrepl proxy-addr
                                   target-addr
                                   on-data
                                   status)]
      (reset! conn nrepl-conn)))
  (backend/connection-status [_] status))

(defrecord Nrepl [proxy-addr target-addr conn history-atom evaluated-atom status-atom]
  backend/WithHistory
  (history [_] history-atom)
  backend/Completions
  (backend/active-completions [_] (atom nil))
  (backend/complete [_ _prefix] nil)
  backend/Backend
  (backend/send
    [_ code _ignored-callback]
    (let [id (gensym "brepl")
          payload (map->NreplEvalRequest {:op "eval" :code code :id id})]
      (swap! history-atom conj payload)
      ((:send @conn) payload)))
  (backend/close [_] ((:close @conn)))
  (backend/connect [_]
    (let [nrepl-conn (create-nrepl proxy-addr
                                   target-addr
                                   #(->> % nrepl-response->record (swap! history-atom conj))
                                   status-atom)]
      (reset! conn nrepl-conn)))
  (backend/connection-status [_] status-atom))

(defn nrepl [{:keys [proxy repl]}]
  {:background
   (map->BackgroundNrepl
    {:status (r/atom nil)
     :callbacks (atom {})
     :conn (atom {:send (constantly nil) :close (constantly nil)})
     :proxy-addr proxy
     :target-addr repl})
   :repl
   (map->Nrepl
    {:history-atom (r/atom nil)
     :status-atom (r/atom nil)
     :evaluated-atom (atom {})
     :conn (atom {:send (constantly nil) :close (constantly nil)})
     :proxy-addr proxy
     :target-addr repl})})
