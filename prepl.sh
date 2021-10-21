#!/usr/bin/env bash

set -eo pipefail

CLASSPATH=jars/clojure-tools-1.10.3.986.jar:.
PORT=$1

java -Xverify:none \
     -XX:+TieredCompilation \
     -XX:TieredStopAtLevel=1 \
     -Dclojure.server.repl="{:port ${PORT} :accept clojure.core.server/io-prepl}" \
     -cp "${CLASSPATH}" clojure.main
