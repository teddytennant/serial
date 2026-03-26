package com.teddytennant.serial.epub

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.TextNode
import java.io.File
import java.io.InputStream
import java.util.zip.ZipInputStream
import javax.xml.parsers.DocumentBuilderFactory

data class EpubBook(
    val title: String,
    val author: String,
    val chapters: List<EpubChapter>,
    val coverPath: String?
)

data class EpubChapter(
    val title: String,
    val words: List<String>
)

class EpubParser(private val context: Context) {

    fun parse(uri: Uri): EpubBook {
        val entries = readZipEntries(uri)
        val containerXml = entries["META-INF/container.xml"]
            ?: error("Invalid EPUB: missing container.xml")

        val opfPath = parseContainerXml(containerXml)
        val opfDir = opfPath.substringBeforeLast("/", "")
        val opfContent = entries[opfPath] ?: error("Invalid EPUB: missing OPF at $opfPath")

        val opf = parseOpf(opfContent, opfDir)
        val coverPath = extractCover(entries, opf, opfDir)

        val chapters = opf.spineItems.mapNotNull { href ->
            val content = entries[href] ?: return@mapNotNull null
            val htmlString = String(content)
            if (isFrontMatter(htmlString, href)) return@mapNotNull null
            val text = extractText(htmlString)
            val words = tokenize(text)
            if (words.size < 10) return@mapNotNull null // skip near-empty pages
            val title = extractChapterTitle(htmlString) ?: "Chapter"
            EpubChapter(title = title, words = words)
        }

        return EpubBook(
            title = opf.title,
            author = opf.author,
            chapters = chapters,
            coverPath = coverPath
        )
    }

    private fun isFrontMatter(html: String, href: String): Boolean {
        val hrefLower = href.lowercase()
        val frontMatterPaths = listOf(
            "cover", "title", "copyright", "rights", "toc", "contents",
            "dedication", "epigraph", "foreword", "preface", "acknowledgment",
            "halftitle", "frontmatter", "front_matter", "also-by", "also_by",
            "aboutauthor", "about-author", "about_author"
        )
        if (frontMatterPaths.any { hrefLower.contains(it) }) return true

        val doc = Jsoup.parse(html)
        val body = doc.body() ?: return true
        val bodyText = body.text().trim()

        // Skip pages with very little text (likely cover/title pages)
        if (bodyText.length < 50) return true

        // Check for common front matter class/id patterns
        val allElements = body.allElements
        for (el in allElements) {
            val classes = el.classNames().joinToString(" ").lowercase()
            val id = (el.id() ?: "").lowercase()
            val combined = "$classes $id"
            if (frontMatterPaths.any { combined.contains(it) }) {
                // Only skip if this element is a major container
                if (el.tagName() in listOf("body", "div", "section", "article")) return true
            }
        }

        // Check for copyright markers in short pages
        if (bodyText.length < 500) {
            val lower = bodyText.lowercase()
            if (lower.contains("all rights reserved") ||
                lower.contains("copyright ©") ||
                lower.contains("isbn") ||
                lower.contains("published by") ||
                lower.contains("table of contents")
            ) return true
        }

        return false
    }

    private fun readZipEntries(uri: Uri): Map<String, ByteArray> {
        val entries = mutableMapOf<String, ByteArray>()
        context.contentResolver.openInputStream(uri)?.use { stream ->
            ZipInputStream(stream).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory) {
                        entries[entry.name] = zip.readBytes()
                    }
                    entry = zip.nextEntry
                }
            }
        }
        return entries
    }

    private fun parseContainerXml(data: ByteArray): String {
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            .parse(data.inputStream())
        val rootfiles = doc.getElementsByTagName("rootfile")
        return rootfiles.item(0)?.attributes?.getNamedItem("full-path")?.nodeValue
            ?: error("Invalid container.xml")
    }

    private data class OpfData(
        val title: String,
        val author: String,
        val spineItems: List<String>,
        val manifest: Map<String, String>,
        val coverItemId: String?
    )

    private fun parseOpf(data: ByteArray, opfDir: String): OpfData {
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            .parse(data.inputStream())

        val title = doc.getElementsByTagName("dc:title").item(0)?.textContent ?: "Unknown"
        val author = doc.getElementsByTagName("dc:creator").item(0)?.textContent ?: "Unknown"

        // Build manifest: id -> href
        val manifestMap = mutableMapOf<String, String>()
        val manifestNodes = doc.getElementsByTagName("item")
        for (i in 0 until manifestNodes.length) {
            val node = manifestNodes.item(i)
            val id = node.attributes.getNamedItem("id")?.nodeValue ?: continue
            val href = node.attributes.getNamedItem("href")?.nodeValue ?: continue
            manifestMap[id] = if (opfDir.isNotEmpty()) "$opfDir/$href" else href
        }

        // Find cover item
        var coverItemId: String? = null
        val metaNodes = doc.getElementsByTagName("meta")
        for (i in 0 until metaNodes.length) {
            val node = metaNodes.item(i)
            if (node.attributes.getNamedItem("name")?.nodeValue == "cover") {
                coverItemId = node.attributes.getNamedItem("content")?.nodeValue
                break
            }
        }

        // Build spine order
        val spineItems = mutableListOf<String>()
        val spineNodes = doc.getElementsByTagName("itemref")
        for (i in 0 until spineNodes.length) {
            val node = spineNodes.item(i)
            val idref = node.attributes.getNamedItem("idref")?.nodeValue ?: continue
            val href = manifestMap[idref] ?: continue
            spineItems.add(href)
        }

        return OpfData(title, author, spineItems, manifestMap, coverItemId)
    }

    private fun extractCover(entries: Map<String, ByteArray>, opf: OpfData, opfDir: String): String? {
        val coverHref = opf.coverItemId?.let { opf.manifest[it] } ?: return null
        val coverData = entries[coverHref] ?: return null

        // Verify it's actually an image
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(coverData, 0, coverData.size, options)
        if (options.outWidth <= 0) return null

        val coverFile = File(context.cacheDir, "covers/${coverHref.hashCode()}.jpg")
        coverFile.parentFile?.mkdirs()
        coverFile.writeBytes(coverData)
        return coverFile.absolutePath
    }

    private fun extractText(html: String): String {
        val doc = Jsoup.parse(html)
        val body = doc.body() ?: return ""
        val sb = StringBuilder()
        extractTextRecursive(body, sb)
        return sb.toString().trim()
    }

    private val blockTags = setOf(
        "p", "div", "h1", "h2", "h3", "h4", "h5", "h6",
        "br", "li", "blockquote", "section", "article", "hr",
        "ol", "ul", "dl", "dt", "dd", "figure", "figcaption",
        "table", "tr", "pre"
    )

    private fun extractTextRecursive(element: Element, sb: StringBuilder) {
        for (node in element.childNodes()) {
            when (node) {
                is TextNode -> {
                    val text = node.getWholeText()
                    // Collapse internal whitespace but preserve word boundaries
                    val collapsed = text.replace(Regex("\\s+"), " ")
                    if (collapsed.isNotBlank()) {
                        sb.append(collapsed)
                    }
                }
                is Element -> {
                    val tag = node.tagName().lowercase()
                    if (tag in blockTags) {
                        if (sb.isNotEmpty() && !sb.endsWith('\n') && !sb.endsWith(' ')) {
                            sb.append('\n')
                        }
                    }
                    // Inline elements (span, em, i, b, strong, a, etc.) - no extra space
                    extractTextRecursive(node, sb)
                    if (tag in blockTags) {
                        if (sb.isNotEmpty() && !sb.endsWith('\n') && !sb.endsWith(' ')) {
                            sb.append('\n')
                        }
                    }
                }
            }
        }
    }

    private fun extractChapterTitle(html: String): String? {
        val doc = Jsoup.parse(html)
        // Try heading tags in order
        for (tag in listOf("h1", "h2", "h3", "title")) {
            val el = doc.selectFirst(tag)
            if (el != null) {
                val text = el.text().trim()
                if (text.isNotEmpty()) return text
            }
        }
        return null
    }

    private fun tokenize(text: String): List<String> {
        return text
            .replace(Regex("[\\u00AD\\u200B\\u200C\\u200D\\uFEFF]"), "") // strip soft hyphens & zero-width chars
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }
            .map { it.trim() }
    }
}
