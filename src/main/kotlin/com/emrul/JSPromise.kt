package com.emrul

import org.graalvm.polyglot.Value

/**
 * Created by emrul on 25/07/2021.
 * @author Emrul Islam <emrul@emrul.com>
 * Copyright 2021 Emrul Islam
 */

/**
 * An arbitrary "thenable" interface. Used to expose Java methods to JavaScript
 * Promise objects.
 */
fun interface JSPromise {
    fun then(onResolve: Value, onReject: Value)
}