import { parseEDNString } from "edn-data"
import type { ParseOptions } from "edn-data/dist/parse"
import type { Addr, ReplImpl } from "./repl";
import { encode, decode, BencodeDict } from "./bencode";


const websocketURL = (proxy: Addr, repl: Addr) =>
    `ws://${proxy.hostname || "localhost"}:${proxy.port}/nrepl/${repl.hostname || "localhost"}:${repl.port}`

export class Nrepl implements ReplImpl {
    private socket!: WebSocket
    private callId = 0;
    private callbacks: Map<number, any>;
    private proxyAddr: Addr
    private replAddr: Addr
    private parseOptions: ParseOptions
    private sessionId?: string
    private isConnected = false;
    private blocking: ((value: boolean) => void)[]

    constructor(proxyAddr: Addr, replAddr: Addr, parseOptions: ParseOptions) {
        this.callbacks = new Map();
        this.proxyAddr = proxyAddr
        this.replAddr = replAddr
        this.parseOptions = parseOptions
        this.blocking = []

        console.log("connect");
        this.socket = new WebSocket(websocketURL(this.proxyAddr, this.replAddr))

        this.socket.onopen = (ev: Event) => {
            console.log("socket opened")
            this.socket.send("d2:op5:clonee")
        }

        this.socket.onmessage = (ev: MessageEvent<string>) => {
            console.log("MESSAGE", ev.data)
            const data = decode(ev.data) as BencodeDict
            console.log(JSON.stringify(data, null, 2))

            if (!this.sessionId) {
                this.sessionId = data["new-session"] as string
                this.isConnected = true
                this.blocking.forEach((f) => f(true))
                this.blocking = []
            } else {
                console.log(JSON.stringify(data, null, 2))
                const id = data["id"] as number
                const expr = data["value"]
                if (expr) {
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
        if (this.isConnected) {
            return Promise.resolve(true)
        }
        return new Promise((resolve: (value: boolean) => void, reject) => {
            this.blocking.push(resolve)
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
