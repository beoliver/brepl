import React, { useEffect, useState } from "react"
import { Repl, NsTreeValue } from "../lib/repl/repl"

interface Props { repl: Repl }

const colours = new Map(
    [
        [0, "purple"],
        [1, "red"],
        [2, "green"],
        [3, "blue"],
        [4, "orange"],
        [5, "yellow"]
    ]
)

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
    const [namespaceTree, setNamespaceTrees] = useState<NsTreeValue>({ children: {} })
    useEffect(() => {
        (async () => {
            repl.loadedNamespaceTree().then((data) => {
                setNamespaceTrees(data)
            })
        })()
    }, [repl])

    return (<div>
        <div>{htmlTree(namespaceTree, 0)}</div>
    </div>)
}

export default NamespaceTree
