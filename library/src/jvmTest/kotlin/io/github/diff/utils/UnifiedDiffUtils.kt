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
package io.github.diff.utils

import io.github.diff.Chunk
import io.github.diff.Delta
import io.github.diff.DeltaType
import io.github.diff.Patch
import io.github.diff.internal.helper.PatchHelper
import io.github.diff.internal.helper.PatchHelperImpl
import io.github.diff.internal.patch.PatchImpl

internal object UnifiedDiffUtils {
    private val UNIFIED_DIFF_CHUNK_REGEXP = Regex("^@@\\s+-(\\d+)(?:,(\\d+))?\\s+\\+(\\d+)(?:,(\\d+))?\\s+@@.*$")
    private const val NULL_FILE_INDICATOR = "/dev/null"

    /**
     * Parse the given text in unified format and creates the list of deltas for it.
     *
     * @param diff the text in unified format
     * @return the patch with deltas.
     */

    fun parseUnifiedDiff(diff: List<String>): Patch<String> {
        var inPrelude = true
        val rawChunk = mutableListOf<Array<String>>()
        val patch = PatchImpl<String>()

        var oldLn = 0
        var newLn = 0

        for (line in diff) {
            // Skip leading lines until after we've seen one starting with '+++'
            if (inPrelude) {
                if (line.startsWith("+++")) {
                    inPrelude = false
                }
                continue
            }
            val match = UNIFIED_DIFF_CHUNK_REGEXP.find(line)
            if (match != null) {
                // Process the lines in the previous chunk
                processLinesInPrevChunk(rawChunk, patch, oldLn, newLn)
                // Parse the @@ header
                oldLn = match.groupValues[1].toIntOrNull() ?: 1
                newLn = match.groupValues[3].toIntOrNull() ?: 1

                if (oldLn == 0) oldLn = 1
                if (newLn == 0) newLn = 1
            } else {
                if (line.isNotEmpty()) {
                    val tag = line.substring(0, 1)
                    val rest = line.substring(1)
                    if (tag == " " || tag == "+" || tag == "-") {
                        rawChunk.add(arrayOf(tag, rest))
                    }
                } else {
                    rawChunk.add(arrayOf(" ", ""))
                }
            }
        }

        // Process the lines in the last chunk
        processLinesInPrevChunk(rawChunk, patch, oldLn, newLn)

        return patch
    }

    private fun processLinesInPrevChunk(
        rawChunk: MutableList<Array<String>>,
        patch: Patch<String>,
        oldLn: Int,
        newLn: Int,
    ) {
        if (rawChunk.isNotEmpty()) {
            val oldChunkLines = mutableListOf<String>()
            val newChunkLines = mutableListOf<String>()

            val removePosition = mutableListOf<Int>()
            val addPosition = mutableListOf<Int>()
            var removeNum = 0
            var addNum = 0

            for (rawLine in rawChunk) {
                val tag = rawLine[0]
                val rest = rawLine[1]
                if (tag == " " || tag == "-") {
                    removeNum++
                    oldChunkLines.add(rest)
                    if (tag == "-") {
                        removePosition.add(oldLn - 1 + removeNum)
                    }
                }
                if (tag == " " || tag == "+") {
                    addNum++
                    newChunkLines.add(rest)
                    if (tag == "+") {
                        addPosition.add(newLn - 1 + addNum)
                    }
                }
            }
            patch.addDelta(
                Delta(
                    DeltaType.CHANGE,
                    Chunk(oldLn - 1, oldChunkLines, removePosition),
                    Chunk(newLn - 1, newChunkLines, addPosition),
                ),
            )
            rawChunk.clear()
        }
    }

    /**
     * generateUnifiedDiff takes a Patch and some other arguments, returning the Unified Diff format
     * text representing the Patch.  Author: Bill James (tankerbay@gmail.com).
     *
     * @param originalFileName - Filename of the original (unrevised file)
     * @param revisedFileName - Filename of the revised file
     * @param originalLines - Lines of the original file
     * @param patch - Patch created by the diff() function
     * @param contextSize - number of lines of context output around each difference in the file.
     * @return List of strings representing the Unified Diff representation of the Patch argument.
     */

    fun generateUnifiedDiff(
        originalFileName: String?,
        revisedFileName: String?,
        originalLines: List<String>,
        patch: Patch<String>,
        contextSize: Int,
    ): List<String> {
        if (patch.getDeltas().isEmpty()) {
            return emptyList()
        }

        val ret = mutableListOf<String>()
        ret.add("--- ${originalFileName ?: NULL_FILE_INDICATOR}")
        ret.add("+++ ${revisedFileName ?: NULL_FILE_INDICATOR}")

        val patchDeltas = patch.getDeltas().toMutableList()

        val deltas = mutableListOf<Delta<String>>()
        var delta = patchDeltas[0]
        deltas.add(delta)

        if (patchDeltas.size > 1) {
            for (i in 1 until patchDeltas.size) {
                val position = delta.source.position

                val nextDelta = patchDeltas[i]
                if ((position + delta.source.size() + contextSize) >= (nextDelta.source.position - contextSize)) {
                    deltas.add(nextDelta)
                } else {
                    val curBlock = processDeltas(originalLines, deltas, contextSize, false)
                    ret.addAll(curBlock)
                    deltas.clear()
                    deltas.add(nextDelta)
                }
                delta = nextDelta
            }
        }

        val curBlock =
            processDeltas(
                originalLines,
                deltas,
                contextSize,
                patchDeltas.size == 1 && originalFileName == null,
            )
        ret.addAll(curBlock)
        return ret
    }

    private fun processDeltas(
        origLines: List<String>,
        deltas: List<Delta<String>>,
        contextSize: Int,
        newFile: Boolean,
    ): List<String> {
        val buffer = mutableListOf<String>()
        var origTotal = 0
        var revTotal = 0

        val curDelta = deltas[0]
        val origStart =
            if (newFile) {
                0
            } else {
                val start = curDelta.source.position + 1 - contextSize
                if (start < 1) 1 else start
            }

        var revStart = curDelta.target.position + 1 - contextSize
        if (revStart < 1) revStart = 1

        var contextStart = curDelta.source.position - contextSize
        if (contextStart < 0) contextStart = 0

        for (line in contextStart until curDelta.source.position) {
            buffer.add(" ${origLines[line]}")
            origTotal++
            revTotal++
        }

        buffer.addAll(getDeltaText(curDelta))
        origTotal += curDelta.source.lines.size
        revTotal += curDelta.target.lines.size

        var deltaIndex = 1
        var currentDelta = curDelta
        while (deltaIndex < deltas.size) {
            val nextDelta = deltas[deltaIndex]
            val intermediateStart = currentDelta.source.position + currentDelta.source.lines.size
            for (line in intermediateStart until nextDelta.source.position) {
                buffer.add(" ${origLines[line]}")
                origTotal++
                revTotal++
            }
            buffer.addAll(getDeltaText(nextDelta))
            origTotal += nextDelta.source.lines.size
            revTotal += nextDelta.target.lines.size
            currentDelta = nextDelta
            deltaIndex++
        }

        contextStart = currentDelta.source.position + currentDelta.source.lines.size
        val endLine = minOf(contextStart + contextSize, origLines.size)
        for (line in contextStart until endLine) {
            buffer.add(" ${origLines[line]}")
            origTotal++
            revTotal++
        }

        val header = "@@ -$origStart,$origTotal +$revStart,$revTotal @@"
        buffer.add(0, header)

        return buffer
    }

    private fun getDeltaText(delta: Delta<String>): List<String> {
        val buffer = mutableListOf<String>()
        for (line in delta.source.lines) {
            buffer.add("-$line")
        }
        for (line in delta.target.lines) {
            buffer.add("+$line")
        }
        return buffer
    }

    /**
     * Compare the differences between two files and return to the original file and diff format
     */

    fun generateOriginalAndDiff(
        original: List<String>,
        revised: List<String>,
    ): List<String> {
        return generateOriginalAndDiff(original, revised, null, null)
    }

    /**
     * Compare the differences between two files and return to the original file and diff format
     */

    fun generateOriginalAndDiff(
        original: List<String>,
        revised: List<String>,
        originalFileName: String?,
        revisedFileName: String?,
    ): List<String> {
        val origFileName = originalFileName ?: "original"
        val revFileName = revisedFileName ?: "revised"

        val helper: PatchHelper<String> = PatchHelperImpl()
        val patch = helper.generate(original, revised)
        var unifiedDiff = generateUnifiedDiff(origFileName, revFileName, original, patch, 0)

        if (unifiedDiff.isEmpty()) {
            unifiedDiff =
                mutableListOf(
                    "--- $origFileName",
                    "+++ $revFileName",
                    "@@ -0,0 +0,0 @@",
                )
        } else if (unifiedDiff.size >= 3 && !unifiedDiff[2].contains("@@ -1,")) {
            unifiedDiff = unifiedDiff.toMutableList()
            unifiedDiff.add(2, "@@ -0,0 +0,0 @@")
        }

        val originalWithPrefix = original.map { " $it" }
        return insertOrig(originalWithPrefix, unifiedDiff)
    }

    private fun insertOrig(
        original: List<String>,
        unifiedDiff: List<String>,
    ): List<String> {
        val result = mutableListOf<String>()
        val diffList = mutableListOf<List<String>>()
        val diff = mutableListOf<String>()

        for (i in unifiedDiff.indices) {
            val u = unifiedDiff[i]
            if (u.startsWith("@@") && u != "@@ -0,0 +0,0 @@" && !u.contains("@@ -1,")) {
                diffList.add(diff.toList())
                diff.clear()
                diff.add(u)
                continue
            }
            if (i == unifiedDiff.size - 1) {
                diff.add(u)
                diffList.add(diff.toList())
                diff.clear()
                break
            }
            diff.add(u)
        }

        insertOrig(diffList, result, original)
        return result
    }

    private fun insertOrig(
        diffList: List<List<String>>,
        result: MutableList<String>,
        original: List<String>,
    ) {
        for (i in diffList.indices) {
            val diff = diffList[i]
            val nextDiff = if (i == diffList.size - 1) null else diffList[i + 1]
            val simb = if (i == 0) diff[2] else diff[0]
            val nextSimb = nextDiff?.get(0)

            insert(result, diff)
            val map = getRowMap(simb)

            if (nextSimb != null) {
                val nextMap = getRowMap(nextSimb)
                val start =
                    if (map["orgRow"]!! != 0) {
                        map["orgRow"]!! + map["orgDel"]!! - 1
                    } else {
                        0
                    }
                val end = nextMap["revRow"]!! - 2
                insert(result, getOrigList(original, start, end))
            }

            var start = map["orgRow"]!! + map["orgDel"]!! - 1
            start = if (start == -1) 0 else start

            if (simb.contains("@@ -1,") && nextSimb == null && map["orgDel"]!! != original.size) {
                insert(result, getOrigList(original, start, original.size - 1))
            } else if (nextSimb == null && (map["orgRow"]!! + map["orgDel"]!! - 1) < original.size) {
                insert(result, getOrigList(original, start, original.size - 1))
            }
        }
    }

    private fun insert(
        result: MutableList<String>,
        noChangeContent: List<String>,
    ) {
        result.addAll(noChangeContent)
    }

    private fun getRowMap(str: String): Map<String, Int> {
        val map = mutableMapOf<String, Int>()
        if (str.startsWith("@@")) {
            val sp = str.split(" ")
            val org = sp[1]
            val orgSp = org.split(",")
            map["orgRow"] = orgSp[0].substring(1).toInt()
            map["orgDel"] = orgSp[1].toInt()
            val revSp = org.split(",")
            map["revRow"] = revSp[0].substring(1).toInt()
            map["revAdd"] = revSp[1].toInt()
        }
        return map
    }

    private fun getOrigList(
        originalWithPrefix: List<String>,
        start: Int,
        end: Int,
    ): List<String> {
        return if (originalWithPrefix.size >= 1 && start <= end && end < originalWithPrefix.size) {
            originalWithPrefix.subList(start, end + 1)
        } else {
            emptyList()
        }
    }
}
