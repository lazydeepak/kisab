package com.susankhya.kisab

import org.junit.Assert.assertNotNull
import org.junit.Test

class KisabSessionAppJvmTest {
    @Test
    fun appClassLoads() {
        assertNotNull(KisabSessionApp())
    }
}
