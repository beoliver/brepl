# brepl
a browser based repl for clojure

1. Make sure you have a copy of `clojure-tools-1.10.3.986.jar` in the `/jars` directory.
2. Start the prepl clojure server on port `8888`
    ```bash
    $ ./prepl.sh 8888
    ```
3. Start the server. This is for the frontend. The server doesn't need to know about the `prepl` server port.
    ```bash
    $ cd server
    $ go run main.go
    ```
4. got to [http://localhost:7777](http://localhost:7777)