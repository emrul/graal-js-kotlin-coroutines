
# What is this?
A weekend project where I wanted to try out a number of things.  In no particular order:
* Kotlin with JDK16 and latest GraalVM (21.2.0)
* Embedding GraalJS Engine and using it from Kotlin's coroutines.  In particular, I wanted to see if I could turn Promises returned from JavaScript into suspension points (turns out yes!)
* Since GraalJS doesn't implement the WebFetch API I wanted to see if I could Polyfill it from Kotlin using the Ktor Client (yes I can!)
* Latest (alpha) SLF4j API (2.0) - seems to work fine
* Mapped Diagnostic Context - just for the hell of throwing that in there somewhere
* Loading of ES6 modules (works great)
* Try out a quick and easy GraphQL API (**thanks to 'Trevor'** for this [API Endpoint](https://countries.trevorblades.com)

# License
Take your pick of - MIT / BSD / Apache2

# Next steps
- [ ] Try creating a dockerised native image
- [ ] Write up some Unit Tests (why not).
- [ ] Write up some docs (why not)
- [ ] Try some JMH Benchmarks

# Takeaways
* I'm not confident that I've used all the coroutines APIs correctly or in the most idiomatic way
* PRs with code improvements / suggestions are welcome
* This isn't going anywhere in particular but hoping I'll be able to use a few approaches in other projects
