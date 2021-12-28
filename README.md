# _b_**REPL**

### bREPL lets your browser connect to **any** clojure application that is running either a [prepl](https://clojuredocs.org/clojure.core.server/io-prepl) or [nREPL](https://nrepl.org/nrepl/index.html) server. No extra Deps required!

---

## How?

Web browsers don't allow raw sockets. However, we can open up a websocket and connect to a second "proxy" [server](./server).

---

## Why?

Browsers are fantastic for experimenting with UX design. Which aspects of the clojure development process could benefit from the ability to explore visually?

---

## nREPL

```
$ lein repl
nREPL server started on port 64801 on host 127.0.0.1 - nrepl://127.0.0.1:64801
...
```



## PREPL

By including the following jvm options it is possible to connect to your running repl via a tcp connection.
```
-Dclojure.server.repl={:port 8888 :accept clojure.core.server/io-prepl}
```

For example, to just craete an "empty" repl, we could run the following clojure cli command

```
$ clj -J-Dclojure.server.repl="{:port 8888 :accept clojure.core.server/io-prepl}"
Clojure 1.10.3
user=> (def greeting "Hello")
#'user/greeting
```
In a separate tab, we can use the `nc` command to interact with the repl
```
$ nc localhost 8888
greeting
{:tag :ret, :val "\"Hello\"", :ns "user", :ms 0, :form "greeting"}
```

Web browsers don't allow raw sockets. However, we can open up a websocket and connect to a second server that can act as a "proxy".

A small [proxy server](./server) (written in GO) serves this role as well as serving the static html/js/css for the frontent.

```
[client]<----websocket--->[PROXY]<---TCP--->[CLOJURE/JVM]
                                            [   nREPL   ]
                                                  |
[EMACS/VSCODE]<-----------------------------------|
```
The nice thing about this is that you can open a browser as well as having
a project open in say, emacs.

## server

Start the server on your favourite port.

```bash
$ cd server
$ go run main.go --port 8080 --root ./public
```

You can now go to [http://localhost:8080](http://localhost:8080)!
If you are still running the `prepl` on port `8888` from the earlier example, you will already be able to connect and have a look around.


## client

The client is written in clojurescript. The client should be built and copied into the `server/public` directory.
If you are hacking on the client, you want to make sure that you start server with `-ws-any-origin` flag as during development
you will be on a different port eg `9500`.

## Lein
For a quick test you can add the following `jvm-opts` to your `:user` profile located at `~/.lein/profiles.clj`. You can can use whatever value for `:port` you want.

```clojure
{:user {:jvm-opts ["-Dclojure.server.repl={:port 8888 :accept clojure.core.server/io-prepl}"]}}
```

_Note that you will have issues if you try to open more than one project as there will be a bound socket (8888) already in use._

This will start a `prepl` server when you run `lein repl` from the command line - or use `cider-jack-in` within emacs.

If you go back to [http://localhost:8080](http://localhost:8080) you will now be able to connect using ports `8080` and `8888`

To test you can use the `nc` command




