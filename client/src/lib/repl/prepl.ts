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
    private socket: WebSocket
    private callId = 0;
    private callbacks: Map<number, any>;
    private parseOptions: ParseOptions
    private awaitingConnection: ((value:boolean)=>void)[]

    private isConnected = false;

    constructor(proxyAddr: Addr, replAddr: Addr, parseOptions: ParseOptions) {
        this.callbacks = new Map();
        this.parseOptions = parseOptions
        this.awaitingConnection = []

        this.socket = new WebSocket(websocketURL(proxyAddr, replAddr))

        this.socket.onopen = (ev: Event) => {
            this.isConnected = true            
            this.awaitingConnection.forEach((f) => f(true))
            this.awaitingConnection = []
        }

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
    }

    public get connected() {
        if (this.isConnected) {
            return Promise.resolve(true)
        }
        return new Promise((resolve : (value : boolean) => void, reject) => {
            this.awaitingConnection.push(resolve)
        })
    }

    public eval<T>(expr: string): Promise<T> {
        return new Promise((resolve, reject) => {
            const id = this.callId++
            this.callbacks.set(id, (data: unknown) => {
                resolve(data as T)
            })
            this.socket.send(`{:id ${id} :expr ${expr}}`)
        })
    }

}
