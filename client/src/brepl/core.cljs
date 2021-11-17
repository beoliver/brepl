(ns ^:figwheel-hooks
    brepl.core
  (:require [brepl.config :as config]
            [brepl.background :as background]
            [brepl.connection-managment-component :refer [socket-connector-component]]
            [brepl.repl-component :as repl-component]
            [reagent.core :as r]
            [reagent.dom :as dom]
            [brepl.nrepl :as nrepl]
            [brepl.prepl :as prepl]))

;;; MAIN -----------------------------------------------------------------

(def backends
  {"nrepl" (fn [config] (nrepl/nrepl config))
   "prepl" (fn [config] (prepl/prepl config))})



(defn main-component []
  (let [backend (r/atom nil)]
    (js/console.log "main component mounted")
    (fn mounted-main-component []
      (js/console.log "main component (re)rendered")
      [:div {:style {:width "100%" :height "100vh"}}
       ;; HEADER
       [socket-connector-component {:background-color "yellow"
                                    :font-family "'JetBrains Mono', monospace"
                                    :font-size "0.8em"}
        backend
        backends]
       ;; CONTENT
       [:div {:style {:height "calc(100vh - 2em)" :display "flex"}}
        ;; LEFT
        [:div {:style {:resize "horizontal"
                       :background-color "#fafafa"
                       :min-width  "20em"
                       :width      "40em"
                       :overflow-y "scroll"
                       :height     "calc(100vh-2em)"}}
         (when @backend
           (background/ns-tree-component (:background @backend)))]
        [:div {:style {:background-color "black"
                       :font-family "'JetBrains Mono', monospace"
                       :width "100%"
                       :font-size "0.8em"}}
         (when @backend
           [repl-component/repl-component
            {}
            {:padding-left "2em"
             :max-height "30vh"
             :min-height "30vh"
             :overflow-y "scroll"
             :background-color "#1e1c23"
             :color "#fafafa"}
            {:overflow-y "scroll"
             :background-color "#151319"
             :max-height "calc(70vh-2em)"
             :min-height "calc(70vh-2em)"
             :padding "1em 1em"}
            backend])]]])))

;;; ENTRY ----------------------------------------------------------------

(defn mount []
  (config/load!)
  ;;; backends allow us to support different types of socket servers
  (dom/render [main-component] (js/document.getElementById "app")))

;; and this is what figwheel calls after each save
(defn ^:after-load ^:export main []
  (mount))


;; this only gets called once
(defonce start-up (do (mount) true))
