import React, { useEffect, useState } from "react"
import { Repl } from "../lib/repl/repl"
import type { Symbol } from "../lib/repl/clojure";

interface Props { repl: Repl, ns?: string }

const Specs: React.FunctionComponent<Props> = ({ repl }) => {
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
    return <div>{Array.from(specs.mappings.entries()).map(([name, ns], i) =>
    (
        <div key={i}>
            <h3>{name}</h3>
            <div>
                {ns.map((x, i) => <ul key={i}>{x}</ul>)}
            </div>
        </div>
    ))}</div>
}

export default Specs
