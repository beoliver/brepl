import React, { ChangeEvent, useEffect, useState } from "react"
import { Repl, NsTreeValue, nsNameTree } from "../lib/repl/repl"

interface Props { repl: Repl }

const htmlTree = (tree: NsTreeValue, level: number): JSX.Element => {
    return (
        <div>
            {tree.ns ? <a href="">{tree.ns}</a> : <div></div>}
            {Object.entries(tree.children).map(([k, v]) => {
                if (Object.keys(v.children).length === 0) {
                    return (
                        <div {... { key: k, style: { background: `rgba(200,200,0,${0.1 * (level + 1)})`, marginLeft: `10px` } }}>
                            <a href="">{v.ns}</a>
                        </div>
                    )
                } else {
                    return (
                        <details {... { key: k, style: { background: `rgba(200,200,0,${0.1 * (level + 1)})`, marginLeft: `10px` } }}>
                            <summary>{k}</summary>
                            {htmlTree(v, level + 1)}
                        </details>
                    )
                }
            })}
        </div>
    )
}

const NamespaceTree: React.FunctionComponent<Props> = ({ repl }) => {
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
            <div>
                {htmlTree(nsNameTree(namespaces.filter(ns => ns.match(filterRegex.regex))), 0)}
            </div>
            <button onClick={async (e : any) => {
                repl.allLoadedNamespaceNames().then((namespaces) => {                    
                    setNamespaces(namespaces)
                })
            }}>Reload Namespace List</button>
        </div>
    )
}

export default NamespaceTree
