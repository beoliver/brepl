import React, { useEffect, useState } from "react"
import { Meta, Repl } from "../lib/repl/repl"
import { NamespaceQualified } from "../lib/repl/utils"
import { LoadedSymbol, LoadSymbolResult } from "./actions/LoadedSymbol"

export interface ViewerAction {
    loadNamespace?: { ns: string }
    loadSymbol?: { ns: string, symbol: string }
}

export const loadSymbol = (ns: string, symbol: string) => {
    return { loadSymbol: { ns, symbol } }
}


type Result = LoadSymbolResult;

interface ViewerProps { repl: Repl, action?: ViewerAction, ns? : string }


export const Viewer: React.FunctionComponent<ViewerProps> = ({ repl, action }) => {

    const [result, setResult] = useState<Result>()

    useEffect(() => {
        if (action === undefined) {
            return
        }
        if (action.loadNamespace) {
            const { ns } = action.loadNamespace
        }
        if (action.loadSymbol) {
            const { ns, symbol } = action.loadSymbol;
            (async () => {
                const m = await repl.metaForSymbol(ns, symbol);
                const result: LoadSymbolResult = {
                    action: "loadSymbol",
                    args: { ns, symbol },
                    result: m
                }
                setResult(result)
            })()
        }
    }, [action])

    if (!result) {
        return <div></div>
    }

    if (result.action === "loadSymbol") {
        return <LoadedSymbol {... { repl, result }} />
    }

    return <div></div>

}
