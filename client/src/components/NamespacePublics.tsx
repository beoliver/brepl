import React, { useEffect, useState } from "react"
import { Repl, Meta } from "../lib/repl/repl"
import styled from "styled-components";
import { toEDNString } from "edn-data"

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

const printableArglists = (arglists: any[][]): string[] => {
    if (arglists) {
        // console.log(toEDNString(arglists))
        return arglists.map((xs) => `(${xs.map(x => x.sym).join(" ")})`)
    }
    return []
}

const NamespacePublics: React.FunctionComponent<Props> = ({ repl, ns }) => {
    const [interns, setInterns] = useState<Meta[]>([])
    useEffect(() => {
        if (ns) {
            (async () => {
                repl.metaForNsPublics("'" + ns).then((data) => {
                    data.sort((a, b) => (a.name.sym.localeCompare(b.name.sym)))
                    setInterns(data)
                })
            })()
        }
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
                        <p key={i}><code >{meta.arglists}</code></p>                        
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
