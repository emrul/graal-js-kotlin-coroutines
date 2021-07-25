package com.emrul

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.slf4j.MDCContext
import org.graalvm.polyglot.Value
import kotlin.coroutines.CoroutineContext

/**
 * Created by emrul on 25/07/2021.
 * @author Emrul Islam <emrul@emrul.com>
 * Copyright 2021 Emrul Islam
 *
 * Takes a suspendable function and wraps it in a JSPromise suitable for JavaScript
 * in GraalVM.
 */

class SuspendableJSPromise<T>(context: CoroutineContext, val invoke: suspend () -> T) :
    CoroutineScope, JSPromise {
    override val coroutineContext: CoroutineContext = context + CoroutineName("suspendable-promise")

    override fun then(onResolve: Value, onReject: Value) {
        launch(MDCContext()) {
            try {
                onResolve.executeVoid(invoke())
            } catch (e: Exception) {
                onReject.executeVoid(e)
            }
        }
    }
}