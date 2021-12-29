import React, { useEffect, useState } from "react"
import { Repl, Meta } from "../lib/repl/repl"
import type { Symbol } from "../lib/repl/clojure"
import styled from "styled-components";

interface Props { repl: Repl, ns?: string }

const Container = styled.div<{ depricated?: string }>`
    background-color: ${(props) => props.depricated ? "red" : "#fbfbfb"};
    padding: 1em 1em;
`
const Name = styled.h3`
    margin-bottom: 0em;    
`
const BlueBar = styled.hr`
    color: blue;
`

const printableArglists = (arglists: Symbol[][]): string[] => {
    if (arglists) {
        return arglists.map((xs) => `(${xs.map(x => x.sym).join(" ")})`)
    }
    return []
}

const NamespacePublics: React.FunctionComponent<Props> = ({ repl, ns }) => {
    const [interns, setInterns] = useState<Meta[]>([])
    useEffect(() => {
        (async () => {
            repl.metaForNsPublics("'" + ns).then((data) => {
                data.sort((a, b) => (a.name.sym.localeCompare(b.name.sym)))
                setInterns(data)
            })
        })()
    }, [ns])

    return (
        <div>
            {interns.map((meta, i) =>
                <Container key={i} depricated={meta.deprecated}>
                    <Name>
                        {meta.name.sym}
                    </Name>
                    <BlueBar />
                    <section>
                        {printableArglists(meta.arglists).map((x, i) => {
                            return (<p key={i}><code >{x}</code></p>)
                        })}
                    </section>
                    <section>
                        <p>{meta.doc}</p>
                    </section>
                </Container>
            )}
        </div>
    )
}

export default NamespacePublics
