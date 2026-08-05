package com.susankhya.kisab

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element

/**
 * Durable English/Nepali resource parity checks that run against repository
 * files by repository-relative path, so they also run in CI.
 */
class LocalizationParityTest {

    private val repositoryRoot: File by lazy {
        var dir = File(System.getProperty("user.dir") ?: ".")
        while (true) {
            if (dir.resolve("settings.gradle.kts").isFile && dir.resolve("app").isDirectory) {
                return@lazy dir
            }
            val parent = dir.parentFile ?: break
            dir = parent
        }
        dir
    }

    private val englishFile: File = File(repositoryRoot, "app/src/main/res/values/strings.xml")
    private val nepaliFile: File = File(repositoryRoot, "app/src/main/res/values-ne/strings.xml")

    private data class ResourceEntry(
        val name: String,
        val translatable: Boolean,
        val value: String,
        val pluralQuantity: String?
    )

    private fun parse(file: File): List<ResourceEntry> {
        assertTrue("Resource file must exist at ${file.path}", file.isFile)
        val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
        val entries = mutableListOf<ResourceEntry>()
        val childElements = { parent: Element ->
            (0 until parent.childNodes.length).mapNotNull { parent.childNodes.item(it) as? Element }
        }
        for (node in childElements(document.documentElement)) {
            when (node.tagName) {
                "string" -> entries += ResourceEntry(
                    name = node.getAttribute("name"),
                    translatable = node.getAttribute("translatable") != "false",
                    value = node.textContent,
                    pluralQuantity = null
                )
                "plurals" -> {
                    val name = node.getAttribute("name")
                    val translatable = node.getAttribute("translatable") != "false"
                    for (child in childElements(node)) {
                        entries += ResourceEntry(
                            name = name,
                            translatable = translatable,
                            value = child.textContent,
                            pluralQuantity = child.getAttribute("quantity")
                        )
                    }
                }
            }
        }
        return entries
    }

    private fun placeholdersOf(value: String): List<Pair<Int, Char>> =
        Regex("%(\\d+)\\$([a-zA-Z])").findAll(value)
            .map { it.groupValues[1].toInt() to it.groupValues[2].single() }
            .sortedBy { it.first }
            .toList()

    private val english by lazy { parse(englishFile) }
    private val nepali by lazy { parse(nepaliFile) }

    @Test
    fun bothResourceFilesAreParsable() {
        assertTrue(english.isNotEmpty())
        assertTrue(nepali.isNotEmpty())
    }

    @Test
    fun noDuplicateNamesWithinEachLocale() {
        fun duplicates(entries: List<ResourceEntry>): List<String> =
            entries.groupBy { it.name to (it.pluralQuantity ?: "") }
                .filterValues { it.size > 1 }
                .keys
                .map { "${it.first} (${it.second.ifEmpty { "string" }})" }
        val englishDuplicates = duplicates(english)
        val nepaliDuplicates = duplicates(nepali)
        assertTrue("Duplicate English names: $englishDuplicates", englishDuplicates.isEmpty())
        assertTrue("Duplicate Nepali names: $nepaliDuplicates", nepaliDuplicates.isEmpty())
    }

    @Test
    fun noValuesNpDirectoryExists() {
        assertFalse(File(repositoryRoot, "app/src/main/res/values-np").exists())
    }

    @Test
    fun everyNepaliKeyExistsInEnglish() {
        for (entry in nepali) {
            assertTrue(
                "Nepali key ${entry.name} (${entry.pluralQuantity ?: "string"}) missing from English",
                english.any { it.name == entry.name && (it.pluralQuantity ?: "") == (entry.pluralQuantity ?: "") }
            )
        }
    }

    @Test
    fun everyTranslatableEnglishKeyHasNepaliCounterpart() {
        for (entry in english) {
            if (!entry.translatable) continue
            assertTrue(
                "Translatable English key ${entry.name} (${entry.pluralQuantity ?: "string"}) missing from Nepali",
                nepali.any { it.name == entry.name && (it.pluralQuantity ?: "") == (entry.pluralQuantity ?: "") }
            )
        }
    }

    @Test
    fun noEmptyValues() {
        for (entry in english + nepali) {
            assertTrue("Empty value for ${entry.name}", entry.value.isNotBlank())
        }
    }

    @Test
    fun placeholderSignaturesMatchAcrossLocales() {
        val englishByKey = english.groupBy { it.name to (it.pluralQuantity ?: "") }
        val nepaliByKey = nepali.groupBy { it.name to (it.pluralQuantity ?: "") }
        for ((key, englishEntries) in englishByKey) {
            val nepaliEntries = nepaliByKey[key] ?: continue
            val englishSig = placeholdersOf(englishEntries.single().value)
            val nepaliSig = placeholdersOf(nepaliEntries.single().value)
            assertEquals("Placeholder signature mismatch for ${key.first}", englishSig, nepaliSig)
        }
    }

    @Test
    fun pluralQuantitiesMatchAcrossLocales() {
        val englishPlurals = english.filter { it.pluralQuantity != null }
            .groupBy { it.name }
            .mapValues { it.value.map { entry -> entry.pluralQuantity }.toSet() }
        val nepaliPlurals = nepali.filter { it.pluralQuantity != null }
            .groupBy { it.name }
            .mapValues { it.value.map { entry -> entry.pluralQuantity }.toSet() }
        for ((name, quantities) in englishPlurals) {
            assertEquals("Plural quantities differ for $name", quantities, nepaliPlurals[name])
        }
    }

    @Test
    fun nonTranslatableEnglishKeysAreExplicitlyExcludedFromNepali() {
        val excluded = english.filter { !it.translatable }.map { it.name }
        assertTrue("Expected at least one non-translatable key", excluded.isNotEmpty())
        for (name in excluded) {
            assertTrue(
                "Non-translatable key $name should not require a Nepali counterpart",
                nepali.none { it.name == name }
            )
        }
    }
}
