import { parseEDNString } from "edn-data"
import type { ParseOptions } from "edn-data/dist/parse"
import type { Addr, ReplImpl } from "./repl";
import { encode, decode, BencodeDict } from "./bencode";


const websocketURL = (proxy: Addr, repl: Addr) =>
    `ws://${proxy.hostname || "localhost"}:${proxy.port}/nrepl/${repl.hostname || "localhost"}:${repl.port}`

export class Nrepl implements ReplImpl {

    private socket: WebSocket

    private callId = 0;
    private callbacks: Map<number, any>;


    private parseOptions: ParseOptions
    private sessionId?: string
    private isConnected?: boolean;
    private awaitingConnection: ((value: boolean) => void)[]

    private setConnectionStatus(connected: boolean) {
        this.isConnected = connected
        this.awaitingConnection.forEach((f) => f(connected))
        this.awaitingConnection = []
    }

    constructor(proxyAddr: Addr, replAddr: Addr, parseOptions: ParseOptions) {
        this.callbacks = new Map();
        this.parseOptions = parseOptions
        this.awaitingConnection = []

        this.socket = new WebSocket(websocketURL(proxyAddr, replAddr))

        this.socket.onopen = (ev: Event) => {
            this.socket.send(encode({ op: "clone" }))
        }

        this.socket.onclose = (ev: Event) => {
            this.setConnectionStatus(false)
        }

        this.socket.onmessage = (ev: MessageEvent<string>) => {
            console.log(ev.data)
            const data = decode(ev.data) as BencodeDict

            if (!this.sessionId) {
                this.sessionId = data["new-session"] as string
                this.setConnectionStatus(true)
            } else {
                const id = data["id"] as number
                const expr = data["value"]
                if (id !== undefined && expr !== undefined) {
                    const callback = this.callbacks.get(id)
                    // assume callback is there _shrug_
                    this.callbacks.delete(id)
                    // remove the callback to avoid a memory leak                    
                    callback(parseEDNString(expr as string, this.parseOptions))
                }
            }
        }
    }

    public get connected() {
        if (this.isConnected !== undefined) {
            return Promise.resolve(this.isConnected)
        }
        return new Promise((resolve: (value: boolean) => void, reject) => {
            this.awaitingConnection.push(resolve)
        })
    }

    public eval<T>(expr: string): Promise<T> {
        console.log("called eval")
        return new Promise((resolve, reject) => {
            const id = this.callId++
            this.callbacks.set(id, (data: unknown) => {
                resolve(data as T)
            })
            const payload = { "session": this.sessionId!, id, op: "eval", code: expr }
            const encoded = encode(payload)
            console.log(payload)
            console.log(encoded)
            this.socket.send(encode(payload))
        })
    }

}
