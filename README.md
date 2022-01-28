# b**REPL**

### Explore **any** clojure application via your browser.

---

bREPL is a single binary that provides:

1. A Web Socket to TCP proxy/bridge a la [websockify](https://github.com/novnc/websockify).
2. A browser based front end for navigating data.

If your application can expose an [io-prepl](https://clojuredocs.org/clojure.core.server/io-prepl) server then you're good to go.

---

# Building

Make sure you have `golang` and `npm` installed.

```bash
$ git clone git@github.com:beoliver/brepl.git
$ cd brepl
$ make
```

You can just copy the crerated binary and put it wherever you want.

```bash
$ mv server/bin/brepl ~/bin/brepl
```

# Use

In the following example it is assumed that: 

2. The prepl server is open on port `7777`
3. the bREPL server is open on port `8888`

## Starting the bREPL server

```
$ brepl -p 8888
```

Direct your browser to  [http://localhost:8888](http://localhost:8888).


## Starting the prepl

If you have an existing project that you're developing, chances are you're using nrepl. While bREPL can use nREPL - it is easier (for now) to simply start an `io-prepl` server from you're repl.

```clojure
user> (def server 
        (clojure.core.server/start-server
         {:name "sockserver" 
          :port 7777
          :accept 'clojure.core.server/io-prepl}))
#'user/server
user> server
#object[java.net.ServerSocket 0x6377f56e "ServerSocket[addr=localhost/127.0.0.1,localport=7777]"]
user> (.close server)
nil
```

By including the following jvm options it is possible to start a server directly - for example

```clojure
-Dclojure.server.repl={:port 7777 :accept clojure.core.server/io-prepl}
```

For example, to just craete an "empty" repl, we could run the following clojure cli command

```bash
$ clj -J-Dclojure.server.repl="{:port 7777 :accept clojure.core.server/io-prepl}"
Clojure 1.10.3
user=> 
```

## Lein

For a quick test you can add the following `jvm-opts` to your `:user` profile located at `~/.lein/profiles.clj`. You can can use whatever value for `:port` you want.

```clojure
{:user {:jvm-opts ["-Dclojure.server.repl={:port 7777 :accept clojure.core.server/io-prepl}"]}}
```

_Note that you will have issues if you try to open more than one project as there will be a bound socket (7777) already in use._



