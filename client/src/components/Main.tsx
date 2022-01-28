import React, { FormEvent, useCallback, useEffect, useState } from "react"
import { Repl } from "../lib/repl/repl"
import NamespaceTree from "./NamespaceTree";
import NamespacePublics from "./NamespaceInterns";
import styled from 'styled-components';
import { Connect } from "./Connect"
import Tabs from "./Tabs";
import Apropos from "./Apropos";
import { NamespaceQualified } from "../lib/repl/utils";
import { Viewer, ViewerAction } from "./Viewer";
import SymbolSearch from "./SymbolSearch";

const MainContainerStyle = styled.div`
    background-color: white;    
    display: flex;
    width: 100%;
    height: 100vh;
`

const RightColumn = styled.div`    
    font-family: 'JetBrains Mono';  
    margin:1em 1em;
    width: 100%;
    overflow-x: auto;    
    overflow-y: scroll;    
`

interface Props { }

export const Main: React.FunctionComponent<Props> = () => {
    const [repl, setRepl] = useState<Repl | undefined>()
    const [ns, setNs] = useState<string>()    
    const [action, setAction] = useState<ViewerAction>()

    if (repl) {
        return (
            <MainContainerStyle>
                <Tabs>
                    <NamespaceTree label={"tree"} {...{ repl, setNs }} />
                    <Apropos label={"apropos"} {...{ setAction, repl }} />                    
                </Tabs>
                <RightColumn>                    
                    <SymbolSearch label={"search"} {...{ setAction, repl, ns }} />
                </RightColumn>                
                <RightColumn>
                    <Viewer {...{ repl, action }} />
                    {/* <NamespacePublics {...{ repl, ns, symbol }} /> */}
                </RightColumn>
            </MainContainerStyle>
        )
    } else {
        return <Connect setRepl={setRepl} />
    }
}