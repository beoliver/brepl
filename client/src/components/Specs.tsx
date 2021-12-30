import React, { ChangeEvent, useEffect, useState } from "react"
import { Repl } from "../lib/repl/repl"
import type { Symbol } from "../lib/repl/clojure";

interface Props { repl: Repl, ns?: string }


const filterEntries = (entries: IterableIterator<[string, string[]]>, keyRegex: RegExp, valRegex: RegExp) => {
    const xs = Array.from(entries).map(([k, vals]) => [k, vals.filter(v => v.match(valRegex))]) as [string, string[]][]
    return xs.filter(([k, v]) => k.match(keyRegex) && v.length > 0)
}

const Specs: React.FunctionComponent<Props> = ({ repl, ns }) => {

    const [customNsRegex, setCustomNsRegex] = useState(false)
    const [sortByNs, setSortByNs] = useState<boolean>(false)
    const [nsRegex, setNsRegex] = useState({ regex: new RegExp(ns || ""), display: ns || "" })
    const [nameRegex, setNameRegex] = useState({ regex: new RegExp(""), display: "" })

    useEffect(() => {
        if (ns && !customNsRegex) {
            setNsRegex({ regex: new RegExp(ns), display: ns })
        }
    }, [ns])

    const handleNsRegexChange = (event: ChangeEvent<HTMLInputElement>) => {
        try {
            const regex = new RegExp("^" + event.target.value)
            setNsRegex({ regex, display: event.target.value })
            setCustomNsRegex(true)
        } catch (error) { }
    }

    const handleNameRegexChange = (event: ChangeEvent<HTMLInputElement>) => {
        try {
            const regex = new RegExp("^" + event.target.value)
            setNameRegex({ regex, display: event.target.value })
        } catch (error) { }
    }

    const [specs, setSpecs] = useState<{
        specs: Array<string>,
        sortedByName: Map<string, string[]>,
        sortedByNs: Map<string, string[]>
    }>({ specs: [], sortedByName: new Map(), sortedByNs: new Map() })

    useEffect(() => {
        (async () => {
            repl.allSpecs().then((data) => {
                const sortedByName = new Map<string, string[]>()
                const sortedByNs = new Map<string, string[]>()
                let ns, name: string
                for (let i = 0; i < data.length; i++) {
                    if (typeof data[i] !== "string") {
                        (data[i] = (data[i] as Symbol).sym)
                    }
                    const matchArray = (data[i] as string).match(/^(?<ns>.*)\/(?<name>.*)$/)
                    if (matchArray && matchArray.groups) {
                        ns = matchArray.groups.ns
                        name = matchArray.groups.name
                        if (sortedByName.has(name)) {
                            sortedByName.get(name)!.push(ns)
                        } else {
                            sortedByName.set(name, [ns])
                        }
                        if (sortedByNs.has(ns)) {
                            sortedByNs.get(ns)!.push(name)
                        } else {
                            sortedByNs.set(ns, [name])
                        }
                    }
                }
                data.sort()
                setSpecs({ specs: data as Array<string>, sortedByName: sortedByName, sortedByNs: sortedByNs })
            })
        })()
    }, [repl])

    return (
        <div>
            <label>
                <input
                    type="checkbox"
                    checked={sortByNs}
                    onChange={(_) => setSortByNs(!sortByNs)}
                />
                Group By Namespace
            </label>
            <form >
                <label>
                    ns pattern:
                    <input type="text" value={nsRegex.display} onChange={handleNsRegexChange} />
                </label>
                <label>
                    spec pattern:
                    <input type="text" value={nameRegex.display} onChange={handleNameRegexChange} />
                </label>
            </form>

            {
                sortByNs ? filterEntries(specs.sortedByNs.entries(), nsRegex.regex, nameRegex.regex).map(([k,vals], i) => (
                    <div key={i}>
                    <h3>{k}</h3>
                    <div>
                        {vals.map((v, i) => <ul key={i}>{v}</ul>)}
                    </div>
                </div>
                )) : 
                filterEntries(specs.sortedByName.entries(), nameRegex.regex, nsRegex.regex).map(([k,vals], i) => (
                    <div key={i}>
                    <h3>{k}</h3>
                    <div>
                        {vals.map((v, i) => <ul key={i}>{v}</ul>)}
                    </div>
                </div>
                ))
            }
        </div>
    )
}

export default Specs
