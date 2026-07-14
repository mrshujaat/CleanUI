package com.example.mediabrowser.data.remote

import com.example.mediabrowser.data.remote.dto.ArchivePostDto
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader

/**
 * The DAPI `s=post` endpoint returns two shapes depending on the site's
 * Gelbooru version:
 *  - 0.2.0 family (Rule34, Safebooru, Realbooru, Xbooru, TBIB, HypnoHub):
 *    a bare JSON array `[ {...}, {...} ]`
 *  - 0.2.5 (Gelbooru.com): an object wrapper `{ "@attributes": ..., "post": [...] }`
 *
 * Returns the post array in either case, or null if the response is neither
 * (e.g. an auth-error string).
 */
fun extractPostArray(element: JsonElement): JsonArray? = when (element) {
    is JsonArray -> element
    is JsonObject -> element["post"] as? JsonArray
    else -> null
}

private val leniencyJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
    coerceInputValues = true
}

/**
 * Parses one raw DAPI response body into posts, automatically choosing JSON
 * or XML. Some booru forks (Realbooru is a known offender) ignore `json=1`
 * and always return XML — for those we parse the `<post>` element attributes
 * directly. This makes the pager work uniformly across every site.
 */
fun parsePostResponse(body: String): List<ArchivePostDto> {
    val trimmed = body.trimStart()
    return if (trimmed.startsWith("<")) {
        parsePostsXml(trimmed)
    } else {
        val element = runCatching { Json.parseToJsonElement(trimmed) }.getOrNull() ?: return emptyList()
        val array = extractPostArray(element) ?: return emptyList()
        runCatching {
            leniencyJson.decodeFromString<List<ArchivePostDto>>(array.toString())
        }.getOrDefault(emptyList())
    }
}

/**
 * XML DAPI shape: `<posts><post id="..." file_url="..." .../></posts>`.
 * Every field we need is an attribute on `<post>`, so a single-pass pull
 * parser reads them all straight into DTOs.
 */
private fun parsePostsXml(xml: String): List<ArchivePostDto> {
    val posts = mutableListOf<ArchivePostDto>()
    return runCatching {
        val parser = XmlPullParserFactory.newInstance().newPullParser()
        parser.setInput(StringReader(xml))
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG && parser.name == "post") {
                val id = parser.getAttributeValue(null, "id")?.toLongOrNull() ?: 0L
                posts += ArchivePostDto(
                    id = id,
                    previewUrl = parser.getAttributeValue(null, "preview_url").orEmpty(),
                    fileUrl = parser.getAttributeValue(null, "file_url").orEmpty(),
                    sampleUrl = parser.getAttributeValue(null, "sample_url").orEmpty(),
                    width = parser.getAttributeValue(null, "width")?.toIntOrNull() ?: 0,
                    height = parser.getAttributeValue(null, "height")?.toIntOrNull() ?: 0,
                    score = parser.getAttributeValue(null, "score")?.toIntOrNull() ?: 0,
                    tags = parser.getAttributeValue(null, "tags").orEmpty(),
                    directory = parser.getAttributeValue(null, "directory")?.toIntOrNull(),
                    image = parser.getAttributeValue(null, "image")
                )
            }
            event = parser.next()
        }
        posts
    }.getOrDefault(emptyList())
}

