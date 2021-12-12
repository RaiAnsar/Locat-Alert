package com.woxich.locatalert.model

/**
 * Groups properties of an input field, such as text, error, error message.
 */
data class InputField(
    var input: String = "",
    var isError: Boolean = false,
    var errorMessage: String = ""
)