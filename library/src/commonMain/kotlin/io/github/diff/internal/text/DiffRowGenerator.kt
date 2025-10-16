/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.diff.internal.text

import io.github.diff.Chunk
import io.github.diff.Delta
import io.github.diff.DeltaType
import io.github.diff.DiffAlgorithm
import io.github.diff.Patch
import io.github.diff.internal.helper.PatchHelper
import io.github.diff.internal.helper.PatchHelperImpl
import io.github.diff.internal.text.DiffRow.Tag
import io.github.diff.internal.text.deltamerge.DeltaMergeUtils
import io.github.diff.internal.text.deltamerge.InlineDeltaMergeInfo

/**
 * This class for generating DiffRows for side-by-side view.
 * // todo refactoring needed
 */
internal class DiffRowGenerator private constructor(builder: Builder) {
    private val columnWidth: Int = builder.columnWidth
    private val equalizer: (String, String) -> Boolean
    private val ignoreWhiteSpaces: Boolean = builder.ignoreWhiteSpaces
    private val inlineDiffSplitter: (String) -> List<String> = builder.inlineDiffSplitter
    private val mergeOriginalRevised: Boolean = builder.mergeOriginalRevised
    private val newTag: (Tag, Boolean) -> String = builder.newTag
    private val oldTag: (Tag, Boolean) -> String = builder.oldTag
    private val reportLinesUnchanged: Boolean = builder.reportLinesUnchanged
    private val lineNormalizer: (String) -> String = builder.lineNormalizer
    private val processDiffs: ((String) -> String)? = builder.processDiffs
    private val inlineDeltaMerger: (InlineDeltaMergeInfo) -> List<Delta<String>> = builder.inlineDeltaMerger
    private val showInlineDiffs: Boolean = builder.showInlineDiffs
    private val replaceOriginalLinefeedInChangesWithSpaces: Boolean = builder.replaceOriginalLinefeedInChangesWithSpaces
    private val decompressDeltas: Boolean = builder.decompressDeltas

    init {
        equalizer = builder.equalizer ?: if (ignoreWhiteSpaces) {
            IGNORE_WHITESPACE_EQUALIZER
        } else {
            DEFAULT_EQUALIZER
        }
    }

    /**
     * Get the DiffRows describing the difference between original and revised
     * texts using the given patch. Useful for displaying side-by-side diff.
     */
    fun generateDiffRows(
        original: List<String>,
        revised: List<String>,
    ): List<DiffRow> {
        val helper: PatchHelper<String> = PatchHelperImpl()
        val algorithm = DiffAlgorithm.Factory.createMyers(equalizer = equalizer)
        return generateDiffRows(original, helper.generate(original, revised, algorithm))
    }

    /**
     * Generates the DiffRows describing the difference between original and
     * revised texts using the given patch. Useful for displaying side-by-side
     * diff.
     */
    fun generateDiffRows(
        original: List<String>,
        patch: Patch<String>,
    ): List<DiffRow> {
        val diffRows = mutableListOf<DiffRow>()
        var endPos = 0
        val deltaList = patch.getDeltas()

        if (decompressDeltas) {
            for (originalDelta in deltaList) {
                for (delta in decompressDeltas(originalDelta)) {
                    endPos = transformDeltaIntoDiffRow(original, endPos, diffRows, delta)
                }
            }
        } else {
            for (delta in deltaList) {
                endPos = transformDeltaIntoDiffRow(original, endPos, diffRows, delta)
            }
        }

        // Copy the final matching chunk if any.
        for (line in original.subList(endPos, original.size)) {
            diffRows.add(buildDiffRow(Tag.EQUAL, line, line))
        }
        return diffRows
    }

    private fun transformDeltaIntoDiffRow(
        original: List<String>,
        endPos: Int,
        diffRows: MutableList<DiffRow>,
        delta: Delta<String>,
    ): Int {
        val orig = delta.source
        val rev = delta.target

        for (line in original.subList(endPos, orig.position)) {
            diffRows.add(buildDiffRow(Tag.EQUAL, line, line))
        }

        when (delta.type) {
            DeltaType.INSERT -> {
                for (line in rev.lines) {
                    diffRows.add(buildDiffRow(Tag.INSERT, "", line))
                }
            }
            DeltaType.DELETE -> {
                for (line in orig.lines) {
                    diffRows.add(buildDiffRow(Tag.DELETE, line, ""))
                }
            }
            else -> {
                if (showInlineDiffs) {
                    diffRows.addAll(generateInlineDiffs(delta))
                } else {
                    for (j in 0 until maxOf(orig.size(), rev.size())) {
                        diffRows.add(
                            buildDiffRow(
                                Tag.CHANGE,
                                if (orig.lines.size > j) orig.lines[j] else "",
                                if (rev.lines.size > j) rev.lines[j] else "",
                            ),
                        )
                    }
                }
            }
        }

        return orig.last() + 1
    }

    private fun decompressDeltas(delta: Delta<String>): List<Delta<String>> {
        if (delta.type == DeltaType.CHANGE && delta.source.size() != delta.target.size()) {
            val deltas = mutableListOf<Delta<String>>()

            val minSize = minOf(delta.source.size(), delta.target.size())
            val orig = delta.source
            val rev = delta.target

            deltas.add(
                Delta(
                    DeltaType.CHANGE,
                    Chunk(orig.position, orig.lines.subList(0, minSize)),
                    Chunk(rev.position, rev.lines.subList(0, minSize)),
                ),
            )

            if (orig.lines.size < rev.lines.size) {
                deltas.add(
                    Delta(
                        DeltaType.INSERT,
                        Chunk(orig.position + minSize, emptyList()),
                        Chunk(rev.position + minSize, rev.lines.subList(minSize, rev.lines.size)),
                    ),
                )
            } else {
                deltas.add(
                    Delta(
                        DeltaType.DELETE,
                        Chunk(orig.position + minSize, orig.lines.subList(minSize, orig.lines.size)),
                        Chunk(rev.position + minSize, emptyList()),
                    ),
                )
            }
            return deltas
        }

        return listOf(delta)
    }

    private fun buildDiffRow(
        type: Tag,
        orgline: String,
        newline: String,
    ): DiffRow {
        return if (reportLinesUnchanged) {
            DiffRow(type, orgline, newline)
        } else {
            var wrapOrg = preprocessLine(orgline)
            if (type == Tag.DELETE) {
                if (mergeOriginalRevised || showInlineDiffs) {
                    wrapOrg = oldTag(type, true) + wrapOrg + oldTag(type, false)
                }
            }
            var wrapNew = preprocessLine(newline)
            if (type == Tag.INSERT) {
                if (mergeOriginalRevised) {
                    wrapOrg = newTag(type, true) + wrapNew + newTag(type, false)
                } else if (showInlineDiffs) {
                    wrapNew = newTag(type, true) + wrapNew + newTag(type, false)
                }
            }
            if (type == Tag.CHANGE) {
                if (mergeOriginalRevised) {
                    wrapOrg = oldTag(type, true) + wrapOrg + oldTag(type, false)
                }
            }
            DiffRow(type, wrapOrg, wrapNew)
        }
    }

    private fun buildDiffRowWithoutNormalizing(
        type: Tag,
        orgline: String,
        newline: String,
    ): DiffRow {
        return DiffRow(
            type,
            StringUtils.wrapText(orgline, columnWidth),
            StringUtils.wrapText(newline, columnWidth),
        )
    }

    fun normalizeLines(list: List<String>): List<String> {
        return if (reportLinesUnchanged) {
            list
        } else {
            list.map { lineNormalizer(it) }
        }
    }

    private fun generateInlineDiffs(delta: Delta<String>): List<DiffRow> {
        val orig = normalizeLines(delta.source.lines)
        val rev = normalizeLines(delta.target.lines)
        val joinedOrig = orig.joinToString("\n")
        val joinedRev = rev.joinToString("\n")

        val origList = inlineDiffSplitter(joinedOrig).toMutableList()
        val revList = inlineDiffSplitter(joinedRev).toMutableList()

        val helper: PatchHelper<String> = PatchHelperImpl()
        val originalInlineDeltas = helper.generate(origList, revList, algorithm = null).getDeltas()
        val inlineDeltas = inlineDeltaMerger(InlineDeltaMergeInfo(originalInlineDeltas, origList, revList))

        inlineDeltas.reversed().forEach { inlineDelta ->
            val inlineOrig = inlineDelta.source
            val inlineRev = inlineDelta.target
            when (inlineDelta.type) {
                DeltaType.DELETE -> {
                    wrapInTag(
                        origList,
                        inlineOrig.position,
                        inlineOrig.position + inlineOrig.size(),
                        Tag.DELETE,
                        oldTag,
                        processDiffs,
                        replaceOriginalLinefeedInChangesWithSpaces && mergeOriginalRevised,
                    )
                }
                DeltaType.INSERT -> {
                    if (mergeOriginalRevised) {
                        origList.addAll(
                            inlineOrig.position,
                            revList.subList(inlineRev.position, inlineRev.position + inlineRev.size()),
                        )
                        wrapInTag(
                            origList,
                            inlineOrig.position,
                            inlineOrig.position + inlineRev.size(),
                            Tag.INSERT,
                            newTag,
                            processDiffs,
                            false,
                        )
                    } else {
                        wrapInTag(
                            revList,
                            inlineRev.position,
                            inlineRev.position + inlineRev.size(),
                            Tag.INSERT,
                            newTag,
                            processDiffs,
                            false,
                        )
                    }
                }
                DeltaType.CHANGE -> {
                    if (mergeOriginalRevised) {
                        origList.addAll(
                            inlineOrig.position + inlineOrig.size(),
                            revList.subList(inlineRev.position, inlineRev.position + inlineRev.size()),
                        )
                        wrapInTag(
                            origList,
                            inlineOrig.position + inlineOrig.size(),
                            inlineOrig.position + inlineOrig.size() + inlineRev.size(),
                            Tag.CHANGE,
                            newTag,
                            processDiffs,
                            false,
                        )
                    } else {
                        wrapInTag(
                            revList,
                            inlineRev.position,
                            inlineRev.position + inlineRev.size(),
                            Tag.CHANGE,
                            newTag,
                            processDiffs,
                            false,
                        )
                    }
                    wrapInTag(
                        origList,
                        inlineOrig.position,
                        inlineOrig.position + inlineOrig.size(),
                        Tag.CHANGE,
                        oldTag,
                        processDiffs,
                        replaceOriginalLinefeedInChangesWithSpaces && mergeOriginalRevised,
                    )
                }
                else -> {}
            }
        }

        val origResult = StringBuilder()
        val revResult = StringBuilder()
        for (character in origList) {
            origResult.append(character)
        }
        for (character in revList) {
            revResult.append(character)
        }

        val original = origResult.toString().split("\n").toMutableList()
        val revised = revResult.toString().split("\n").toMutableList()
        // Kotlin's split preserves trailing empty strings, .
        // The original behavior (which tests are based on) does not produce an extra
        // empty diff row when both original and revised inline representations end with a trailing
        // newline. We therefore trim a single trailing empty element iff BOTH sides end with an
        // empty string. This keeps legitimate blank line changes (where only one side is empty or
        // where the blank line itself is meaningful inside the diff) while avoiding an extra
        // spurious [CHANGE, , ] row (observed in testGeneratorWithMerge3).
        if (original.size > 1 && revised.size > 1 && original.last().isEmpty() && revised.last().isEmpty()) {
            original.removeAt(original.lastIndex)
            revised.removeAt(revised.lastIndex)
        }
        val diffRows = mutableListOf<DiffRow>()
        for (j in 0 until maxOf(original.size, revised.size)) {
            diffRows.add(
                buildDiffRowWithoutNormalizing(
                    Tag.CHANGE,
                    if (original.size > j) original[j] else "",
                    if (revised.size > j) revised[j] else "",
                ),
            )
        }
        return diffRows
    }

    private fun preprocessLine(line: String): String {
        return if (columnWidth == 0) {
            lineNormalizer(line)
        } else {
            StringUtils.wrapText(lineNormalizer(line), columnWidth)
        }
    }

    class Builder internal constructor() {
        internal var showInlineDiffs = false
        internal var ignoreWhiteSpaces = false
        internal var decompressDeltas = true
        internal var oldTag: (
            Tag,
            Boolean,
        ) -> String = { _, f -> if (f) "<span class=\"editOldInline\">" else "</span>" }
        internal var newTag: (
            Tag,
            Boolean,
        ) -> String = { _, f -> if (f) "<span class=\"editNewInline\">" else "</span>" }
        internal var columnWidth = 0
        internal var mergeOriginalRevised = false
        internal var reportLinesUnchanged = false
        internal var inlineDiffSplitter: (String) -> List<String> = SPLITTER_BY_CHARACTER
        internal var lineNormalizer: (String) -> String = LINE_NORMALIZER_FOR_HTML
        internal var processDiffs: ((String) -> String)? = null
        internal var equalizer: ((String, String) -> Boolean)? = null
        internal var replaceOriginalLinefeedInChangesWithSpaces = false
        internal var inlineDeltaMerger: (InlineDeltaMergeInfo) -> List<Delta<String>> = DEFAULT_INLINE_DELTA_MERGER

        fun showInlineDiffs(value: Boolean) = apply { showInlineDiffs = value }

        fun ignoreWhiteSpaces(value: Boolean) = apply { ignoreWhiteSpaces = value }

        fun reportLinesUnchanged(value: Boolean) = apply { reportLinesUnchanged = value }

        fun oldTag(generator: (Tag, Boolean) -> String) = apply { oldTag = generator }

        fun oldTag(generator: (Boolean) -> String) = apply { oldTag = { _, f -> generator(f) } }

        fun newTag(generator: (Tag, Boolean) -> String) = apply { newTag = generator }

        fun newTag(generator: (Boolean) -> String) = apply { newTag = { _, f -> generator(f) } }

        fun processDiffs(processor: (String) -> String) = apply { processDiffs = processor }

        fun columnWidth(width: Int) = apply { if (width >= 0) columnWidth = width }

        fun build() = DiffRowGenerator(this)

        fun mergeOriginalRevised(value: Boolean) = apply { mergeOriginalRevised = value }

        fun decompressDeltas(value: Boolean) = apply { decompressDeltas = value }

        fun inlineDiffByWord(value: Boolean) =
            apply {
                inlineDiffSplitter = if (value) SPLITTER_BY_WORD else SPLITTER_BY_CHARACTER
            }

        fun inlineDiffBySplitter(splitter: (String) -> List<String>) = apply { inlineDiffSplitter = splitter }

        fun lineNormalizer(normalizer: (String) -> String) = apply { lineNormalizer = normalizer }

        fun equalizer(eq: (String, String) -> Boolean) = apply { equalizer = eq }

        fun replaceOriginalLinefeedInChangesWithSpaces(value: Boolean) =
            apply {
                replaceOriginalLinefeedInChangesWithSpaces = value
            }

        fun inlineDeltaMerger(merger: (InlineDeltaMergeInfo) -> List<Delta<String>>) = apply { inlineDeltaMerger = merger }
    }

    companion object {
        fun create() = Builder()

        val DEFAULT_EQUALIZER: (String, String) -> Boolean = { a, b -> a == b }

        val IGNORE_WHITESPACE_EQUALIZER: (String, String) -> Boolean = { original, revised ->
            adjustWhitespace(original) == adjustWhitespace(revised)
        }

        val LINE_NORMALIZER_FOR_HTML: (String) -> String = { str -> StringUtils.normalize(str) }

        val SPLITTER_BY_CHARACTER: (String) -> List<String> = { line -> line.map { it.toString() } }

        val SPLIT_BY_WORD_REGEX: Regex = "\\s+|[,\\.\\[\\](){}\\/\\\\*+\\-#<>;:&']+".toRegex()
//        Regex("\\s+|[,.\[\](){}/\\\\*+\-#<>;:&']+")

        val SPLITTER_BY_WORD: (String) -> List<String> = { line ->
            splitStringPreserveDelimiter(line, SPLIT_BY_WORD_REGEX)
        }

        val WHITESPACE_REGEX: Regex = Regex("\\s+")

        val DEFAULT_INLINE_DELTA_MERGER: (InlineDeltaMergeInfo) -> List<Delta<String>> = { it.deltas }

        val WHITESPACE_EQUALITIES_MERGER: (InlineDeltaMergeInfo) -> List<Delta<String>> = { deltaMergeInfo ->
            DeltaMergeUtils.mergeInlineDeltas(deltaMergeInfo) { equalities ->
                equalities.all { it.replace(Regex("\\s+"), "") == "" }
            }
        }

        private fun adjustWhitespace(raw: String): String = WHITESPACE_REGEX.replace(raw.trim(), " ")

        fun splitStringPreserveDelimiter(
            str: String?,
            pattern: Regex,
        ): List<String> {
            val list = mutableListOf<String>()
            if (str != null) {
                var pos = 0
                for (m in pattern.findAll(str)) {
                    if (pos < m.range.first) {
                        val before = str.substring(pos, m.range.first)
                        // Separate trailing whitespace from non-whitespace to keep whitespace as its own token(s)
                        val trailingWsIndex = before.indexOfLast { !it.isWhitespace() }
                        if (trailingWsIndex >= 0) {
                            val nonWs = before.substring(0, trailingWsIndex + 1)
                            val ws = before.substring(trailingWsIndex + 1)
                            if (nonWs.isNotEmpty()) list.add(nonWs)
                            if (ws.isNotEmpty()) list.add(ws)
                        } else if (before.isNotEmpty()) {
                            list.add(before)
                        }
                    }
                    list.add(m.value)
                    pos = m.range.last + 1
                }
                if (pos < str.length) {
                    val after = str.substring(pos)
                    // Separate leading whitespace from the rest so that whitespace becomes its own token
                    val firstNonWs = after.indexOfFirst { !it.isWhitespace() }
                    if (firstNonWs == -1) {
                        // All whitespace
                        if (after.isNotEmpty()) list.add(after)
                    } else {
                        val leadingWs = after.substring(0, firstNonWs)
                        val remainder = after.substring(firstNonWs)
                        if (leadingWs.isNotEmpty()) list.add(leadingWs)
                        if (remainder.isNotEmpty()) list.add(remainder)
                    }
                }
            }
            return list
        }

        fun wrapInTag(
            sequence: MutableList<String>,
            startPosition: Int,
            endPosition: Int,
            tag: Tag,
            tagGenerator: (Tag, Boolean) -> String,
            processDiffs: ((String) -> String)?,
            replaceLinefeedWithSpace: Boolean,
        ) {
            // Normalize bounds and optionally replace newlines inside region with spaces.
            if (startPosition >= endPosition) return
            var start = startPosition
            var end = endPosition
            // Trim leading newlines (cannot sensibly be wrapped) but honor replaceLinefeedWithSpace
            while (start < end && sequence[start] == "\n") {
                if (replaceLinefeedWithSpace) sequence[start] = " " else start++
            }
            // Trim trailing newlines
            while (end > start && sequence[end - 1] == "\n") {
                if (replaceLinefeedWithSpace) {
                    sequence[end - 1] = " "
                    break
                } else {
                    end--
                }
            }
            if (start >= end) return
            // Replace internal newlines if requested
            if (replaceLinefeedWithSpace) {
                for (i in start until end) if (sequence[i] == "\n") sequence[i] = " "
            }
            // Apply diff processing to last token (parity with previous implementation)
            processDiffs?.let { sequence[end - 1] = it(sequence[end - 1]) }
            // Insert closing tag first (after the region) then opening tag at start.
            sequence.add(end, tagGenerator(tag, false))
            sequence.add(start, tagGenerator(tag, true))
        }
    }
}
