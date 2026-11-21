package com.sabeel.steps.i18n

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element

/**
 * Guards the EN/AR contract without a new dependency (no Robolectric, no Android runtime): both
 * files are parsed as XML and compared for keys, placeholders and honesty.
 *
 * A translation may only be identical to its English source when the value is language-neutral or
 * when the key is in ALLOWED_IDENTICAL (the language picker labels and the export file name).
 */
class LocalizationParityTest {

    private data class Entry(val values: List<String>)

    /** Matches %1$s, %2$d and %d; the double-percent escape is not a placeholder. */
    private val placeholder = Regex("%(?:\\d+\\$)?[a-zA-Z]")

    @Test
    fun englishAndArabicDeclareTheSameKeys() {
        val en = parse(resourceFile("values"))
        val ar = parse(resourceFile("values-ar"))
        assertEquals("string keys differ between values and values-ar", en.keys.sorted(), ar.keys.sorted())
    }

    @Test
    fun noStringIsBlankOrPadded() {
        val problems = mutableListOf<String>()
        for (config in listOf("values", "values-ar")) {
            parse(resourceFile(config)).forEach { (key, entry) ->
                entry.values.forEachIndexed { index, value ->
                    val where = config + " " + key + "[" + index + "]"
                    if (value.isBlank()) problems += where + " is blank"
                    if (value != value.trim()) problems += where + " has leading/trailing whitespace"
                }
            }
        }
        assertTrue(problems.joinToString("\n"), problems.isEmpty())
    }

    @Test
    fun placeholdersMatchBetweenLanguages() {
        val en = parse(resourceFile("values"))
        val ar = parse(resourceFile("values-ar"))
        val problems = mutableListOf<String>()
        for ((key, enEntry) in en) {
            val arEntry = ar[key] ?: continue
            if (key.startsWith("plurals/")) {
                // Arabic declares more quantities (zero/one/two/few/many/other), so compare the
                // distinct placeholder tokens rather than the per-item sequences.
                val enTokens = enEntry.values.flatMap { tokens(it) }.distinct().sorted()
                val arTokens = arEntry.values.flatMap { tokens(it) }.distinct().sorted()
                if (enTokens != arTokens) {
                    problems += key + " uses " + enTokens + " in English but " + arTokens + " in Arabic"
                }
            } else {
                val enTokens = tokens(enEntry.values.single())
                val arTokens = tokens(arEntry.values.single())
                if (enTokens != arTokens) {
                    problems += key + " uses " + enTokens + " in English but " + arTokens + " in Arabic"
                }
            }
        }
        assertTrue(problems.joinToString("\n"), problems.isEmpty())
    }

    @Test
    fun identicalValuesAreLanguageNeutralOrAllowlisted() {
        val en = parse(resourceFile("values"))
        val ar = parse(resourceFile("values-ar"))
        val problems = mutableListOf<String>()
        for ((key, enEntry) in en) {
            if (key.startsWith("plurals/")) continue
            val arEntry = ar[key] ?: continue
            val enValue = enEntry.values.single()
            if (enValue != arEntry.values.single()) continue
            if (key in ALLOWED_IDENTICAL) continue
            // Placeholders are language-neutral: "%1$s %2$s" is prose-free even though it has letters.
            if (!proseOf(enValue).any { it.isLetter() }) continue
            problems += key + " is identical in both languages (" + enValue + ") but is not in ALLOWED_IDENTICAL"
        }
        assertTrue(problems.joinToString("\n"), problems.isEmpty())
    }

    private fun tokens(value: String): List<String> =
        placeholder.findAll(value).map { it.value }.toList()

    /** The value with format placeholders removed, so templates can be told apart from prose. */
    private fun proseOf(value: String): String = placeholder.replace(value, "")

    private fun parse(file: File): Map<String, Entry> {
        val factory = DocumentBuilderFactory.newInstance()
        // These are local, trusted files: refuse DOCTYPE/entities so the parser stays inert.
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
        val document = factory.newDocumentBuilder().parse(file)
        val entries = linkedMapOf<String, Entry>()
        val nodes = document.documentElement.childNodes
        for (index in 0 until nodes.length) {
            val element = nodes.item(index) as? Element ?: continue
            when (element.tagName) {
                "string" -> {
                    val key = "string/" + element.getAttribute("name")
                    if (entries.containsKey(key)) throw AssertionError("Duplicate key " + key + " in " + file.name)
                    entries[key] = Entry(listOf(element.textContent))
                }

                "plurals" -> {
                    val key = "plurals/" + element.getAttribute("name")
                    if (entries.containsKey(key)) throw AssertionError("Duplicate key " + key + " in " + file.name)
                    val items = element.getElementsByTagName("item")
                    val values = (0 until items.length).map { (items.item(it) as Element).textContent }
                    entries[key] = Entry(values)
                }
            }
        }
        if (entries.isEmpty()) throw AssertionError("No strings parsed from " + file.absolutePath)
        return entries
    }

    /** Unit tests run with the module (app/) as the working directory; walk up for safety. */
    private fun resourceFile(config: String): File {
        val relative = File("src/main/res", config + "/strings.xml")
        var directory: File? = File(System.getProperty("user.dir") ?: ".").absoluteFile
        while (directory != null) {
            val direct = File(directory, "src/main/res/" + config + "/strings.xml")
            if (direct.isFile) return direct
            val fromRoot = File(directory, "app/src/main/res/" + config + "/strings.xml")
            if (fromRoot.isFile) return fromRoot
            directory = directory.parentFile
        }
        throw AssertionError("Could not find " + relative.path + " from " + System.getProperty("user.dir"))
    }

    private companion object {
        /**
         * Legitimate reasons for an identical value: the language pickers always show both names,
         * and the export file name is a file, not prose.
         */
        val ALLOWED_IDENTICAL = setOf(
            "string/settings_export_file_name",
            "string/settings_language_ar",
            "string/settings_language_en",
            // The provider's brand name is the same Latin string in both languages.
            "string/settings_health_connect_title",
        )
    }
}
