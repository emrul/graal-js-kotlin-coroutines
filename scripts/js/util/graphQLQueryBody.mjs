import {compressGraphQLQuery} from "./compressGraphQLQuery.mjs";

export function graphQLQueryBody(query, variables) {
    return JSON.stringify({query: compressGraphQLQuery(query), variables});
}