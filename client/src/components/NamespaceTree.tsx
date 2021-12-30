import React, { ChangeEvent, useEffect, useState } from "react"
import styled from "styled-components"
import { Repl, NsTreeValue, nsNameTree } from "../lib/repl/repl"

interface Props { repl: Repl, setNs: (ns: string) => void }

const Container = styled.div`
    background-color: #fafafa;
`

const Controls = styled.div`
    margin-bottom: 1em;
`

const RegexInputField = styled.input`
    width: 100%; 
    box-sizing: border-box;
    margin-bottom: 0.5em;
`

const ReloadNamespaceListButton = styled.button`
    width: 100%; 
    padding: 0em 0em;
`

const TreeDetails = styled.details<{ level: number }>`
    margin-left: ${(props) => props.level === 0 ? '0px' : '10px'};    
    &:hover {
        background-color: ${(props) => `rgba(200,200,0,${0.1 * (props.level + 1)})`};
    }
`

const TreeLeaf = styled.div<{ level: number }>`
    font-size: 0.9em;
    overflow-x: hidden;
    margin-left: ${(props) => props.level === 0 ? '0px' : '10px'};    
    &:hover {
        background-color: red;
        cursor: pointer;
    }
`

const htmlTree = (tree: NsTreeValue, level: number, setNs: (ns: string) => void): JSX.Element => {
    return (
        <div>
            {tree.ns ? <TreeLeaf level={level} onClick={() => tree.ns ? setNs(tree.ns) : null}>
                <code>{tree.ns}</code></TreeLeaf> : <div></div>}
            {Object.entries(tree.children).map(([k, v]) => {
                if (Object.keys(v.children).length === 0) {
                    return (
                        <TreeLeaf key={k} level={level} onClick={() => v.ns ? setNs(v.ns) : null}>
                            <code>{v.ns}</code>
                        </TreeLeaf>
                    )
                } else {
                    return (
                        <TreeDetails key={k} level={level}>
                            <summary>{k}</summary>
                            {htmlTree(v, level + 1, setNs)}
                        </TreeDetails>
                    )
                }
            })}
        </div>
    )
}

const NamespaceTree: React.FunctionComponent<Props> = ({ repl, setNs }) => {
    const [filterRegex, setFilterRegex] = useState({ regex: new RegExp(""), display: "" })
    const [namespaces, setNamespaces] = useState<string[]>([])

    const handleChange = (event: ChangeEvent<HTMLInputElement>) => {
        try {
            const regex = new RegExp("^" + event.target.value)
            setFilterRegex({ regex, display: event.target.value })
        } catch (error) { }
    }

    useEffect(() => {
        (async () => {
            repl.allLoadedNamespaceNames().then((namespaces) => {
                namespaces.sort()
                setNamespaces(namespaces)
            })
        })()
    }, [repl])

    return (
        <Container>
            <Controls>
                <RegexInputField
                    placeholder="namespace regex"
                    type="text"
                    value={filterRegex.display}
                    onChange={handleChange} />
                <ReloadNamespaceListButton
                    onClick={async (e: any) => {
                        repl.allLoadedNamespaceNames().then((namespaces) => {
                            namespaces.sort()
                            setNamespaces(namespaces)
                        })
                    }}>
                    Reload Namespace List
                </ReloadNamespaceListButton>
            </Controls>
            {htmlTree(nsNameTree(namespaces.filter(ns => ns.match(filterRegex.regex))), 0, setNs)}
        </Container>
    )
}

export default NamespaceTree
