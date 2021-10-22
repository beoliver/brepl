#!/usr/bin/env bash

# clj -M -m cljs.main --optimizations advanced -c brepl.core

# clojure -m figwheel.main
clojure -M -m figwheel.main -O advanced -bo dev;