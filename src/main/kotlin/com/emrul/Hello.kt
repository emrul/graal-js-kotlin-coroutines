package com.emrul

import kotlinx.coroutines.*
import kotlinx.coroutines.future.await
import kotlinx.coroutines.slf4j.MDCContext
import mu.KotlinLogging
import org.graalvm.polyglot.*
import org.graalvm.polyglot.io.FileSystem
import org.slf4j.MDC
import java.io.IOException
import java.net.URI
import java.nio.channels.SeekableByteChannel
import java.nio.file.*
import java.nio.file.attribute.FileAttribute
import java.util.concurrent.CompletableFuture
import java.util.concurrent.locks.ReentrantLock


private val logger = KotlinLogging.logger {}

fun <A1, A2, R> ((A1, A2) -> R).toJava(): java.util.function.BiFunction<A1, A2, R> = java.util.function.BiFunction { a1, a2 -> this(a1, a2) }
fun <A1, R> ((A1) -> R).toJava(): java.util.function.Function<A1, R> = java.util.function.Function { a1 -> this(a1) }

fun main(args: Array<String>) {
    logger.info("Hello, World")

    val mainSource = Source
        .newBuilder("js", """import {fetchCountries} from 'scripts/js/fetchCountries.mjs'; fetchCountries""", "entrypoint.mjs")
        .build()

    val continents = setOf("AF", "AN", "AS", "EU", "NA", "OC", "SA")
    try {

        val l = mutableListOf<Deferred<Unit>>()
        runBlocking {
            suspendingPolyglotExecutor().use { polyglot ->
                for ( continent in continents) {
                    MDC.put("coroutine_mdc", continent)
                    logger.info { "starting continent" }
                    l += async(MDCContext()) {
                        logger.info { "started continent JS" }
                        val res = polyglot.go("js", mainSource, emptyMap(), continent)
                        logger.info { "completed continent JS" }
                    }
                }
                MDC.remove("coroutine_mdc")
                l.awaitAll()
            }
            logger.info { "Cleaned up" }

        }
        logger.info("Finished")
    } catch (e: Exception) {
        logger.error { e }
    }
}