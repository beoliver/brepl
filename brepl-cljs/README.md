
## Repl

From this directory run

```bash
clj -M --main cljs.main --compile brepl.core --repl
```

## Building

```bash
clj -M -m cljs.main --optimizations advanced -c brepl.core
```

Examine `out/main.js` - you can zip it
