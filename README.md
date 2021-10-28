# _b_**REPL**
## Explore and interact with clojure in the browser
---

bREPL lets you interact with any clojure application that is running a [io-prepl](https://clojuredocs.org/clojure.core.server/io-prepl) socket server.

## How does it work?

By including the following jvm options it is possible to connect to your running repl via a tcp connection.
```
-Dclojure.server.repl={:port 8888 :accept clojure.core.server/io-prepl}
```

Web browsers don't allow raw sockets. However, we can open up a websocket and connect to a second server that can act as a "proxy".

A small server [brepl-server]() (written in GO) serves this role as well as serving the static html/js/ect for the frontent.

```
[BROWSER]<---websocket--->[brepl-server]<---TCP--->[CLOJURE]
                                                       |
												       |
[EMACS/VSCODE]<-------------------------------------[nREPL]
```
The nice thing about this is that you can open a browser as well as having
a project open in say, emacs.

## brepl-server

Start the server on your favourite port.

```bash
$ cd brepl-server
$ go run main.go -port 8080 -serve-from ../public
```

You can now go to [http://localhost:8080](http://localhost:8080)! Notice that while you can interact with the page, you **can not** open a connection!

## Lein
For a quick test you can add the following `jvm-opts` to your `:user` profile located at `~/.lein/profiles.clj`. You can can use whatever value for `:port` you want.

```clojure
{:user {:jvm-opts ["-Dclojure.server.repl={:port 8888 :accept clojure.core.server/io-prepl}"]}}
```

_Note that you will have issues if you try to open more than one project as there will be a bound socket (8888) already in use._

This will start a `prepl` server when you run `lein repl` from the command line - or use `cider-jack-in` within emacs.

If you go back to [http://localhost:8080](http://localhost:8080) you will now be able to connect using ports `8080` and `8888`

To test you can use the `nc` command

```clojure
$ nc 127.0.0.1 8888
*ns*
{:tag :ret, :val "#object[clojure.lang.Namespace 0x64d79c43 \"user\"]", :ns "user", :ms 0, :form "*ns*"}
```


