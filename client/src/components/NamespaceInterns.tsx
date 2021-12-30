import React, { useCallback, useEffect, useState } from "react"
import { Repl, Meta } from "../lib/repl/repl"
import styled from "styled-components";

interface Props { repl: Repl, ns?: string }

const Container = styled.div<{ depricated?: string, private?: boolean }>`
    background-color: ${(props) => props.depricated ? "red" : (props.private ? "black" : "#fbfbfb")};
    color: ${(props) => props.depricated ? "black" : (props.private ? "white" : "black")};
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
    color: white;
`

interface NamespaceInternProps {
    repl: Repl
    ns?: string
    meta: Meta
}

const NamespaceIntern: React.FunctionComponent<NamespaceInternProps> = ({ repl, ns, meta }) => {
    const [source, setSource] = useState<string>()
    const [showSource, setShowSource] = useState<boolean>(false)
    const handleFetchSource = useCallback(() => {
        (async () => {
            setShowSource(true)
            if (!source) {
                repl.sourceFor(ns!, meta.name.sym).then((code) => { setSource(code) })
            }
        })()
    }, [repl, ns, meta, setShowSource])
    return (
        <Container key={meta.name.sym} depricated={meta.deprecated} private={meta.private}>
            <Name>
                {meta.name.sym}
            </Name>
            {meta.private ? <PrivateBar /> : <PublicBar />}
            <section>
                {showSource ? <div><button onClick={(_) => setShowSource(false)}>Hide</button><pre>{source}</pre></div> : <button onClick={(_) => handleFetchSource()}>Source</button>}
            </section>
            <section>
                <p><code>{meta.arglists}</code></p>
            </section>
            <section>
                <p>{meta.doc}</p>
            </section>
        </Container>
    )
}

const NamespaceInterns: React.FunctionComponent<Props> = ({ repl, ns }) => {

    const [interns, setInterns] = useState<Meta[]>([])

    useEffect(() => {
        if (ns) {
            (async () => {
                const nsSymbol = "'" + ns
                const interns = await repl.metaForNsInterns(nsSymbol);
                interns.sort((a,b) => (a.line || 0) - (b.line || 0))
                setInterns(interns)
            })()
        }
    }, [ns])

    return (
        <div>
            <h1>{ns}</h1>
            <div>
                {interns.map((meta, i) =>
                    <NamespaceIntern key={`${ns}/${meta.name.sym}`} repl={repl} meta={meta} ns={ns} />
                )}
            </div>
        </div>

    )
}

export default NamespaceInterns
