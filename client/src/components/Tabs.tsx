import React, { useState } from 'react'
import styled from 'styled-components'
import Tab from './Tab'

interface Props { children: { props: any }[] }

const TabContainer = styled.div`
    background-color: #fafafa;
    display: flex;
`
const Wrapper = styled.div`
    width: 40em;
    min-width: 20em;    
    height: inherit;    
    height: 100vh;
`

const Content = styled.div`   
`
const Child = styled.div<{ active: boolean }>`
    display: ${({ active }) => active ? 'block' : 'none'};
    visibility: ${({ active }) => active ? 'visible' : 'hidden'};
`


const Tabs: React.FunctionComponent<Props> = ({ children }) => {

    const [active, setActive] = useState<string>(children[0].props.label)
    console.log(active)

    return (
        <Wrapper>
            <TabContainer>
                {children.map(({ props }) => <Tab {...{ label: props.label, active: active === props.label, onClick: () => setActive(props.label) }} />)}
            </TabContainer>
            <Content>
                {children.map((child, index) => {
                    return (
                        <Child active={child.props.label === active}>
                            {child}
                        </Child>
                    )
                })}
            </Content>
        </Wrapper>
    )
}

export default Tabs