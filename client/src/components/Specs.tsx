import React, { ChangeEvent, useEffect, useState } from "react"
import { Repl } from "../lib/repl/repl"
import type { Symbol } from "../lib/repl/clojure";

interface Props { repl: Repl, ns?: string }


const filterEntries = (entries : IterableIterator<[string, string[]]>, nsRegex :RegExp , nameRegex : RegExp) => {
    const xs = Array.from(entries).map(([k,v]) => [k, v.filter(ns => ns.match(nsRegex))]) as [string, string[]][]
    return xs.filter(([name,v]) => name.match(nameRegex) && v.length > 0)    
}

const Specs: React.FunctionComponent<Props> = ({ repl, ns }) => {

    const [customNsRegex, setCustomNsRegex] = useState(false)
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

    const [specs, setSpecs] = useState<{ specs: Array<string>, mappings: Map<string, string[]> }>({ specs: [], mappings: new Map() })

    useEffect(() => {
        (async () => {
            repl.allSpecs().then((data) => {
                const mappings = new Map<string, string[]>()
                let ns, name: string
                for (let i = 0; i < data.length; i++) {
                    if (typeof data[i] !== "string") {
                        (data[i] = (data[i] as Symbol).sym)
                    }
                    const matchArray = (data[i] as string).match(/^(?<ns>.*)\/(?<name>.*)$/)
                    if (matchArray && matchArray.groups) {
                        ns = matchArray.groups.ns
                        name = matchArray.groups.name
                        if (mappings.has(name)) {
                            mappings.get(name)!.push(ns)
                        } else {
                            mappings.set(name, [ns])
                        }
                    }
                }
                data.sort()
                setSpecs({ specs: data as Array<string>, mappings: mappings })
            })
        })()
    }, [repl])

    return (
        <div>
            <form >
                <label>
                    ns pattern:
                    <input type="text" value={nsRegex.display} onChange={handleNsRegexChange} />
                </label>
                <label>
                    name pattern:
                    <input type="text" value={nameRegex.display} onChange={handleNameRegexChange} />
                </label>
            </form>
            {filterEntries(specs.mappings.entries(), nsRegex.regex, nameRegex.regex).map(([name, namespaces], i) =>
            (
                <div key={i}>
                    <h3>{name}</h3>
                    <div>
                        {namespaces.filter((ns) => ns.match(nsRegex.regex)).map((x, i) => <ul key={i}>{x}</ul>)}
                    </div>
                </div>
            ))}
        </div>
    )
}

export default Specs
