package com.emrul

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.logging.*
import kotlinx.coroutines.*
import kotlinx.coroutines.slf4j.MDCContext
import mu.KotlinLogging
import org.graalvm.polyglot.*
import org.graalvm.polyglot.io.FileSystem
import java.io.File
import java.io.IOException
import java.net.URI
import java.nio.channels.SeekableByteChannel
import java.nio.file.*
import java.nio.file.attribute.FileAttribute
import java.util.function.Consumer
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.suspendCoroutine


/**
 * Created by emrul on 25/07/2021.
 * @author Emrul Islam <emrul@emrul.com>
 * Copyright 2021 Emrul Islam
 */

private val logger = KotlinLogging.logger {}

fun CoroutineScope.suspendingPolyglotExecutor(): SuspendingPolyglotExecutor = SuspendingPolyglotExecutor(coroutineContext)

class SuspendingPolyglotExecutor(context: CoroutineContext) : CoroutineScope, AutoCloseable {
    val engine = Engine.create()

    private val threadLocal: ThreadLocal<Context> = ThreadLocal.withInitial {
        createNewContext(engine)
    }
    override val coroutineContext: CoroutineContext = context /*+ SupervisorJob(context[Job])*/ + CoroutineName("polyglot-server")

    override fun close() {
        coroutineContext.cancel(CancellationException("Closing engine"))
        engine.close(true)
    }


    fun getFileSystem(): FileSystem {

        val fileSystem: FileSystem = object : FileSystem {
            var fullIO = FileSystems.getDefault()
            override fun parsePath(uri: URI?): Path? {
                return fullIO.provider().getPath(uri)
            }

            override fun parsePath(path: String?): Path? {
                return fullIO.getPath(path)
            }


            @Throws(IOException::class)
            override fun checkAccess(path: Path?, modes: Set<AccessMode?>, vararg linkOptions: LinkOption?) {
                if (linkOptions.size > 0) {
                    throw UnsupportedOperationException(
                        "CheckAccess for this FileSystem is unsupported with non " +
                                "empty link options."
                    )
                }
                fullIO.provider().checkAccess(path, *modes.toTypedArray())
            }

            @Throws(IOException::class)
            override fun createDirectory(dir: Path?, vararg attrs: FileAttribute<*>?) {
                fullIO.provider().createDirectory(dir, *attrs)
            }

            @Throws(IOException::class)
            override fun delete(path: Path?) {
                fullIO.provider().delete(path)
            }

            @Throws(IOException::class)
            override fun newByteChannel(
                path: Path?, options: Set<OpenOption?>?,
                vararg attrs: FileAttribute<*>?
            ): SeekableByteChannel? {
                return fullIO.provider().newByteChannel(path, options, *attrs)
            }

            @Throws(IOException::class)
            override fun newDirectoryStream(dir: Path?, filter: DirectoryStream.Filter<in Path?>?): DirectoryStream<Path?>? {
                return fullIO.provider().newDirectoryStream(dir, filter)
            }

            override fun toAbsolutePath(path: Path): Path? {
                return path.toAbsolutePath()
            }

            @Throws(IOException::class)
            override fun toRealPath(path: Path, vararg linkOptions: LinkOption?): Path? {
                var realPath = path
                if (!Files.exists(path, *linkOptions) && !path.fileName.toString().endsWith(".mjs")) {
                    realPath = path.resolveSibling(path.fileName.toString() + ".mjs")
                }
                return realPath.toRealPath(*linkOptions)
            }

            @Throws(IOException::class)
            override fun readAttributes(path: Path?, attributes: String?, vararg options: LinkOption?): Map<String?, Any?>? {
                return fullIO.provider().readAttributes(path, attributes, *options)
            }
        }

        return fileSystem
    }

    val httpFetchApiClient = HttpClient(CIO) {

    }
    fun createNewContext(engine: Engine): Context {
        val ctx = Context
            .newBuilder()
            .engine(engine)
            .allowAllAccess(true)
            .fileSystem(getFileSystem())
            .build()

        val fetchApi = JSFetchAPIProvider(
            httpClient = httpFetchApiClient,
            polyglotContext = ctx,
            context = coroutineContext
        )
        ctx.getBindings("js").putMember("fetch", fetchApi::fetch.toJava())
        return ctx
    }

    suspend fun go(language: String = "js", source: Source, bindings: Map<String, Any>, vararg args: Any) : Any? {
        val ctx = threadLocal.get()


        val contextBindings = ctx.getBindings(language)
        bindings.forEach { (key, value) -> contextBindings.putMember(key, value) }

        try {
            val jsFn = ctx.eval(source)
            val res = jsFn.executeMaybePromise(*args)
            logger.info { "Returning: $res"}
            return res
        }
        catch (e: Exception) {
            logger.warn { e }
            throw e
        }
    }


    suspend fun Value.executeMaybePromise(vararg args: Any): Any = suspendCoroutine { cont ->
        try {
            val result = this.execute(*args)
            if (result.metaObject.metaSimpleName.equals("Promise")) {
                result
                    .invokeMember("then", Consumer<Any> { value ->
                        cont.resumeWith(Result.success(value))
                    })
                    .invokeMember("catch", Consumer<Throwable> { value ->
                        cont.resumeWith(Result.failure(value))
                    })
            } else {
                cont.resumeWith(Result.success(result))
            }
        } catch (e: Exception) {
            cont.resumeWith(Result.failure(e))
        }
    }
}
