import React, { useCallback, useEffect, useState } from "react"
import { Repl, Meta } from "../lib/repl/repl"
import styled from "styled-components";

interface Props { repl: Repl, ns?: string }

const Container = styled.div<{ depricated?: string, private?: boolean }>`
    background-color: ${(props) => props.depricated ? "red" : (props.private ? "#1A2421" : "#fbfbfb")};
    color: ${(props) => props.depricated ? "black" : (props.private ? "white" : "black")};
    padding: 1em 1em;
    overflow-x: scroll;
    font-size: 0.9em;
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

const extractHeadPosition = (s: string) => {
    const result = s.match(/^\((?<head>[^\s]+)/)
    if (result && result.groups) {
        return result.groups.head
    }
}

const NamespaceIntern: React.FunctionComponent<NamespaceInternProps> = ({ repl, ns, meta }) => {
    const [source, setSource] = useState<{ source: string, head: string }>()
    const [showSource, setShowSource] = useState<boolean>(false)
    const [dispatches, setDispatches] = useState<[string, string][]>([])
    const handleFetchSource = useCallback(() => {
        (async () => {
            setShowSource(true)
            if (!source) {
                try {
                    const source = await repl.sourceFor(ns!, meta.name.sym)
                    const head = extractHeadPosition(source)
                    setSource({ source: source, head: head || "" })
                } catch (err) { }
            }
        })()
    }, [repl, ns, meta, setShowSource])
    useEffect(() => {
        if (source && source.head) {
            switch (source.head) {
                case "defmulti": {
                    (async () => {
                        const dispatches = await repl.multiMethodDispatch(ns!, meta.name.sym)
                        for (let i = 0; i < dispatches.length; i++) {
                            const [dispatchOn, dispatchFnName] = dispatches[i]
                            const dispatchNs = dispatchFnName.match(/^(?<ns>[^\$]+)/);
                            if (dispatchNs && dispatchNs.groups) {
                                dispatches[i] = [dispatchOn, dispatchNs.groups.ns.replaceAll("_", "-")]
                            }
                        }
                        setDispatches(dispatches)
                    })()
                    break;
                };
                default: {
                }
            }
        }
    }, [source])
    return (
        <Container key={meta.name.sym} depricated={meta.deprecated} private={meta.private}>
            <Name>
                {meta.name.sym}
            </Name>
            {meta.protocol ? <p>{meta.protocol.tag}</p> : <p></p>}
            <p style={{ fontSize: '0.9em' }}>{meta.file}</p>
            {meta.private ? <PrivateBar /> : <PublicBar />}
            {meta.file ? <section>
                {
                    showSource ?
                        (
                            <div>
                                <button onClick={(_) => setShowSource(false)}>Hide</button>
                                <pre>{source?.head}</pre>
                                <pre>{source?.source}</pre>
                                {dispatches.map(([k, v], i) => (
                                    <div key={i}>
                                        <span style={{fontFamily: "Roboto"}}><code>{k}</code> implemented in <code>{v}</code></span></div>
                                ))}
                            </div>
                        )
                        :
                        <button onClick={(_) => handleFetchSource()}>Source</button>
                }
            </section> : <div></div>}
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
    const [nsDoc, setNsDoc] = useState<string>()
    const [showPrivate, setShowPrivate] = useState(true)

    useEffect(() => {
        if (ns) {
            (async () => {
                const nsSymbol = "'" + ns
                const interns = await repl.metaForNsInterns(nsSymbol);
                interns.sort((a, b) => (a.line || 0) - (b.line || 0))
                setInterns(interns)
            })();
            (async () => {
                const nsSymbol = "'" + ns
                const nsMeta = await repl.namespaceMeta(nsSymbol);
                setNsDoc(nsMeta ? nsMeta.doc : undefined)
            })();
        }
    }, [ns])

    return (
        <div>
            <h3>{ns}</h3>
            <pre>{nsDoc}</pre>
            <label>
                <input
                    type="checkbox"
                    checked={showPrivate}
                    onChange={(_) => setShowPrivate(!showPrivate)}
                />
                Include Private vars
            </label>
            <div>
                {interns.filter(x => !x.private || showPrivate).map((meta, i) =>
                    <NamespaceIntern key={`${ns}/${meta.name.sym}`} repl={repl} meta={meta} ns={ns} />
                )}
            </div>
        </div>

    )
}

export default NamespaceInterns
