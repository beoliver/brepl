import React, { useEffect, useState } from "react"
import { Repl, Meta } from "../lib/repl/repl"
import type { Symbol } from "../lib/repl/clojure"

interface Props { repl: Repl }

const colours = new Map([[0, "pink"], [1, "skyBlue"]])

const printableArglists = (arglists: Symbol[][]) : string[] => {
    if (arglists) {
        return arglists.map((xs) => xs.map(x => x.sym).join(" "))
    }
    return []
}

const NamespacePublics: React.FunctionComponent<Props> = ({ repl }) => {
    const [interns, setInterns] = useState<Meta[]>([])
    useEffect(() => {
        (async () => {
            repl.metaForNsPublics("'clojure.core").then((data) => {
                setInterns(data)
            })
        })()
    }, [repl])

    return (
        <div>
            {interns.map((meta, i) =>
                <div {...{ key: i, style: { background: colours.get(i % 2) } }}>
                    <h3>{meta.name.sym}</h3>
                    <code>{JSON.stringify(printableArglists(meta.arglists), null, 2)}</code>
                    <div>{meta.doc}</div>
                </div>
            )}
        </div>
    )
}

export default NamespacePublics
