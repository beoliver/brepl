(ns ^:figwheel-hooks brepl.prepl-repl
  (:require [brepl.sockets :as ws]
            [brepl.repl :as repl]
            [brepl.config :as config]
            [cljs.tools.reader.edn :as edn]
            [reagent.core :as r]
            [cljs.pprint :refer [cl-format]]))

(defonce sock-name :user-repl/prepl)

;;; STATE
(defonce current-namespace (r/atom nil))
;;; END OF STATE

(defn set-ns!
  "allows the users namespace to be controlled
  from outside of the repl.
  This means that a user can use the file browser
  to navigate and 'switch' namespaces"
  [ns-str]
  (->> (cl-format nil "(clojure.core/in-ns '~a)" ns-str)
       (ws/socket-write! sock-name)))

(defn close! [] (ws/socket-close! sock-name))

(defmethod ws/on-socket-open sock-name
  [_ _]
  (reset! repl/connected? true)
  (reset! repl/error? false))

(defmethod ws/on-socket-close sock-name
  [_ _] (reset! repl/connected? false))

(defn maybe-replace [old new] (or new old))

(defmethod ws/on-socket-message sock-name
  [_ event] (let [data (->> event
                            .-data
                            edn/read-string)]
              (swap! repl/history conj data)
              (swap! current-namespace maybe-replace (:ns data))))

(defmethod ws/on-socket-error sock-name
  [_ _]
  (reset! repl/connected? false)
  (reset! repl/error? true))


;;; HISTORY ITEMS

(defn ret-exception-component
  [{:keys [val form]}]
  [:div {:style {:display "flex"}}
   [:div {:style {:padding-right "1em" :color (:red @config/config)}} [:b "E"]]
   [:div {:style {:background-color (:red-light @config/config)}}
    [:span (:cause (edn/read-string val))]
    [:div
     [:span "trace:" val]
     [:span "input: " form]]]])

(defn ret-component
  [{:keys [val ms form]}]
  [:div {:style {:display "flex"}}
   [:div {:style {:padding-right "1em" :color (:green @config/config)}} [:b "R"]]
   [:div {:style {:background-color (:green-light @config/config)}}
    [:div
     [:span {:style {:padding-left "1em"
                     :padding-right "1em"}} val]
     [:span " <-" ms "ms—" [:span {:style {:padding-left "1em"
                                           :padding-right "1em"}} form]]]]])



(defmulti prepl-data-to-component :tag)

(defmethod prepl-data-to-component :ret
  [{:keys [exception] :as data}]
  (if exception
    [ret-exception-component data]
    [ret-component data]))

(defmethod prepl-data-to-component :out
  [{:keys [val]}]
  [:div {:style {:display "flex"}}
   [:div {:style {:padding-right "1em" :color (:blue @config/config)}} [:b "O"]]
   [:div {:style {:background-color (:blue-light @config/config)}}
    [:div
     [:span {:style {:padding-left "1em"
                     :padding-right "1em"}} val]]]])

(defmethod prepl-data-to-component :err
  [{:keys [val]}]
  [:div {:style {:background-color (:red @config/config)}}
   [:span [:b "E"] val]
   [:span "error: " val]])

(defmethod prepl-data-to-component :tap
  [{:keys [val]}]
  [:div {:style {:background-color (:yellow @config/config)}}
   [:span [:b "T"] val]
   [:span "tap: " val]])


(defmethod repl/history-item->component sock-name [data]
  (prepl-data-to-component data))

(defmethod repl/remote-eval! sock-name [expr]
  (ws/socket-write! sock-name expr))
