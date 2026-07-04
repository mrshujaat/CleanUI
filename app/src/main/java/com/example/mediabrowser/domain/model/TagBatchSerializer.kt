package com.example.mediabrowser.domain.model

/**
 * Plain-text serialization for tag batches ("My Poison"), used by import/export
 * so users don't lose their batches when uninstalling.
 *
 * Format (one batch per block, blocks separated by a blank line):
 *
 *   # Batch Name
 *   tag_one tag_two tag_three
 *
 *   # Another Batch
 *   tag_four tag_five
 *
 * A line starting with '#' begins a new batch and names it; the following
 * non-empty line holds its space-separated tags. This is easy to read, easy to
 * hand-edit, and survives copy/paste into any text app.
 */
object TagBatchSerializer {

    fun export(batches: List<TagBatch>): String = buildString {
        batches.forEach { batch ->
            append("# ").append(batch.name.trim()).append('\n')
            append(batch.tags.joinToString(" ")).append('\n')
            append('\n')
        }
    }.trim() + "\n"

    /**
     * Parses the export format back into batches. Tolerant of extra blank lines
     * and Windows/Unix line endings. Timestamps are set to "now" on import since
     * the text format intentionally doesn't carry them.
     */
    fun import(text: String): List<TagBatch> {
        val now = System.currentTimeMillis()
        val result = mutableListOf<TagBatch>()
        var currentName: String? = null
        val currentTags = mutableListOf<String>()

        fun flush() {
            val name = currentName?.trim()
            if (!name.isNullOrEmpty()) {
                result += TagBatch(
                    id = 0,
                    name = name,
                    tags = currentTags.toList(),
                    createdAt = now,
                    updatedAt = now
                )
            }
            currentName = null
            currentTags.clear()
        }

        text.split('\n').forEach { rawLine ->
            val line = rawLine.trim()
            when {
                line.startsWith("#") -> {
                    // New batch begins — emit the previous one first.
                    flush()
                    currentName = line.removePrefix("#").trim()
                }
                line.isNotEmpty() && currentName != null -> {
                    currentTags += line.split(" ", "\t", ",")
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }
                }
            }
        }
        flush()
        return result
    }
}