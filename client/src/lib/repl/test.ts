import { Prepl } from "./prepl";
import { Repl, ednParseOptions } from "./repl";

import type { Symbol } from "./clojure";

export const test = async () => {
    const repl = new Repl(new Prepl({ port: "8888" }, { port: "7777" }, ednParseOptions))
    await repl.connect();
    const results: any[] = [];

    let result = await repl.eval<number>("(+ 1 2 3)");
    results.push(result);

    result = await repl.eval(42);
    results.push(result);

    let boolResult = await repl.eval<boolean>(true);
    results.push(boolResult);

    let nullResult = await repl.eval<null>('(println "hello")');
    results.push(nullResult);

    let nsResult = await repl.eval<Symbol>("(ns-name *ns*)");
    results.push(nsResult.sym);

    let strResult = await repl.currentNamespace();
    results.push(strResult);

    let anyResult = await repl.currentNamespaceSymbols()
    results.push(anyResult);

    anyResult = await repl.allLoadedNamespaceNames()
    results.push(anyResult)

    let treeResult = await repl.loadedNamespaceTree();
    results.push(treeResult)

    console.log(results);
}