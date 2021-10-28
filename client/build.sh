#!/usr/bin/env bash

# clj -M -m cljs.main --optimizations advanced -c brepl.core

clojure -M -m figwheel.main -O advanced -bo dev;
# copy into server/public dir
cp target/public/cljs-out/dev-main.js ../server/public/cljs-out/dev-main.js
cp resources/public/index.html ../server/public/index.html
cp resources/public/test.clj ../server/public/test.clj