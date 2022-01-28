import React, { ChangeEvent, useCallback, useEffect, useState } from "react"
import styled from "styled-components"
import { Symbol } from "../lib/repl/clojure"
import { Repl } from "../lib/repl/repl"
import { parseAsNamespaceQualified, NamespaceQualified } from "../lib/repl/utils"
import { loadSymbol, ViewerAction } from "./Viewer"

interface Props { label?: string, repl: Repl, setAction: (a: ViewerAction) => void }

const Wrapper = styled.div`
    width: 100%;    
`
const Controls = styled.div`
    width: 100%;
`
const Content = styled.div`    
    background-color: #fbfbfb;    
    height: calc(100vh - 5em);
    overflow-x: scroll;
    overflow-y: scroll;
`



const Apropos: React.FunctionComponent<Props> = ({ repl, setAction }) => {

    const [groupByNamespace, setGroupByNamespace] = useState(true)
    const [filterRegex, setFilterRegex] = useState({ regex: new RegExp(""), display: "" })

    const [apropos, setApropos] = useState<string>()
    const [results, setResults] = useState<{
        byNs: Map<string, NamespaceQualified[]>,
        byName: Map<string, NamespaceQualified[]>,
        list: NamespaceQualified[],
    }>(
        {
            byNs: new Map(),
            byName: new Map(),
            list: []
        }
    )

    const handleChange = (event: ChangeEvent<HTMLInputElement>) => {
        try {
            const regex = new RegExp("^" + event.target.value)
            setFilterRegex({ regex, display: event.target.value })
        } catch (error) { }
    }

    const f = useCallback((apropos: string) => {
        repl.eval<Symbol[]>(`(clojure.repl/apropos "${apropos}")`).then((results) => {
            const parsed: NamespaceQualified[] = []
            const byNs = new Map<string, NamespaceQualified[]>()
            const byName = new Map<string, NamespaceQualified[]>()
            results.forEach((x) => {
                const q = parseAsNamespaceQualified(x)
                if (q && q.ns.match(filterRegex.regex)) {
                    parsed.push(q)
                    if (byNs.has(q.ns)) {
                        byNs.get(q.ns)!.push(q)
                    } else {
                        byNs.set(q.ns, [q])
                    }
                    if (byName.has(q.symbol)) {
                        byName.get(q.symbol)!.push(q)
                    } else {
                        byName.set(q.symbol, [q])
                    }
                }
            })
            setResults({ list: parsed, byNs, byName })
        })
    }, [repl, filterRegex])

    useEffect(() => {
        if (apropos && apropos.length > 2) {
            f(apropos)
        }
    }, [apropos])

    return (
        <Wrapper>
            <Controls>
                <div style={{ display: "flex" }}>
                    <input
                        placeholder="namespace regex"
                        type="text"
                        value={filterRegex.display}
                        onChange={handleChange} />
                    <input
                        placeholder="symbol regex"
                        type="text"
                        value={apropos}
                        onChange={(event) => setApropos(event.target.value)} />
                </div>
                <div>
                    <label>
                        <input
                            type="checkbox"
                            checked={groupByNamespace}
                            onChange={(_) => setGroupByNamespace(!groupByNamespace)}
                        />
                        Group by Namespace
                    </label>
                    <button
                        onClick={async (e: any) => apropos ? f(apropos) : null}>
                        Apropos
                    </button>
                </div>

            </Controls>
            <Content>
                {groupByNamespace ? Array.from(results.byNs.entries()).map(([ns, symbols], i) => {
                    return (
                        <div key={i} style={{ overflowX: "scroll" }}>
                            <pre>{ns}</pre>
                            <div style={{ paddingLeft: "1em" }} >
                                {symbols.map((s, i) => <AproposSymbol onClick={(_) => setAction(loadSymbol(s.ns, s.symbol))}>
                                    <pre style={{ fontWeight: "bold" }} key={i}>{s.symbol}</pre>
                                </AproposSymbol>)}
                            </div>
                        </div>)
                }) : Array.from(results.byName.entries()).map(([name, symbols], i) => {
                    return (
                        <div key={i} style={{ overflowX: "scroll" }}>
                            <pre>{name}</pre>
                            <div style={{ paddingLeft: "1em" }}>
                                {symbols.map((s, i) => <div onClick={(_) => setAction(loadSymbol(s.ns, s.symbol))}><pre style={{ fontWeight: "bold" }} key={i}>{s.qualified}</pre></div>)}
                            </div>
                        </div>)
                })}

            </Content>

        </Wrapper>
    )
}

export default Apropos



const AproposSymbol = styled.div`    
    &:hover {
        color: blue;
        cursor: pointer;
    }
`

// onMouseOver={(_) => setAction(loadSymbol(s.ns, s.symbol))} 