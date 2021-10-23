# brepl

## A browser based repl for clojure.

A small GO server accepts websocket requests and then "proxies" them to a running
instance of a clojure prepl socket server.

## Lein

If you add the following `jvm-opts` to your `:user` profile located at `~/.lein/profiles.clj`.

```clojure
{:user {:jvm-opts ["-Dclojure.server.repl={:port 8888 :accept clojure.core.server/io-prepl}"]}}
```

This will start a `prepl` server when you run `lein repl`.
The `:port` number is arbitrary.
To test you can use the `nc` command

```clojure
$ nc 127.0.0.1 8888
*ns*
{:tag :ret, :val "#object[clojure.lang.Namespace 0x64d79c43 \"user\"]", :ns "user", :ms 0, :form "*ns*"}
```

## prepl script

Make sure that you have **one** of the following combinations of jars in the `/jars` directory

1. `clojure-1.10.3.jar`, `core.specs.alpha-0.2.62.jar` and `spec.alpha-0.2.194.jar`
2. `clojure-tools-1.10.3.986.jar`


Start the prepl clojure server on a port of your choosing...

```bash
$ ./prepl.sh 8888
```

Start the server on your favourite port.

```bash
$ cd brepl-server
$ go run main.go -port 7777 -serve-from ../public
```

You can now go to [http://localhost:7777](http://localhost:7777)
