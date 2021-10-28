#!/usr/bin/env bash

set -eo pipefail

PORT=$1

clj -J-Xverify:none \
     -J-XX:+TieredCompilation \
     -J-XX:TieredStopAtLevel=1 \
     -J-Dclojure.server.repl="{:port ${PORT} :accept clojure.core.server/io-prepl}" \
