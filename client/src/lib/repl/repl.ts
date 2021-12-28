import { ParseOptions } from "edn-data/dist/parse"
import type { Symbol } from "./clojure"

export interface Addr {
    hostname?: string
    port: string
}

export type ProxyAddr = Addr;

export interface ReplAddr extends Addr {
    type: string
}


export const ednParseOptions: ParseOptions = {
    mapAs: "object",
    setAs: "array",
    listAs: "array",
    keywordAs: "string",
    charAs: "string",
}

export interface ReplImpl {
    connect(): Promise<void>;
    eval<T>(expr: any): Promise<T>
}

export class Repl {

    private repl: ReplImpl
    constructor(repl: ReplImpl) {
        this.repl = repl
    }

    public async connect() {
        await this.repl.connect()
    }

    public async eval<T>(expr: any) {
        return this.repl.eval<T>(expr)
    }

    public async currentNamespace(): Promise<string> {
        const sym = await this.repl.eval<Symbol>("(ns-name *ns*)")
        return sym.sym
    }

    public async allLoadedNamespaceNames(): Promise<string[]> {
        return this.repl.eval<Array<string>>(`(mapv (comp name ns-name) (all-ns))`)
    }

    public async loadedNamespaceTree(): Promise<NsTreeValue> {
        const namespaces = await this.allLoadedNamespaceNames();
        return nsNameTree(namespaces)
    }

    public async nsMapKeys(ns: string): Promise<string[]> {
        return this.repl.eval<Array<string>>(`(mapv name (keys (ns-map ${ns})))`)
    }

    public async currentNamespaceSymbols(): Promise<string[]> {
        return this.nsMapKeys("*ns*")
    }

    public async metaForNsPublics(ns: string): Promise<Meta[]> {
        const data = await this.repl.eval<Meta[]>(`(identity (mapv #(select-keys (meta %) [:added :name]) (vals (ns-publics ${ns}))))`)
        console.log(data)
        return data
    }

}

export interface Meta {
    arglists: any;
    doc: string;
    added: string;
    line: number;
    column: number;
    file: string;
    name: Symbol;
    ns: any
}

type NsTree = { [key: string]: NsTreeValue }
export type NsTreeValue = { ns?: string, children: NsTree }

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
                    tree[segment] = { ns, children: {} }
                } else {
                    tree[segment] = { children: {} }
                }
            } else {
                if (isLastSegmentinNamespace) {
                    // console.log(ns)
                    tree[segment].ns = ns
                }
            }
            tree = tree[segment].children!
        })
        tree = root
    })
    return { children: tree }
}
