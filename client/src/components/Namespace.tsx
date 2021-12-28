import React, { useEffect, useState } from "react"
import { Repl } from "../lib/repl/repl"

interface Props { repl: Repl }

const Namespace: React.FunctionComponent<Props> = ({ repl }) => {
    const [namespace, setNamespace] = useState<string>("")
    useEffect(() => {
        (async () => {
            repl.currentNamespace().then((data) => {
                setNamespace(data)
            })
        })()
    }, [repl])
    return <h1>{namespace}</h1>
}

export default Namespace
