#!/usr/bin/env bash

# clj -M -m cljs.main --optimizations advanced -c brepl.core

clojure -M -m figwheel.main -O advanced -bo dev;
# copy into public dir
cp target/public/cljs-out/dev-main.js ../public/js