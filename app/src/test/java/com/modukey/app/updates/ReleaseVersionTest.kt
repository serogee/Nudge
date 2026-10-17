package com.modukey.app.updates

import org.junit.Assert.*
import org.junit.Test

class ReleaseVersionTest {
    private fun version(value: String) = requireNotNull(ReleaseVersion.parse(value))

    @Test fun `version ordering is numeric and build metadata does not create an update`() {
        assertTrue(version("v0.1.10") > version("0.1.9"))
        assertTrue(version("1.0.0") > version("0.99.99"))
        assertEquals(version("v0.1.1"), version("0.1.1+build.9"))
        assertEquals(0, version("0.1.1").compareTo(version("V0.1.1")))
    }

    @Test fun `stable releases follow prereleases and numeric prerelease identifiers compare numerically`() {
        val versions = listOf("1.0.0-alpha", "1.0.0-alpha.1", "1.0.0-beta", "1.0.0-rc.2", "1.0.0-rc.10", "1.0.0")
        versions.zipWithNext().forEach { (older, newer) -> assertTrue("$newer must follow $older", version(newer) > version(older)) }
        assertTrue(version("1.0.0-rc.999999999999999999999") > version("1.0.0-rc.10"))
    }

    @Test fun `unsupported malformed and overflowing tags cannot be compared as valid releases`() {
        listOf("latest", "0.1", "0.1.1.2", "00.1.1", "0.1.1-01", "0.1.1-", "0.1.1/evil", "999999999999.1.1", " v0.1.1").forEach {
            assertNull(it, ReleaseVersion.parse(it))
        }
    }
}
