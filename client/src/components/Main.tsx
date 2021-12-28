import React, { useEffect, useState } from "react"
import { Prepl } from "../lib/repl/prepl"
import { Repl, ednParseOptions, ProxyAddr, ReplAddr } from "../lib/repl/repl"
import NamespaceTree from "./NamespaceTree";
import NamespacePublics from "./NamespacePublics";
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
    min-width: 20em;
    max-width: 30em;
    width: 50%;
    overflow-x: auto;
    overflow-y: scroll;
`
const RightColumn = styled.div`
    background-color: white;
    font-family: 'JetBrains Mono';
    min-width: 20em;
    max-width: 40em;
    width: 50%;
    overflow-x: auto;
    overflow-y: scroll;    
`

interface Props { proxyAddr: ProxyAddr, replAddr: ReplAddr }

export const Main: React.FunctionComponent<Props> = ({ proxyAddr, replAddr }) => {
    const [repl, setRepl] = useState<Repl | undefined>()
    useEffect(() => {
        (async () => {
            switch (replAddr.type) {
                case "prepl": {
                    const repl = new Repl(new Prepl(proxyAddr, replAddr, ednParseOptions))
                    return repl.connect().then((_) => {
                        setRepl(repl)
                    })
                }
                default: new Error("bad repl type")
            }
        })()
    }, [])

    if (repl !== undefined) {
        return (
            <MainContainerStyle>
                <LeftColumn>
                    <NamespaceTree {...{ repl }} />
                </LeftColumn>
                <RightColumn>
                    <NamespacePublics {...{ repl }} />
                </RightColumn>
            </MainContainerStyle>
        )
    } else {
        return <></>
    }

}