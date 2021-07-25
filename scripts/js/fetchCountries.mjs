import {FetchCountriesByContinentQuery} from "./queries/FetchCountriesByContinentQuery.mjs";
import {graphQLQueryBody} from "./util/graphQLQueryBody.mjs";

const defaultRequestOptions = {method: "POST"};
const defaultRequestHeaders = {"Content-Type": "application/json", 'Accept': 'application/json'}

export async function fetchCountries(continent_code) {
    const url = "https://countries.trevorblades.com/";
    let res = await fetch(url, {
        ...defaultRequestOptions,
        headers: {
            ...defaultRequestHeaders
        },
        body: graphQLQueryBody(FetchCountriesByContinentQuery, {continent_code})
    });
    return await res.json();
}
