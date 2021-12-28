import React, { useEffect, useState } from "react"
import { Repl, Meta } from "../lib/repl/repl"
import type { Symbol } from "../lib/repl/clojure"
import styled from "styled-components";

interface Props { repl: Repl }

const Container = styled.div<{ depricated?: string }>`
    background-color: ${(props) => props.depricated ? "red" : "#fbfbfb"};
    padding: 1em 1em;
`
const Name = styled.h3`
    background-color: black;
    color: #fafafa;
`

const colours = new Map([[0, "#fbfbfb"], [1, "#fbfbfb"]])

const printableArglists = (arglists: Symbol[][]): string[] => {
    if (arglists) {
        return arglists.map((xs) => `(${xs.map(x => x.sym).join(" ")})`)
    }
    return []
}

const NamespacePublics: React.FunctionComponent<Props> = ({ repl }) => {
    const [interns, setInterns] = useState<Meta[]>([])
    useEffect(() => {
        (async () => {
            repl.metaForNsPublics("'clojure.core").then((data) => {
                data.sort((a, b) => (a.name.sym.localeCompare(b.name.sym)))
                setInterns(data)
            })
        })()
    }, [repl])

    return (
        <div>
            {interns.map((meta, i) =>
                <Container key={i} depricated={meta.deprecated}>
                    <Name>
                        {meta.name.sym}
                    </Name>
                    <hr />
                    <code>{JSON.stringify(printableArglists(meta.arglists), null, 2)}</code>
                    <section>
                        <p>{meta.doc}</p>
                    </section>
                </Container>
            )}
        </div>
    )
}

export default NamespacePublics
