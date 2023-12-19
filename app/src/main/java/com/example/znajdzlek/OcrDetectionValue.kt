package com.example.znajdzlek

class OcrDetectionValue {
    private val isPresent: Boolean
    private val value: String

    constructor(value: String) {
        isPresent = true
        this.value = value
    }

    private constructor() {
        isPresent = false
        value = ""
    }

    fun getValue(): String {
        check(isPresent) { "Trying to obtain OcrDetectionValue that is not present" }
        return value
    }

    companion object {
        fun empty(): OcrDetectionValue {
            return OcrDetectionValue()
        }
    }
}