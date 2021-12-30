import { parseEDNString } from "edn-data"
import type { ParseOptions } from "edn-data/dist/parse"
import type { Addr, ReplImpl } from "./repl";

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
    private socket!: WebSocket
    private callId = 0;
    private callbacks: Map<number, any>;
    private proxyAddr: Addr
    private replAddr: Addr
    private parseOptions: ParseOptions

    constructor(proxyAddr: Addr, replAddr: Addr, parseOptions: ParseOptions) {
        this.callbacks = new Map();
        this.proxyAddr = proxyAddr
        this.replAddr = replAddr
        this.parseOptions = parseOptions
    }

    public connect(): Promise<void> {
        this.socket = new WebSocket(websocketURL(this.proxyAddr, this.replAddr))

        this.socket.onmessage = (ev: MessageEvent<string>) => {
            const edn = parseEDNString(ev.data, this.parseOptions) as PreplResult<any>
            if (edn.tag === "out") {
                console.log("IO:", edn.val)
            }
            if (edn.tag === "ret") {
                if (edn.exception) {
                    console.warn("EXCEPTION")
                    console.warn(edn)
                } else {
                    const { id, expr } = parseEDNString(edn.val, this.parseOptions) as Execution
                    const callback = this.callbacks.get(id)
                    // assume callback is there _shrug_
                    this.callbacks.delete(id)
                    // remove the callback to avoid a memory leak
                    callback(expr)
                }
            }
        }

        return new Promise((resolve, reject) => {
            this.socket.onopen = (ev: Event) => {
                resolve()
            }
            this.socket.onerror = (ev: Event) => {
                reject(ev)
            }
        })
    }

    public eval<T>(expr: any): Promise<T> {
        return new Promise((resolve, reject) => {
            const id = this.callId++
            this.callbacks.set(id, (data: unknown) => {
                resolve(data as T)
            })
            this.socket.send(`{:id ${id} :expr ${expr}}`)
        })
    }

}
