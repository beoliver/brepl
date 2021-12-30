import React, { ChangeEvent, useEffect, useState } from "react"
import { Repl } from "../lib/repl/repl"
import type { Symbol } from "../lib/repl/clojure";
import styled from "styled-components";

interface Props { repl: Repl, ns?: string }

interface SpecProps { repl: Repl, specNs: string, specName: string, display: string }

const Container = styled.div<{}>`    
    padding: 0em 0em;
    overflow-x: scroll;
`

const Details = styled.details<{}>`    
    padding: 0em 0em;
    margin: 0em 0em;    
`

const Spec: React.FunctionComponent<SpecProps> = ({ repl, specNs, specName, display }) => {
    const [description, setDescription] = useState<any>()
    const [example, setExample] = useState<string>()

    const handleExample = async () => {
        repl.specExample(`:${specNs}/${specName}`).then((example) => setExample(example))
    }

    return (
        <Container>
            <Details>
                <summary>{display}</summary>
                <section>
                    <button onClick={(_) => handleExample()}>EXAMPLE</button>
                    <pre>{example}</pre>
                </section>
            </Details>
        </Container>
    )
}


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
                sortByNs
                    ?
                    filterEntries(specs.sortedByNs.entries(), nsRegex.regex, nameRegex.regex).map(([k, vals], i) => (
                        <div key={k}>
                            <h3>{k}</h3>
                            <div>
                                {vals.map((v, i) => <Spec key={i} repl={repl} specNs={k} specName={v} display={v} />)}
                            </div>
                        </div>
                    ))
                    :
                    filterEntries(specs.sortedByName.entries(), nameRegex.regex, nsRegex.regex).map(([k, vals], i) => (
                        <div key={k}>
                            <h3>{k}</h3>
                            <div>
                                {vals.map((v, i) => <Spec key={i} repl={repl} specNs={v} specName={k} display={v} />)}
                            </div>
                        </div>
                    ))
            }
        </div>
    )
}

export default Specs
