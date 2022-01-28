import React, { ChangeEvent, useCallback, useEffect, useState } from "react"
import styled from "styled-components"
import { Symbol } from "../lib/repl/clojure"
import { Repl } from "../lib/repl/repl"
import { parseAsNamespaceQualified, NamespaceQualified } from "../lib/repl/utils"
import { loadSymbol, ViewerAction } from "./Viewer"

interface Props { label?: string, repl: Repl, setAction: (a: ViewerAction) => void , ns? : string}

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

interface InputProps {
    value?: string
    onChange: (event: React.ChangeEvent<HTMLInputElement>) => void
}

interface SearchBarProps {
    ns: InputProps,
    symbol: InputProps
}

const SearchBar: React.FunctionComponent<SearchBarProps> = (props) => {
    return (
        <div style={{ display: "flex", width: "100%", alignItems: "center" }}>
            <input
                style={{ width: "50%", padding: "0.5em 0.5em" }}
                placeholder="namespace regex"
                type="text"
                value={props.ns.value}
                onChange={props.ns.onChange} />
            <code style={{ marginLeft: "0.5em", marginRight: "0.5em" }}>/</code>
            <input
                style={{ width: "50%", padding: "0.5em 0.5em" }}
                placeholder="symbol regex"
                type="text"
                value={props.symbol.value}
                onChange={props.symbol.onChange} />
        </div>
    )
}


interface Filters {
    ns: { regex: RegExp, display: string }
    symbol: { regex: RegExp, display: string }
}

const SymbolSearch: React.FunctionComponent<Props> = ({ repl, setAction, ns }) => {

    const [filters, setFilters] = useState<Filters>({
        ns: { regex: new RegExp(""), display: "" },
        symbol: { regex: new RegExp(""), display: "" }
    })

    const [groupByNamespace, setGroupByNamespace] = useState(true)
    const [allSymbols, setAllSymbols] = useState<NamespaceQualified[]>([])
    const [displaySymbol, setDisplaySymbols] = useState<NamespaceQualified[]>([])
    const [byNamespace, setByNamespace] = useState<Map<string, NamespaceQualified[]>>(new Map())

    useEffect(() => {
        if (ns) {
            const regex = new RegExp("^" + ns)
            setFilters((value) => { return { ...value, ns: { regex, display: ns } } })
        }
    }, [ns, setFilters])

    const handlefilter = useCallback((event: ChangeEvent<HTMLInputElement>, k: "ns" | "symbol") => {
        try {
            const display = event.target.value            
            switch (k) {
                case "ns": {
                    const regex = new RegExp("^" + display)
                    setFilters((value) => { return { ...value, ns: { regex, display } } })
                    break;
                }
                case "symbol": {
                    const regex = new RegExp(display)
                    setFilters((value) => { return { ...value, symbol: { regex, display } } })
                    break;
                }
            }
        } catch (error) { }
    }, [setFilters])

    useEffect(() => {
        (async () => {
            const symbols = await repl.allSymbols()
            setAllSymbols(symbols.map((x) => parseAsNamespaceQualified(x)!))
        })()
    }, [repl, setAllSymbols])

    useEffect(() => {
        const byNs = new Map<string, NamespaceQualified[]>()
        const filtered = allSymbols.filter(x => x.ns.match(filters.ns.regex) && x.symbol.match(filters.symbol.regex))
        filtered.forEach((x) => {
            if (byNs.has(x.ns)) {
                byNs.get(x.ns)!.push(x)
            } else {
                byNs.set(x.ns, [x])
            }
        })
        setDisplaySymbols(filtered)
        setByNamespace(byNs)
    }, [filters, allSymbols, setDisplaySymbols, setByNamespace])

    return (
        <Wrapper>
            <Controls>
                <SearchBar
                    ns={{ value: filters.ns.display, onChange: (event) => handlefilter(event, "ns") }}
                    symbol={{ value: filters.symbol.display, onChange: (event) => handlefilter(event, "symbol") }}
                />
            </Controls>
            <label>
                <input
                    type="checkbox"
                    checked={groupByNamespace}
                    onChange={(_) => setGroupByNamespace(!groupByNamespace)}
                />
                Group by Namespace
            </label>
            <Content>
                {groupByNamespace ? Array.from(byNamespace.entries()).map(([ns, symbols], i) => {
                    return (
                        <div key={i} style={{ overflowX: "scroll" }}>
                            <pre>{ns}</pre>
                            <div style={{ paddingLeft: "1em" }} >
                                {symbols.map((s, i) => <AproposSymbol key={i} onClick={(_) => setAction(loadSymbol(s.ns, s.symbol))}>
                                    <pre style={{ fontWeight: "bold" }} >{s.symbol}</pre>
                                </AproposSymbol>)}
                            </div>
                        </div>)
                }) : displaySymbol.map((x,i) => <AproposSymbol key={i} onClick={(_) => setAction(loadSymbol(x.ns, x.symbol))}>
                <pre style={{ fontWeight: "bold" }} >{x.qualified}</pre>
            </AproposSymbol>)
                }


            </Content>
        </Wrapper>
    )
}

export default SymbolSearch



const AproposSymbol = styled.div`    
    &:hover {
        color: blue;
        cursor: pointer;
    }
`

