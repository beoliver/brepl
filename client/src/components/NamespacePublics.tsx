import React, { useEffect, useState } from "react"
import { Repl, Meta } from "../lib/repl/repl"
import styled from "styled-components";

interface Props { repl: Repl, ns?: string }

const Container = styled.div<{ depricated?: string, private?: boolean }>`
    background-color: ${(props) => props.depricated ? "red" : (props.private ? "black" : "#fbfbfb") };
    color: ${(props) => props.depricated ? "black" : (props.private ? "white" : "black") };
    padding: 1em 1em;
    overflow-x: scroll;
`
const Name = styled.h3`
    margin-bottom: 0em;    
`
const PublicBar = styled.hr`
    color: blue;
`

const PrivateBar = styled.hr`
    color: greenyellow;
`

const NamespacePublics: React.FunctionComponent<Props> = ({ repl, ns }) => {
    const [interns, setInterns] = useState<Meta[]>([])
    useEffect(() => {
        if (ns) {
            (async () => {
                const nsSymbol = "'" + ns
                const interns = await repl.metaForNsInterns(nsSymbol);
                // interns.sort((a, b) => (a.name.sym.localeCompare(b.name.sym)))
                setInterns(interns)            
            })()
        }
    }, [ns])

    return (
        <div>
            <h1>{ns}</h1>
            <div>
                {interns.map((meta, i) =>
                    <Container key={i} depricated={meta.deprecated} private={meta.private}>
                        <Name>
                            {meta.name.sym}
                        </Name>
                        { meta.private ? <PrivateBar /> : <PublicBar /> }
                        <section>
                            <p key={i}><code >{meta.arglists}</code></p>
                        </section>
                        <section>
                            <p>{meta.doc}</p>
                        </section>
                    </Container>
                )}
            </div>
        </div>

    )
}

export default NamespacePublics
