(ns ^:figwheel-hooks brepl.repl
  (:require [brepl.sockets :as ws]
            [brepl.config :as config]
            [cljs.tools.reader.edn :as edn]
            [reagent.core :as r]
            [cljs.pprint :refer [cl-format]]))

(defonce sock-name :user-repl/prepl)

;;; STATE
(defonce connected? (r/atom false))
(defonce history (r/atom nil))
(defonce error? (r/atom false))
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
  (reset! connected? true)
  (reset! error? false))

(defmethod ws/on-socket-close sock-name
  [_ _] (reset! connected? false))

(defn maybe-replace [old new] (or new old))

(defmethod ws/on-socket-message sock-name
  [_ event] (let [data (->> event
                            .-data
                            edn/read-string)]
              (swap! history conj data)
              (swap! current-namespace maybe-replace (:ns data))))

(defmethod ws/on-socket-error sock-name
  [_ _]
  (reset! connected? false)
  (reset! error? true))


(defn repl-input-component
  "
  Lets the user eval the content using `Shift+Enter`
  "
  []
  (let [shift-active (r/atom false)
        expr         (r/atom "")]
    (fn []
      [:div {:content-editable @connected?
             :on-key-down (fn [event]
                            (let [k (.-key event)]
                              (cond (= k "Tab") (.preventDefault event) ;; should also insert white space...
                                    (= k "Shift")                     (reset! shift-active true)
                                    (and (= k "Enter") @shift-active) (do (ws/socket-write! sock-name @expr)
                                                                          ;; avoid spamming the server?
                                                                          (reset! shift-active false))
                                    :else                             nil)))
             :on-key-up (fn [event]
                          (when (= "Shift" (.-key event))
                            (reset! shift-active false)))
             :on-input #(->> % .-target .-innerText (reset! expr))
             :style {:white-space "pre-wrap" ;; this is vital or newlines and spaces cause errors!!!
                     ;;:padding "1em 1em"
                     :width "100%"
                     :max-height "30vh"
                     :min-height "30vh"
                     :overflow-y "auto"
                     :background-color (if @connected? "#000000" "grey")
                     :font-family "'JetBrains Mono', monospace"
                     :font-size "0.8em"
                     :color (if @connected? "#fafafa" "LightGray")}}])))

;;; HISTORY ITEMS

(defn ret-exception-component
  [{:keys [val ns ms form]}]
  [:div {:style {:display "flex"}}
   [:div {:style {:padding-right "1em" :color (:red @config/config)}} [:b "E"]]
   [:div {:style {:background-color (:red-light @config/config)}}
    [:span (:cause (edn/read-string val))]
    [:div
     [:span "trace:" val]
     #_[:span "namespace: " ns]
     #_[:span "took: " ms "ms"]
     [:span "input: " form]
     ]
    ]]
  )

(defn ret-component
  [{:keys [val ns ms form]}]
  [:div {:style {:display "flex"}}
   [:div {:style {:padding-right "1em" :color (:green @config/config)}} [:b "R"]]
   [:div {:style {:background-color (:green-light @config/config)}}
    [:div
     [:span {:style {:padding-left "1em"
                     :padding-right "1em"}} val]
     #_[:span "namespace: " ns]
     #_[:span "took: " ms "ms"]
     [:span " <-"ms "ms—" [:span {:style {:padding-left "1em"
                                          :padding-right "1em"}} form]]
     ]
    ]])




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
   [:span "error: " val]
   ]
  )

(defmethod prepl-data-to-component :tap
  [{:keys [val]}]
  [:div {:style {:background-color (:yellow @config/config)}}
   [:span [:b "T"] val]
   [:span "tap: " val]
   ]
  )


(defn repl-output-component
  []
  (into [:div {:style {:height "100%"
                       :padding "1em 1em"
                       :font-family "'JetBrains Mono', monospace"
                       :font-size "0.8em"}}]
        (map-indexed (fn [i x] ^{:key i} [:div {:style {:height "1.6em"
                                                        :overflow-x "hidden"}}
                                          [prepl-data-to-component x]]) @history)))
