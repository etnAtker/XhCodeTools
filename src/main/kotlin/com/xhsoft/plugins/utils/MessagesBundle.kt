package com.xhsoft.plugins.utils

import com.intellij.DynamicBundle
import org.jetbrains.annotations.Nls
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.PropertyKey
import java.util.function.Supplier

@NonNls
private const val BUNDLE = "messages.MessagesBundle"

internal object MessagesBundle {
    private val INSTANCE = DynamicBundle(MessagesBundle::class.java, BUNDLE)

    fun message(
        key: @PropertyKey(resourceBundle = BUNDLE) String,
        vararg params: Any
    ): @Nls String {
        return INSTANCE.getMessage(key, *params)
    }

    fun lazyMessage(
        @PropertyKey(resourceBundle = BUNDLE) key: String,
        vararg params: Any
    ): Supplier<@Nls String> {
        return INSTANCE.getLazyMessage(key, *params)
    }
}