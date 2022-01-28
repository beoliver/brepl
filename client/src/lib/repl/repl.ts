import { ParseOptions } from "edn-data/dist/parse"
import type { Symbol } from "./clojure"
import type { ReplImpl } from "./types"

export const ednParseOptions: ParseOptions = {
    mapAs: "object",
    setAs: "array",
    listAs: "array",
    keywordAs: "string",
    charAs: "string",
}

export class Repl {

    private repl: ReplImpl

    constructor(repl: ReplImpl) {
        this.repl = repl
    }

    public connect() {
        return this.repl.connect()
    }

    public async eval<T>(expr: string, parseOptions?: ParseOptions) {
        return this.repl.eval<T>(expr, parseOptions)
    }

    public async currentNamespace(): Promise<string> {
        const sym = await this.repl.eval<Symbol>("(ns-name *ns*)")
        return sym.sym
    }

    public async allLoadedNamespaceNames(): Promise<string[]> {
        return this.repl.eval<Array<string>>(`(mapv (comp name ns-name) (all-ns))`)
    }

    public async allSymbols(): Promise<string[]> {
        return this.repl.eval<Array<string>>(`(map symbol (mapcat (comp vals ns-interns) (all-ns)))`)
    }

    public async nsMapKeys(ns: string): Promise<string[]> {
        return this.repl.eval<Array<string>>(`(mapv name (keys (ns-map ${ns})))`)
    }

    public async currentNamespaceSymbols(): Promise<string[]> {
        return this.nsMapKeys("*ns*")
    }

    public async metaForNsPublics(ns: string): Promise<Meta[]> {
        const data = await this.repl.eval<Meta[]>(`(mapv #(update (meta %) :arglists str) (vals (ns-publics ${ns})))`)
        console.log(data)
        return data
    }
    public async metaForNsInterns(ns: string): Promise<Meta[]> {
        const data = await this.repl.eval<Meta[]>(`(mapv #(update (meta %) :arglists str) (vals (ns-interns ${ns})))`)
        console.log(data)
        return data
    }
    public async sourceFor(ns: string, name: string): Promise<string> {
        const expr = `(clojure.repl/source-fn '${ns}/${name})`
        const data = await this.repl.eval<string>(expr)
        return data
    }

    public async allSpecs() {
        const expr = `(keys (clojure.spec.alpha/registry))`
        const data = await this.repl.eval<Array<string | Symbol>>(expr)
        return data
    }

    public async specExample(spec: string) {
        const expr = `(pr-str (clojure.spec.gen.alpha/generate (clojure.spec.alpha/gen (clojure.spec.alpha/get-spec ${spec}))))`
        const data = await this.repl.eval<string>(expr)
        return data
    }

    public async multiMethodDispatchKeys(ns: string, name: string) {
        const expr = `(mapv pr-str (clojure.core/keys (clojure.core/methods ${ns}/${name})))`
        const data = await this.repl.eval<string[]>(expr)
        return data
    }

    public async multiMethodDispatch(ns: string, name: string) {
        const expr = `(mapv (fn [[k v]] [(pr-str k) (.getName (class v))]) (clojure.core/methods ${ns}/${name}))`
        const data = await this.repl.eval<[string, string][]>(expr)
        return data
    }

    public async inNs(ns: string) {
        const expr = `(in-ns ${ns})`
        const data = await this.repl.eval<any>(expr)
        return data
    }

    public async namespaceMeta(ns: string) {
        const expr = `(clojure.core/meta (clojure.lang.Namespace/find ${ns}))`
        const data = await this.repl.eval<NamespaceMeta | null>(expr);
        return data
    }

    public async metaForSymbol(ns: string, symbol : string) {
        const expr = `(clojure.core/update (clojure.core/meta #'${ns}/${symbol}) :arglists str)`
        const data = await this.repl.eval<Meta | null>(expr);
        return data
    }

}

export interface NamespaceMeta {
    doc?: string;
    author?: string;
    added?: string;
}

export interface Meta {
    arglists: string;
    doc: string;
    added: string;
    line: number;
    column: number;
    file: string;
    name: Symbol;
    ns: any
    deprecated?: string;
    private?: boolean;
    protocol?: { tag: string, val: string }
}

type NsTree = { [key: string]: NsTreeValue }
export type NsTreeValue = { ns?: { ns: string, segment: string }, children: NsTree }

export const nsNameTree = (namespaces: string[]): NsTreeValue => {
    // TODO - this is too janky!
    let tree: NsTree = {}
    namespaces.forEach((ns: string) => {
        const segments = ns.split(".")
        let root = tree
        // make a copy of the tree that we will mutate
        // and recurively "move" down into
        segments.forEach((segment: string, index: number) => {
            // if there is no key for a segment eg "bar"
            const isLastSegmentinNamespace = index === segments.length - 1
            if (!tree[segment]) {
                if (isLastSegmentinNamespace) {
                    // console.log(ns)
                    tree[segment] = { ns: { ns, segment }, children: {} }
                } else {
                    tree[segment] = { children: {} }
                }
            } else {
                if (isLastSegmentinNamespace) {
                    // console.log(ns)
                    tree[segment].ns = { ns, segment }
                }
            }
            tree = tree[segment].children!
        })
        tree = root
    })
    return { children: tree }
}
