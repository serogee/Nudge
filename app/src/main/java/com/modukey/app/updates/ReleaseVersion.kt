package com.modukey.app.updates

@ConsistentCopyVisibility
data class ReleaseVersion private constructor(private val numbers: List<Int>, private val prerelease: List<String>) : Comparable<ReleaseVersion> {
    val isPrerelease get() = prerelease.isNotEmpty()

    override fun compareTo(other: ReleaseVersion): Int {
        numbers.indices.forEach { index -> numbers[index].compareTo(other.numbers[index]).takeIf { it != 0 }?.let { return it } }
        if (prerelease.isEmpty()) return if (other.prerelease.isEmpty()) 0 else 1
        if (other.prerelease.isEmpty()) return -1
        for (index in 0 until minOf(prerelease.size, other.prerelease.size)) {
            val left = prerelease[index]
            val right = other.prerelease[index]
            val leftNumeric = left.all(Char::isDigit)
            val rightNumeric = right.all(Char::isDigit)
            val comparison = when {
                leftNumeric && rightNumeric -> left.length.compareTo(right.length).takeIf { it != 0 } ?: left.compareTo(right)
                leftNumeric -> -1
                rightNumeric -> 1
                else -> left.compareTo(right)
            }
            if (comparison != 0) return comparison
        }
        return prerelease.size.compareTo(other.prerelease.size)
    }

    companion object {
        private val pattern = Regex("""[vV]?(0|[1-9]\d*)\.(0|[1-9]\d*)\.(0|[1-9]\d*)(?:-([0-9A-Za-z-]+(?:\.[0-9A-Za-z-]+)*))?(?:\+[0-9A-Za-z-]+(?:\.[0-9A-Za-z-]+)*)?""")

        fun parse(value: String): ReleaseVersion? {
            val match = pattern.matchEntire(value) ?: return null
            val numbers = (1..3).map { match.groupValues[it].toIntOrNull() ?: return null }
            val prerelease = match.groupValues[4].takeIf { it.isNotEmpty() }?.split('.') ?: emptyList()
            if (prerelease.any { it.length > 1 && it.startsWith('0') && it.all(Char::isDigit) }) return null
            return ReleaseVersion(numbers, prerelease)
        }
    }
}
