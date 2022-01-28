import { useCallback, useEffect, useState } from "react"
import styled from "styled-components"
import { Meta, Repl } from "../../lib/repl/repl"

export interface LoadSymbolResult {
    action: "loadSymbol";
    args: { ns: string, symbol: string }
    result: Meta | null
}

const Name = styled.h3`
    margin-bottom: 0em;    
`

const Source: React.FunctionComponent<{ source?: string }> = ({ source }) => {
    if (source) {
        return (
            <div>
                <hr />
                <pre>{source}</pre>
                <hr />
            </div>
        )
    }
    return <div></div>
}

const Documentation: React.FunctionComponent<{ doc?: string }> = ({ doc }) => {
    if (doc) {
        return (
            <div style={{ overflowX: "scroll" }}>
                <pre>{doc}</pre>
            </div>
        )
    }
    return <div></div>
}

const Arglists: React.FunctionComponent<{ arglists?: string }> = ({ arglists }) => {
    if (arglists) {
        return (
            <div style={{ overflowX: "scroll" }}>
                <pre>{arglists}</pre>
            </div>
        )
    }
    return <div></div>
}

export const LoadedSymbol: React.FunctionComponent<{ repl: Repl, result: LoadSymbolResult }> = ({ repl, result }) => {
    const [source, setSource] = useState<string>()
    const meta = result.result

    useEffect(() => {
        if (repl) {
            (async () => {
                const source = await repl.sourceFor(result.args.ns, result.args.symbol)
                setSource(source)
            })()
        }
    }, [meta])

    if (!meta) {
        return <div></div>
    }
    return (
        <div>
            <div>file:<code>{meta.file}</code></div>
            <div>ns:<code>{result.args.ns}</code></div>
            <div>public:<code>{result.result?.private ? "false" : "true"}</code></div>
            <Name>{result.args.symbol}</Name>
            <div></div>
            {meta.protocol ? <p>{meta.protocol.tag}</p> : <p></p>}
            <div style={{ fontSize: "0.9em", display: "flex", alignItems: "center", justifyContent: "space-between" }}>
                <div>{meta.added ? <p>added: {meta.added}</p> : <></>}</div>
            </div>
            <Arglists {...meta} />
            <Documentation {...meta} />
            <Source source={source} />
        </div>
    )
}