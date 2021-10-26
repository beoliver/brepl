(ns brepl.state
  (:require [reagent.core :as r]))

;;; all REAGENT state lives here

(def state {:config (r/atom {:prepl-port 8888
                             :ws-port 8080
                             :ns-re-filter ".*"
                             :ns-re-remove nil
                             :ns-tree-open-depth 1
                             :ns-tree-path true})})

(defn set-config! [config]
  (update state :config swap! merge config))
