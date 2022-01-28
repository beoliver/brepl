import React, { useCallback, useState } from "react"
import styled from 'styled-components';
import { Nrepl } from "../lib/repl/nrepl";
import { Prepl } from "../lib/repl/prepl";
import { ednParseOptions, Repl } from "../lib/repl/repl";
import { ReplImpl } from "../lib/repl/types";

const ConnectStyle = styled.div`
    background-color: #fafafa;
    font-family: "Jetbrains Mono";
    display: flex;
    justify-content: center;
    align-items: center;
    height: 100vh;
`
const FlexForm = styled.form`
    display: flex;
    flex-direction: column;
    justify-content: center;
    align-items: center;    
`

const PortInput = styled.input`
    box-sizing: border-box; 
    font-family: "JetBrains Mono";
`

interface ConnectProps {
    setRepl: (repl: Repl) => void
}

export const Connect: React.FunctionComponent<ConnectProps> = ({ setRepl }) => {
    const [proxyAddr, setProxyAddr] = useState<string>(window.location.port)
    const [replAddr, setReplAddr] = useState<string>("8081")
    const [replType, setReplType] = useState<string>("prepl")

    const connect = useCallback((ev: React.FormEvent<HTMLFormElement>) => {
        ev.preventDefault();

        let impl: ReplImpl
        switch (replType) {
            case "prepl": {
                impl = new Prepl({ port: proxyAddr }, { port: replAddr }, ednParseOptions)
                break
            }
            case "nrepl": {
                impl = new Nrepl({ port: proxyAddr }, { port: replAddr }, ednParseOptions)
                break
            }
            default: throw Error("bad repl type")
        }
        (async () => {
            try {
                const repl = new Repl(impl)
                const connected = await repl.connect()
                if (connected) {
                    (window as any).repl = repl
                    setRepl(repl)
                } else {
                    console.log(impl)
                    console.warn("could not connect to REPL!")
                }
            } catch (err) {
                console.warn("ERROR", err)
            }
        })()
    }, [setRepl, proxyAddr, replAddr, replType])

    return (
        <ConnectStyle>
            <FlexForm onSubmit={connect}>
                <div>
                    <span>ws://localhost:</span>
                    <PortInput
                        size={5}
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
                    <PortInput
                        size={5}
                        type="text"
                        value={replAddr}
                        onChange={(e) => { setReplAddr(e.target.value) }} />
                </div>
                <input style={{ margin: "1em", padding: "0.5em 0.5em", fontFamily: "JetBrains Mono" }} type="submit" value="Connect" />
            </FlexForm>
        </ConnectStyle>
    )
}
