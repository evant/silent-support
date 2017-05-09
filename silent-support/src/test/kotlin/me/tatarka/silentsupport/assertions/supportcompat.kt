package me.tatarka.silentsupport.assertions

import me.tatarka.assertk.Assert
import me.tatarka.assertk.assert
import me.tatarka.assertk.assertions.isEqualTo
import me.tatarka.silentsupport.SupportCompat

fun Assert<SupportCompat>.hasVersion(version: String) {
    assert("version", actual.version).isEqualTo(version)
}

