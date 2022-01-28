import type { ParseOptions } from "edn-data/dist/parse"

export interface Addr {
    hostname?: string
    port: string
}

export type ProxyAddr = Addr;

export interface ReplAddr extends Addr {
    type: string
}

export interface ReplImpl {
    connect(): Promise<boolean>;
    eval<T>(expr: string, parseOptions?: ParseOptions): Promise<T>
}
