export const FetchCountriesByContinentQuery = `query CountriesQuery($continent_code: String) {
  countries(filter: {continent:{eq:$continent_code}}) {
    name
  }
}`;