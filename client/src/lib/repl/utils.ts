import type { Symbol } from "./clojure"

export interface NamespaceQualified {
    ns: string
    symbol: string
    qualified: string
}

export const parseAsNamespaceQualified = (s: Symbol | string): NamespaceQualified | undefined => {
    if (typeof s !== "string") {
        s = s.sym
    }
    const matches = s.match(/^(?<ns>.+)\/(?<symbol>.+)$/);
    if (matches && matches.groups) {
        return {
            ns: matches.groups.ns,
            symbol: matches.groups.symbol,
            qualified: s
        }
    }
}

export const x = 1