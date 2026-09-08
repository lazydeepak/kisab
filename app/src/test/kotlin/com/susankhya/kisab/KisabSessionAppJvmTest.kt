package com.susankhya.kisab

import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * JVM-only smoke test for [KisabSessionApp].
 *
 * Confirms the class can be instantiated on the JVM without
 * requiring Android framework dependencies.
 */
class KisabSessionAppJvmTest {
    /** KisabSessionApp can be constructed on the JVM. */
    @Test
    fun appClassLoads() {
        assertNotNull(KisabSessionApp())
    }
}
