import React, { FormEvent, useCallback, useEffect, useState } from "react"
import { Prepl } from "../lib/repl/prepl"
import { Nrepl } from "../lib/repl/nrepl"
import { Repl, ednParseOptions, ProxyAddr, ReplAddr } from "../lib/repl/repl"
import NamespaceTree from "./NamespaceTree";
import NamespacePublics from "./NamespaceInterns";
import Specs from "./Specs";
import styled from 'styled-components';

const MainContainerStyle = styled.div`
    background-color: white;    
    display: flex;
    width: 100%;
    height: 100vh;
`
const LeftColumn = styled.div`
    font-family: 'Roboto';
    background-color: white;
    // min-width: 5em;    
    padding: 1em 1em;
    width: 50%;
    resize: horizontal;
    // overflow-x: auto;
    overflow-y: scroll;
`
const CenterColumn = styled.div`
    background-color: white;
    font-family: 'JetBrains Mono';
    // min-width: 5em;    
    resize: horizontal;
    overflow-y: scroll;    
`
const RightColumn = styled.div`
    background-color: pink;
    font-family: 'JetBrains Mono';
    /* min-width: 5em;
    max-width: 40em; */
    overflow-x: auto;
    overflow-y: scroll;    
`

interface Props { }

export const Main: React.FunctionComponent<Props> = () => {
    const [proxyAddr, setProxyAddr] = useState<string>(window.location.port)
    const [replAddr, setReplAddr] = useState<string>("8081")
    const [replType, setReplType] = useState<string>("prepl")
    const [repl, setRepl] = useState<Repl | undefined>()
    const [ns, setNs] = useState<string>()

    const setInNs = useCallback((ns: string) => {
        (async () => {
            const nsStr = "'" + ns
            console.log(nsStr)
            await repl!.inNs(nsStr)
            setNs(ns)
        })()
    }, [repl, setNs])

    const connect = useCallback((event: FormEvent<HTMLFormElement>) => {
        event.preventDefault()
        if (proxyAddr && replAddr) {
            let repl: Repl
            switch (replType) {
                case "prepl": {
                    repl = new Repl(new Prepl({ port: proxyAddr }, { port: replAddr }, ednParseOptions))
                    break
                }
                case "nrepl": {
                    repl = new Repl(new Nrepl({ port: proxyAddr }, { port: replAddr }, ednParseOptions))
                    break
                }
                default: throw Error("bad repl type")
            }
            (async () => {
                await repl.connected
                setRepl(repl)
            })()
        }
    }, [repl, proxyAddr, replAddr])

    if (repl !== undefined) {
        return (
            <MainContainerStyle>
                <LeftColumn>
                    <NamespaceTree {...{ repl, setNs: setInNs }} />
                </LeftColumn>
                <CenterColumn>
                    <NamespacePublics {...{ repl, ns }} />
                </CenterColumn>
                <RightColumn>
                    <Specs {... { repl, ns }} />
                </RightColumn>
            </MainContainerStyle>
        )
    } else {
        return (
            <Connect>
                <FlexForm onSubmit={(data) => { connect(data) }}>
                    <div>
                        <span>ws://localhost:</span>
                        <input
                            size={5}
                            style={{ boxSizing: "border-box", fontFamily: "JetBrains Mono" }}
                            type="text"
                            value={proxyAddr}
                            onChange={(e) => { setProxyAddr(e.target.value) }} />
                        <span>/</span>
                        <select
                            id="repls"
                            defaultValue={replType}
                            onChange={(x) => { x.preventDefault(); setReplType(x.target.value) }}
                        >
                            <option value="prepl">pREPL</option>
                            <option value="nrepl"> nREPL</option>
                        </select>
                        <span>/localhost:</span>
                        <input
                            size={5}
                            style={{ boxSizing: "border-box", fontFamily: "JetBrains Mono" }}
                            type="text"
                            value={replAddr}
                            onChange={(e) => { setReplAddr(e.target.value) }} />
                    </div>
                    <input style={{ margin: "1em", padding: "0.5em 0.5em", fontFamily: "JetBrains Mono" }} type="submit" value="Connect" />
                </FlexForm>
            </Connect>
        )
    }
}

const Connect = styled.div`
    background-color: #fafafa;
    font-family: "Jetbrains Mono";
    display: flex;
    justify-content: center;
    align-items: center;
    height: 100vh
`
const FlexForm = styled.form`
    display: flex;
    flex-direction: column;
    justify-content: center;
    align-items: center;
    
`