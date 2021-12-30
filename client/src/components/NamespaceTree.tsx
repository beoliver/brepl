import React, { ChangeEvent, useEffect, useState } from "react"
import { Repl, NsTreeValue, nsNameTree } from "../lib/repl/repl"

interface Props { repl: Repl, setNs: (ns: string) => void }

const htmlTree = (tree: NsTreeValue, level: number, setNs: (ns: string) => void): JSX.Element => {
    return (
        <div>
            {tree.ns ? <div><pre onClick={() => tree.ns ? setNs(tree.ns) : null}>{tree.ns}</pre></div> : <div></div>}
            {Object.entries(tree.children).map(([k, v]) => {
                if (Object.keys(v.children).length === 0) {
                    return (
                        <div {... { key: k, style: { background: `rgba(200,200,0,${0.1 * (level + 1)})`, marginLeft: `10px` } }}>
                            <div><pre onClick={() => v.ns ? setNs(v.ns) : null}>{v.ns}</pre></div>
                        </div>
                    )
                } else {
                    return (
                        <details {... { key: k, style: { background: `rgba(200,200,0,${0.1 * (level + 1)})`, marginLeft: `10px` } }}>
                            <summary>{k}</summary>
                            {htmlTree(v, level + 1, setNs)}
                        </details>
                    )
                }
            })}
        </div>
    )
}

const NamespaceTree: React.FunctionComponent<Props> = ({ repl, setNs }) => {
    const [filterRegex, setFilterRegex] = useState({ regex: new RegExp(""), display: "" })
    const [namespaces, setNamespaces] = useState<string[]>([])

    const handleChange = (event: ChangeEvent<HTMLInputElement>) => {
        try {
            const regex = new RegExp("^" + event.target.value)
            setFilterRegex({ regex, display: event.target.value })
        } catch (error) { }
    }

    useEffect(() => {
        (async () => {
            repl.allLoadedNamespaceNames().then((namespaces) => {
                namespaces.sort()
                setNamespaces(namespaces)
            })
        })()
    }, [repl])

    return (
        <div>
            <form >
                <label>
                    ns pattern:
                    <input type="text" value={filterRegex.display} onChange={handleChange} />
                </label>
            </form>
            <button onClick={async (e: any) => {
                repl.allLoadedNamespaceNames().then((namespaces) => {
                    namespaces.sort()
                    setNamespaces(namespaces)
                })
            }}>
                Reload Namespace List
            </button>
            <div>
                {htmlTree(nsNameTree(namespaces.filter(ns => ns.match(filterRegex.regex))), 0, setNs)}
            </div>
        </div>
    )
}

export default NamespaceTree
