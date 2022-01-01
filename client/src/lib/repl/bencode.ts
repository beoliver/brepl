

const encodeString = (s: string) => [s.length, ':', s].join('')

const encodeInteger = (i: number) => ['i', i, 'e'].join('')

const encodeList = (xs: any[]) => {
    const encoded = xs.map(x => encode(x)).join('')
    return ['l', encoded, 'e'].join('')
}

const encodeDictionary = (x: { [key: string]: any }) => {
    const sortedEntries = Array.from(Object.entries(x)).sort(([a, _a], [b, _b]) => a.localeCompare(b))
    const encoded = sortedEntries.map(([k, v]) => encode(k) + encode(v)).join('')
    return ['d', encoded, 'e'].join('')
}

export const encode = (data: any): string => {
    if (typeof data === 'string') {
        return encodeString(data);
    }
    if (typeof data === 'number') {
        return encodeInteger(Math.floor(data));
    }
    if ('[object Array]' === {}.toString.call(data)) {
        return encodeList(data);
    }
    return encodeDictionary(data);
};

export type BencodeDecoded = string | number | BencodeDict | BencodeList
export type BencodeList = Array<BencodeDecoded>
export type BencodeDict = { [key: string]: BencodeDecoded }

interface BencodeParseResult<T> {
    val: T
    rem: string
}


const parseInteger = (bencode: string): BencodeParseResult<number> => {
    const end = bencode.indexOf("e")
    return {
        val: Number(bencode.slice(1, end)),
        rem: bencode.slice(end + 1)
    }
}

const parseString = (bencode: string): BencodeParseResult<string> => {
    const colonIndex = bencode.indexOf(":")
    const strLen = Number(bencode.slice(0, colonIndex))
    const strStart = colonIndex + 1
    const strEnd = strStart + strLen
    return {
        val: bencode.slice(strStart, strEnd),
        rem: bencode.slice(strEnd)
    }
}

const parseList = (bencode: string): BencodeParseResult<BencodeList> => {
    const result: BencodeList = []
    bencode = bencode.slice(1)
    let x = 0
    while (true) {
        x++
        if (x > 1000) {
            throw new Error("could not parse bencode list")
        }
        if (bencode.charAt(0) === "e") {
            return { val: result, rem: bencode.slice(1) }
        } else {
            const { val, rem } = parse(bencode)
            result.push(val)
            bencode = rem
        }
    }
}

const parseDict = (bencode: string): BencodeParseResult<BencodeDict> => {
    const result: BencodeDict = {}
    bencode = bencode.slice(1)
    let x = 0
    while (true) {
        x++
        if (x > 10000) {
            throw new Error("could not parse bencode dict")
        }
        if (bencode.charAt(0) === "e") {
            return { val: result, rem: bencode.slice(1) }
        } else {
            const parsedKey = parse(bencode)
            const parsedVal = parse(parsedKey.rem)
            result[parsedKey.val as string] = parsedVal.val
            bencode = parsedVal.rem
        }
    }
}


const parse = (bencode: string): BencodeParseResult<BencodeDecoded> => {
    switch (bencode.charAt(0)) {
        case "i": { return parseInteger(bencode) }
        case "d": { return parseDict(bencode) }
        case "l": { return parseList(bencode) }
        default: { return parseString(bencode) }
    }
}

export const decode = (bencode: string) => parse(bencode).val    
