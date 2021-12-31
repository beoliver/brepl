import React, { FormEvent, useCallback, useEffect, useState } from "react"
import { Prepl } from "../lib/repl/prepl"
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
    min-width: 5em;    
    padding: 1em 1em;
    width: 50%;
    resize: horizontal;
    // overflow-x: auto;
    overflow-y: scroll;
`
const CenterColumn = styled.div`
    background-color: white;
    font-family: 'JetBrains Mono';
    min-width: 5em;    
    resize: horizontal;
    overflow-y: scroll;    
`
const RightColumn = styled.div`
    background-color: pink;
    font-family: 'JetBrains Mono';
    min-width: 5em;
    max-width: 40em;
    overflow-x: auto;
    overflow-y: scroll;    
`

interface Props { }

export const Main: React.FunctionComponent<Props> = () => {
    const [proxyAddr, setProxyAddr] = useState<string>("8888")
    const [preplAddr, setPreplAddr] = useState<string>("8081")
    const [repl, setRepl] = useState<Repl | undefined>()
    const [ns, setNs] = useState<string>()

    const setInNs = useCallback((ns : string) => {
        (async () => {
            const nsStr = "'" + ns
            console.log(nsStr)
            await repl!.inNs(nsStr)
            setNs(ns)
        })()
    }, [repl, setNs])

    const connect = useCallback((event : FormEvent<HTMLFormElement>) => {
        event.preventDefault()
        if (proxyAddr && preplAddr) {
            (async () => {                
                const repl = new Repl(new Prepl({ port: proxyAddr }, { port: preplAddr }, ednParseOptions))
                console.log(repl)
                return repl.connect().then((_) => {
                    console.log(repl)
                    setRepl(repl)
                })
            })()
        }
    }, [repl, proxyAddr, preplAddr])

    if (repl !== undefined) {
        return (
            <MainContainerStyle>
                <LeftColumn>
                    <NamespaceTree {...{ repl, setNs : setInNs }} />
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
        return <div>
            <form onSubmit={(data) => {connect(data)}}>
                <label>
                    Proxy Port:
                    <input type="text" value={proxyAddr} onChange={(e) => { setProxyAddr(e.target.value) }} />
                </label>
                <label>
                    Prepl Port:
                    <input type="text" value={preplAddr} onChange={(e) => { setPreplAddr(e.target.value) }} />
                </label>
                <input type="submit" value="Connect" />
            </form>
        </div>
    }

}