(ns ^:figwheel-hooks brepl.config
  (:require [brepl.utils :as utils]
            [reagent.core :as r]))

;;; what things should be "export/imprt" abale?
;;; Allow passing of values using query params in the URL

(def ^:private default-config {:repl-port 8888
                               :repl-type "prepl"
                               :ws-port 8080
                               :ns-re-filter ".*"
                               :ns-re-remove "^brepl"
                               :ns-tree-open-depth 1
                               :ns-tree-path true
                               :red "#ff4d4d"
                               :red-light "#E6BAAC"
                               :green "#68e266"
                               :green-light "#ade5ac"
                               :blue "#59a6ff"
                               :blue-light "#ADD8E6"
                               :yellow "yellow"
                               :yellow-light "#f2f2bf"
                               :purple "RebaccaPurple"
                               :purple-light "purple"
                               :magenta "magenta"
                               :magenta-light "magenta"
                               :cyan "cyan"
                               :cyan-light "cyan"
                               :black "black"
                               :black-1 "#151319"
                               :black-2 "#1e1c23"
                               :white "white"
                               :white-1 "#fafafa"
                               :white-2 "white"
                               })

(defonce config (r/atom default-config))

(defn load! []
  (try
    (reset! config
            (merge default-config
                   (utils/query-param-map js/window.location.search)))
    (catch js/Error _ nil))
  config)

(defn as-query-params
  "return someting like \"?prepl-port=7777&ws-port=9999\""
  []
  "")
