package com.emrul

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.*
import mu.KotlinLogging
import org.graalvm.polyglot.Context
import org.graalvm.polyglot.HostAccess
import kotlin.coroutines.CoroutineContext

/**
 * Created by emrul on 24/07/2021.
 * @author Emrul Islam <emrul@emrul.com>
 * Copyright 2014 Emrul Islam
 */

private val logger = KotlinLogging.logger {}

class JSFetchAPIProvider(
    val httpClient: HttpClient,
    val polyglotContext: Context,
    context: CoroutineContext
) : CoroutineScope, AutoCloseable {

    override val coroutineContext: CoroutineContext = context + SupervisorJob(context[Job]) + CoroutineName("polyglot-fetch-provider")
    val jsonParseFun = polyglotContext.eval("js", "JSON.parse")!!


    fun newResponseProxy(httpResponse: HttpResponse): Response = Response(this, httpResponse)

    @HostAccess.Export
    fun fetch(resource: String, init: Map<String, Any?>? = null): JSPromise {
        val requestHttpMethod = HttpMethod.parse((init?.get("method") as? String)?.uppercase() ?: HttpMethod.Get.value)
        val reqHeaders = HashMap(init?.get("headers") as? Map<String, String>)
        val reqBody = init?.get("body")
        // TODO: Ref here
        // https://developer.mozilla.org/en-US/docs/Web/API/WindowOrWorkerGlobalScope/fetch

        return SuspendableJSPromise(coroutineContext) {
            logger.info { "Fetching: $resource" }
            val rsp = httpClient.request<HttpResponse>(resource) {
                method = requestHttpMethod

                headers {
                    reqHeaders.entries.map { append(it.key, it.value) }
                }
                if (reqBody != null) body = reqBody
            }
            logger.info { "Fetched resource" }
            return@SuspendableJSPromise newResponseProxy(rsp)
        }

    }

    override fun close() {
        httpClient.close()
    }


    class Response(private val fetchProvider: JSFetchAPIProvider, private val httpResponse: HttpResponse) {

        @HostAccess.Export
        fun json(): JSPromise = SuspendableJSPromise(fetchProvider.coroutineContext) {
            val response = httpResponse.readText()
                logger.info { "Responding with JSON" }
            return@SuspendableJSPromise fetchProvider.jsonParseFun.execute(response)
        }

        @HostAccess.Export
        fun text(): JSPromise = SuspendableJSPromise(fetchProvider.coroutineContext, httpResponse::readText)
    }
}