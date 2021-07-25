export function compressGraphQLQuery(query) {
    return query
        // replace multiple whitespace with a single
        .replace(/(\b|\B)\s+(\b|\B)/gm, ' ')
        // remove all whitespace between everything except for word and word boundaries
        .replace(/(\B)\s+(\B)|(\b)\s+(\B)|(\B)\s+(\b)/gm, '')
        .trim()
}