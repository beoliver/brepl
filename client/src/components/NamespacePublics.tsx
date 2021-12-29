import React, { useEffect, useState } from "react"
import { Repl, Meta } from "../lib/repl/repl"
import styled from "styled-components";

interface Props { repl: Repl, ns?: string }

const Container = styled.div<{ depricated?: string }>`
    background-color: ${(props) => props.depricated ? "red" : "#fbfbfb"};
    padding: 1em 1em;
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
    const [meta, setMeta] = useState<{ public: Meta[], private: Meta[] }>({ public: [], private: [] })
    useEffect(() => {
        if (ns) {
            (async () => {
                const nsSymbol = "'" + ns

                const publics = await repl.metaForNsPublics(nsSymbol);
                publics.sort((a, b) => (a.name.sym.localeCompare(b.name.sym)))
                const publicNames = new Set(publics.map((x) => x.name.sym))

                const interns = await repl.metaForNsInterns(nsSymbol);
                const privates: Meta[] = []
                interns.forEach((x) => {
                    if (!publicNames.has(x.name.sym)) {
                        privates.push(x)
                    }
                })
                privates.sort((a, b) => (a.name.sym.localeCompare(b.name.sym)))

                setMeta({ public: publics, private: privates })

            })()
        }
    }, [ns])

    return (
        <div>
            <h1>{ns}</h1>
            <h2>
                Public
            </h2>
            <div>
                {meta.public.map((meta, i) =>
                    <Container key={i} depricated={meta.deprecated}>
                        <Name>
                            {meta.name.sym}
                        </Name>
                        <PublicBar />
                        <section>
                            <p key={i}><code >{meta.arglists}</code></p>
                        </section>
                        <section>
                            <p>{meta.doc}</p>
                        </section>
                    </Container>
                )}
            </div>
            <h2>
                Private
            </h2>
            <div>
                {meta.private.map((meta, i) =>
                    <Container key={i} depricated={meta.deprecated}>
                        <Name>
                            {meta.name.sym}
                        </Name>
                        <PrivateBar />
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
