import { parseEDNString } from "edn-data"
import type { ParseOptions } from "edn-data/dist/parse"
import type { Addr, ReplImpl } from "./types";

interface PreplResult<T> {
    tag: "ret" | "out",
    val: T,
    ns: string,
    ms: number,
    form: string,
    exception?: boolean
}

interface Execution {
    id: number;
    expr: unknown
}

const websocketURL = (proxy: Addr, repl: Addr) =>
    `ws://${proxy.hostname || "localhost"}:${proxy.port}/prepl/${repl.hostname || "localhost"}:${repl.port}`

export class Prepl implements ReplImpl {

    private url: string

    private socket!: WebSocket
    private parseOptions: ParseOptions

    private callId = 0;
    private callbacks: Map<number, { callback: (data: any, error: any) => any, parseOptions?: ParseOptions }>;

    constructor(proxyAddr: Addr, replAddr: Addr, parseOptions: ParseOptions) {
        this.callbacks = new Map();
        this.parseOptions = parseOptions
        this.url = websocketURL(proxyAddr, replAddr)
    }

    public async connect(): Promise<boolean> {

        this.socket = new WebSocket(this.url)

        this.socket.onmessage = (ev: MessageEvent<string>) => {
            const edn = parseEDNString(ev.data, this.parseOptions) as PreplResult<any>
            console.log(edn)
            switch (edn.tag) {
                case "out": {
                    console.log("IO:", edn.val)
                    break
                }
                case "ret": {
                    if (edn.exception) {
                        console.warn("EXCEPTION")
                        console.warn(edn)
                    } else {
                        // parse the form first to extract the id
                        let id: number
                        try {
                            const val = parseEDNString(edn.val, { mapAs: "object", keywordAs: "string" }) as { id: number }
                            id = val.id;
                        } catch (error) {
                            console.log("error parsing val for id")
                            throw new Error("noooooo!")
                        }
                        const callbackAndParseOptions = this.callbacks.get(id);
                        if (callbackAndParseOptions === undefined) {
                            throw new Error("whoops - no callback")
                        }
                        const { callback, parseOptions } = callbackAndParseOptions
                        // remove the callback to avoid a memory leak      
                        this.callbacks.delete(id)
                        try {                            
                            const { expr } = parseEDNString(edn.val, parseOptions || this.parseOptions) as { expr: any }
                            console.log(expr)
                            callback(expr, null)
                        } catch (err) {
                            callback(edn.val, err)
                        }
                    }
                    break
                }
                default: {

                }
            }
        }

        return new Promise((resolve, reject) => {
            this.socket.onopen = (ev: Event) => {
                resolve(true)
            }
            this.socket.onclose = (ev: Event) => {
                console.log(ev)
                resolve(false)
            }
            this.socket.onerror = (ev: Event) => {
                window.alert(JSON.stringify(ev))
                reject(ev)
            }
        })

    }

    public eval<T>(expr: string, parseOptions?: ParseOptions): Promise<T> {
        return new Promise((resolve, reject) => {
            const id = this.callId++
            this.callbacks.set(id, {
                callback: (data: unknown, err: unknown) => {
                    if (err) {
                        reject(err)
                    }
                    resolve(data as T)
                },
                parseOptions
            })
            this.socket.send(`{:id ${id} :expr ${expr}}`)
        })
    }

}
