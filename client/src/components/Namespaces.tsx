import React, { useEffect, useState } from "react"
import { Repl } from "../lib/repl/repl"

interface Props { repl: Repl }

const Namespaces: React.FunctionComponent<Props> = ({ repl }) => {
    const [namespaces, setNamespaces] = useState<string[]>([])
    useEffect(() => {
        (async () => {
            repl.allLoadedNamespaceNames().then((data) => {
                setNamespaces(data)
            })
        })()
    }, [repl])
    return <div>{namespaces.map((ns) => <ul key={ns}>{ns}</ul>)}</div>
}

export default Namespaces
