import React from 'react'
import styled from 'styled-components'

interface Props {
    label: string
    onClick: () => void
    active: boolean
}

const whitesmoke = "#f5f5f5"
const anitflash = "#f2f3f4"
const antique = "#faebd7"
const azureish = "#dbe9f4"

const StyledTab = styled.div<{ active: boolean }>`
    margin-top: 0.5em;
    // margin-bottom: 1em;
    padding: 0.5em;
    border-radius: 0.5em 0.5em 0 0;
    background-color: ${(props) => props.active ? "inherit" : "#ecedee"};
    -webkit-box-shadow: 4px -4px 8px -4px #8c9696;  /* Safari 3-4, iOS 4.0.2 - 4.2, Android 2.3+ */
    -moz-box-shadow:    4px -4px 8px -4px #8c9696;  /* Firefox 3.5 - 3.6 */
    box-shadow:         4px -4px 8px -4px #8c9696;  /* Opera 10.5, IE 9, Firefox 4+, Chrome 6+, iOS 5 */
    &:hover {
        background-color: ${(props) => props.active ? antique : azureish};
        cursor: ${(props) => props.active ? "default" : "pointer"};
    }
`

const Tab: React.FunctionComponent<Props> = ({ label, onClick, active }) => {
    return <StyledTab onClick={() => onClick()} active={active}>{label}</StyledTab>
}

export default Tab