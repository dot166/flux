package io.github.dot166.flux

import androidx.compose.runtime.Immutable

@Immutable
data class RefreshAwareState<T>(
    val refreshing: Boolean,
    val data: T,
    val listOfData: MutableMap<Int, T>,
)
