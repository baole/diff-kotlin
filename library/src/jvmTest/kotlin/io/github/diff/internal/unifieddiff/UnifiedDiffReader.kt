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
package io.github.diff.internal.unifieddiff

import io.github.diff.Chunk
import io.github.diff.Delta
import io.github.diff.DeltaType
import io.github.diff.UnifiedDiffParserException

/**
 * Kotlin-only unified diff reader / parsing a list of lines.
 */
internal class UnifiedDiffReader private constructor(private val reader: InternalUnifiedDiffReader) {
    private val data = UnifiedDiff()

    // Simple lightweight logger replacement
    private object Log {
        var debug: Boolean = false

        fun fine(msg: String) {
            if (debug) println("[fine] $msg")
        }

        fun warning(msg: String) {
            println("[warn] $msg")
        }
    }

    private val diffCommand = UnifiedDiffLine(true, Regex("^diff\\s")) { _, line -> processDiff(line) }
    private val similarityIndex =
        UnifiedDiffLine(true, Regex("^similarity index (\\d+)%$")) { match, _ -> processSimilarityIndex(match) }
    private val index =
        UnifiedDiffLine(
            true,
            Regex("^index\\s[\\da-zA-Z]+\\.\\.[\\da-zA-Z]+(\\s(\\d+))?$"),
        ) { _, line -> processIndex(line) }
    private val fromFile = UnifiedDiffLine(true, Regex("^---\\s")) { _, line -> processFromFile(line) }
    private val toFile = UnifiedDiffLine(true, Regex("^\\+\\+\\+\\s")) { _, line -> processToFile(line) }
    private val renameFrom =
        UnifiedDiffLine(true, Regex("^rename\\sfrom\\s(.+)$")) { match, _ -> processRenameFrom(match) }
    private val renameTo = UnifiedDiffLine(true, Regex("^rename\\sto\\s(.+)$")) { match, _ -> processRenameTo(match) }
    private val copyFrom = UnifiedDiffLine(true, Regex("^copy\\sfrom\\s(.+)$")) { match, _ -> processCopyFrom(match) }
    private val copyTo = UnifiedDiffLine(true, Regex("^copy\\sto\\s(.+)$")) { match, _ -> processCopyTo(match) }
    private val newFileMode =
        UnifiedDiffLine(true, Regex("^new\\sfile\\smode\\s(\\d+)")) { match, _ -> processNewFileMode(match) }
    private val deleteFileMode =
        UnifiedDiffLine(true, Regex("^deleted\\sfile\\smode\\s(\\d+)")) { match, _ -> processDeletedFileMode(match) }
    private val oldMode = UnifiedDiffLine(true, Regex("^old\\smode\\s(\\d+)")) { match, _ -> processOldMode(match) }
    private val newMode = UnifiedDiffLine(true, Regex("^new\\smode\\s(\\d+)")) { match, _ -> processNewMode(match) }
    private val binaryAdded =
        UnifiedDiffLine(
            true,
            Regex("^Binary\\sfiles\\s/dev/null\\sand\\sb/(.+)\\sdiffer"),
        ) { match, _ -> processBinaryAdded(match) }
    private val binaryDeleted =
        UnifiedDiffLine(
            true,
            Regex("^Binary\\sfiles\\sa/(.+)\\sand\\s/dev/null\\sdiffer"),
        ) { match, _ -> processBinaryDeleted(match) }
    private val binaryEdited =
        UnifiedDiffLine(
            true,
            Regex("^Binary\\sfiles\\sa/(.+)\\sand\\sb/(.+)\\sdiffer"),
        ) { match, _ -> processBinaryEdited(match) }
    private val chunk = UnifiedDiffLine(false, UNIFIED_DIFF_CHUNK_REGEX) { match, _ -> processChunk(match) }
    private val lineEmpty = UnifiedDiffLine(Regex("^$")) { _, _ -> processEmptyContextLine() }
    private val lineNormal = UnifiedDiffLine(Regex("^\\s")) { _, line -> processNormalLine(line) }
    private val lineDeleted = UnifiedDiffLine(Regex("^-")) { _, line -> processDelLine(line) }
    private val lineAdded = UnifiedDiffLine(Regex("^\\+")) { _, line -> processAddLine(line) }

    private var actualFile: UnifiedDiffFile? = null

    private var originalTxt = mutableListOf<String>()
    private var revisedTxt = mutableListOf<String>()
    private var addLineIdxList = mutableListOf<Int>()
    private var delLineIdxList = mutableListOf<Int>()
    private var oldLn = 0
    private var oldSize = 0
    private var newLn = 0
    private var newSize = 0
    private var delLineIdx = 0
    private var addLineIdx = 0

    private fun parse(): UnifiedDiff {
        var line: String? = reader.readLine()
        while (line != null) {
            var headerTxt = ""
            Log.fine("header parsing")
            while (line != null) {
                Log.fine("parsing line $line")
                if (validLine(
                        line,
                        diffCommand,
                        similarityIndex,
                        index,
                        fromFile,
                        toFile,
                        renameFrom,
                        renameTo,
                        copyFrom,
                        copyTo,
                        newFileMode,
                        deleteFileMode,
                        oldMode,
                        newMode,
                        binaryAdded,
                        binaryDeleted,
                        binaryEdited,
                        chunk,
                    )
                ) {
                    break
                } else {
                    headerTxt += line + "\n"
                }
                line = reader.readLine()
            }
            if (headerTxt.isNotEmpty()) data.header = headerTxt
            if (line != null && !chunk.validLine(line)) {
                initFileIfNecessary()
                while (line != null && !chunk.validLine(line)) {
                    if (!processLine(
                            line,
                            diffCommand,
                            similarityIndex,
                            index,
                            fromFile,
                            toFile,
                            renameFrom,
                            renameTo,
                            copyFrom,
                            copyTo,
                            newFileMode,
                            deleteFileMode,
                            oldMode,
                            newMode,
                            binaryAdded,
                            binaryDeleted,
                            binaryEdited,
                        )
                    ) {
                        throw UnifiedDiffParserException("expected file start line not found")
                    }
                    line = reader.readLine()
                }
            }
            if (line != null) {
                processLine(line, chunk)
                while (true) {
                    val currentLine = reader.readLine()
                    line = checkForNoNewLineAtTheEndOfTheFile(currentLine)
                    if (currentLine == null) break
                    if (!processLine(line, lineEmpty, lineNormal, lineAdded, lineDeleted)) {
                        throw UnifiedDiffParserException("expected data line not found")
                    }
                    if (originalTxt.size == oldSize && revisedTxt.size == newSize ||
                        (oldSize == 0 && newSize == 0 && originalTxt.size == oldLn && revisedTxt.size == newLn)
                    ) {
                        finalizeChunk()
                        break
                    }
                }
                line = reader.readLine()
                line = checkForNoNewLineAtTheEndOfTheFile(line)
            }
            if (line == null || (line.startsWith("--") && !line.startsWith("---"))) {
                break
            }
        }
        if (reader.ready()) {
            val tailLinesOriginal = mutableListOf<String>()
            while (reader.ready()) {
                tailLinesOriginal.add(reader.readLine() ?: break)
            }
            if (tailLinesOriginal.isNotEmpty()) {
                val lines = tailLinesOriginal.dropWhile { it.startsWith("--") && !it.startsWith("---") }
                if (lines.isNotEmpty()) {
                    val tailContent = lines.first()
                    val emptyCount = lines.drop(1).count { it.isEmpty() }
                    // Append newline only if there are two or more empty trailing lines (explicit blank + EOF newline)
                    data.setTailTxt(tailContent + if (emptyCount >= 2) "\n" else "")
                }
            }
        }
        return data
    }

    private fun checkForNoNewLineAtTheEndOfTheFile(line: String?): String? {
        if (line == "\\ No newline at end of file") {
            actualFile?.isNoNewLineAtTheEndOfTheFile = true
            return reader.readLine()
        }
        return line
    }

    private fun processLine(
        line: String?,
        vararg rules: UnifiedDiffLine,
    ): Boolean {
        if (line == null) return false
        for (rule in rules) {
            if (rule.processLine(line)) {
                Log.fine("  >>> processed rule $rule")
                return true
            }
        }
        Log.warning("  >>> no rule matched $line")
        return false
    }

    private fun validLine(
        line: String?,
        vararg rules: UnifiedDiffLine,
    ): Boolean {
        if (line == null) return false
        for (rule in rules) {
            if (rule.validLine(line)) {
                Log.fine("  >>> accepted rule $rule")
                return true
            }
        }
        return false
    }

    private fun initFileIfNecessary() {
        if (originalTxt.isNotEmpty() || revisedTxt.isNotEmpty()) throw IllegalStateException()
        actualFile = UnifiedDiffFile().also { data.addFile(it) }
    }

    private fun processDiff(line: String) {
        Log.fine("start $line")
        val fromTo = parseFileNames(reader.lastLine()!!)
        actualFile?.fromFile = fromTo[0]
        actualFile?.toFile = fromTo[1]
        actualFile?.diffCommand = line
    }

    private fun processSimilarityIndex(match: MatchResult) {
        actualFile?.similarityIndex = match.groupValues[1].toInt()
    }

    private fun finalizeChunk() {
        if (originalTxt.isNotEmpty() || revisedTxt.isNotEmpty()) {
            actualFile?.patch?.addDelta(
                Delta(
                    DeltaType.CHANGE,
                    Chunk(oldLn - 1, originalTxt.toList(), delLineIdxList.toList()),
                    Chunk(newLn - 1, revisedTxt.toList(), addLineIdxList.toList()),
                ),
            )
            oldLn = 0
            newLn = 0
            originalTxt.clear()
            revisedTxt.clear()
            addLineIdxList.clear()
            delLineIdxList.clear()
            delLineIdx = 0
            addLineIdx = 0
        }
    }

    private fun processNormalLine(line: String) {
        val cline = line.substring(1)
        originalTxt.add(cline)
        revisedTxt.add(cline)
        delLineIdx++
        addLineIdx++
    }

    private fun processAddLine(line: String) {
        val cline = line.substring(1)
        revisedTxt.add(cline)
        addLineIdx++
        addLineIdxList.add(newLn - 1 + addLineIdx)
    }

    private fun processDelLine(line: String) {
        val cline = line.substring(1)
        originalTxt.add(cline)
        delLineIdx++
        delLineIdxList.add(oldLn - 1 + delLineIdx)
    }

    private fun processEmptyContextLine() {
        // Blank context line present in both original and revised
        originalTxt.add("")
        revisedTxt.add("")
        delLineIdx++
        addLineIdx++
    }

    private fun processChunk(match: MatchResult) {
        oldLn = toInteger(match, 1, 1)
        oldSize = toInteger(match, 2, 1)
        newLn = toInteger(match, 3, 1)
        newSize = toInteger(match, 4, 1)
        if (oldLn == 0) oldLn = 1
        if (newLn == 0) newLn = 1
    }

    private fun processIndex(line: String) {
        actualFile?.index = line.substring(6)
    }

    private fun processFromFile(line: String) {
        actualFile?.fromFile = extractFileName(line)
        actualFile?.fromTimestamp = extractTimestamp(line)
    }

    private fun processToFile(line: String) {
        actualFile?.toFile = extractFileName(line)
        actualFile?.toTimestamp = extractTimestamp(line)
    }

    private fun processRenameFrom(match: MatchResult) {
        actualFile?.renameFrom = match.groupValues[1]
    }

    private fun processRenameTo(match: MatchResult) {
        actualFile?.renameTo = match.groupValues[1]
    }

    private fun processCopyFrom(match: MatchResult) {
        actualFile?.copyFrom = match.groupValues[1]
    }

    private fun processCopyTo(match: MatchResult) {
        actualFile?.copyTo = match.groupValues[1]
    }

    private fun processNewFileMode(match: MatchResult) {
        actualFile?.newFileMode = match.groupValues[1]
    }

    private fun processDeletedFileMode(match: MatchResult) {
        actualFile?.deletedFileMode = match.groupValues[1]
    }

    private fun processOldMode(match: MatchResult) {
        actualFile?.oldMode = match.groupValues[1]
    }

    private fun processNewMode(match: MatchResult) {
        actualFile?.newMode = match.groupValues[1]
    }

    private fun processBinaryAdded(match: MatchResult) {
        actualFile?.binaryAdded = match.groupValues[1]
    }

    private fun processBinaryDeleted(match: MatchResult) {
        actualFile?.binaryDeleted = match.groupValues[1]
    }

    private fun processBinaryEdited(match: MatchResult) {
        actualFile?.binaryEdited = match.groupValues[1]
    }

    private fun extractFileName(_line: String): String {
        var line = _line
        val m = TIMESTAMP_REGEX.find(line)
        if (m != null) {
            line = line.substring(0, m.range.first)
        }
        line = line.split("\t")[0]
        return line.substring(4).replaceFirst(Regex("^(a|b|old|new)/"), "").trim()
    }

    private fun extractTimestamp(line: String): String? = TIMESTAMP_REGEX.find(line)?.value

    inner class UnifiedDiffLine(
        val stopsHeaderParsing: Boolean,
        private val pattern: Regex,
        private val command: (MatchResult, String) -> Unit,
    ) {
        constructor(pattern: Regex, command: (MatchResult, String) -> Unit) : this(false, pattern, command)

        fun validLine(line: String): Boolean = pattern.containsMatchIn(line)

        fun processLine(line: String): Boolean {
            val m = pattern.find(line)
            return if (m != null) {
                command(m, line)
                true
            } else {
                false
            }
        }

        override fun toString(): String = "UnifiedDiffLine{pattern=$pattern, stopsHeaderParsing=$stopsHeaderParsing}"
    }

    companion object {
        val UNIFIED_DIFF_CHUNK_REGEX = Regex("^@@\\s+-(\\d+)(?:,(\\d+))?\\s+\\+(\\d+)(?:,(\\d+))?\\s+@@")

        val TIMESTAMP_REGEX = Regex("(\\d{4}-\\d{2}-\\d{2}[T ]\\d{2}:\\d{2}:\\d{2}\\.\\d{3,})(?: [+-]\\d+)?")

        /** Backwards compatibility aliases (deprecated) */
        @Deprecated("Use UNIFIED_DIFF_CHUNK_REGEX", ReplaceWith("UNIFIED_DIFF_CHUNK_REGEX"))
        val UNIFIED_DIFF_CHUNK_REGEXP: Regex = UNIFIED_DIFF_CHUNK_REGEX

        @Deprecated("Use TIMESTAMP_REGEX", ReplaceWith("TIMESTAMP_REGEX"))
        val TIMESTAMP_REGEXP: Regex = TIMESTAMP_REGEX

        fun parseUnifiedDiff(lines: List<String>): UnifiedDiff {
            val parser = UnifiedDiffReader(InternalUnifiedDiffReader(lines))
            return parser.parse()
        }

        fun parseUnifiedDiff(text: String): UnifiedDiff =
            parseUnifiedDiff(
                if (text.isEmpty()) emptyList() else text.split("\n"),
            )

        fun parseFileNames(line: String): Array<String> {
            val split = line.split(" ")
            return arrayOf(
                split[2].replace(Regex("^a/"), ""),
                split[3].replace(Regex("^b/"), ""),
            )
        }

        private fun toInteger(
            match: MatchResult,
            group: Int,
            defValue: Int,
        ): Int = match.groupValues.getOrNull(group)?.takeIf { it.isNotEmpty() }?.toInt() ?: defValue
    }
}

internal class InternalUnifiedDiffReader(private val lines: List<String>) {
    private var index = 0
    private var lastLine: String? = null

    fun readLine(): String? {
        if (index >= lines.size) {
            lastLine = null
            return null
        }
        val l = lines[index++]
        lastLine = l
        return l
    }

    fun lastLine(): String? = lastLine

    fun ready(): Boolean = index < lines.size
}
