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

import io.github.diff.Delta

/**
 * Pure Kotlin unified diff writer
 */
internal object UnifiedDiffWriter {
    /**
     * Collect diff output into a String.
     */

    fun writeToString(
        diff: UnifiedDiff,
        originalLinesProvider: (String?) -> List<String>,
        contextSize: Int,
    ): String {
        val sb = StringBuilder()
        write(diff, originalLinesProvider, { line -> sb.append(line).append('\n') }, contextSize)
        return if (sb.isNotEmpty()) sb.removeSuffix("\n").toString() else ""
    }

    /**
     * Write diff lines using a line consumer callback.
     */

    fun write(
        diff: UnifiedDiff,
        originalLinesProvider: (String?) -> List<String>,
        writer: (String) -> Unit,
        contextSize: Int,
    ) {
        diff.header?.let { writer(it) }

        for (file in diff.files) {
            val patchDeltas = file.patch.getDeltas().toMutableList()
            if (patchDeltas.isNotEmpty()) {
                writeOrNothing(writer, file.diffCommand)
                file.index?.let { writer("index $it") }
                writer("--- ${file.fromFile ?: "/dev/null"}")
                file.toFile?.let { writer("+++ $it") }

                val originalLines = originalLinesProvider(file.fromFile)

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
                            processDeltas(writer, originalLines, deltas, contextSize, false)
                            deltas.clear()
                            deltas.add(nextDelta)
                        }
                        delta = nextDelta
                    }
                }

                processDeltas(
                    writer,
                    originalLines,
                    deltas,
                    contextSize,
                    patchDeltas.size == 1 && file.fromFile == null,
                )
            }
        }
        diff.tail?.let {
            writer("--")
            writer(it)
        }
    }

    private fun processDeltas(
        writer: (String) -> Unit,
        origLines: List<String>,
        deltas: List<Delta<String>>,
        contextSize: Int,
        newFile: Boolean,
    ) {
        val buffer = mutableListOf<String>()
        var origTotal = 0
        var revTotal = 0

        val curDelta = deltas[0]

        val origStart = if (newFile) 0 else (curDelta.source.position + 1 - contextSize).let { if (it < 1) 1 else it }
        var revStart = curDelta.target.position + 1 - contextSize
        if (revStart < 1) revStart = 1

        var contextStart = curDelta.source.position - contextSize
        if (contextStart < 0) contextStart = 0

        var line = contextStart
        while (line < curDelta.source.position && line < origLines.size) {
            buffer.add(" ${origLines[line]}")
            origTotal++
            revTotal++
            line++
        }

        getDeltaText({ txt -> buffer.add(txt) }, curDelta)
        origTotal += curDelta.source.lines.size
        revTotal += curDelta.target.lines.size

        var deltaIndex = 1
        var currentDelta = curDelta
        while (deltaIndex < deltas.size) {
            val nextDelta = deltas[deltaIndex]
            val intermediateStart = currentDelta.source.position + currentDelta.source.lines.size
            line = intermediateStart
            while (line < nextDelta.source.position && line < origLines.size) {
                buffer.add(" ${origLines[line]}")
                origTotal++
                revTotal++
                line++
            }
            getDeltaText({ txt -> buffer.add(txt) }, nextDelta)
            origTotal += nextDelta.source.lines.size
            revTotal += nextDelta.target.lines.size
            currentDelta = nextDelta
            deltaIndex++
        }

        contextStart = currentDelta.source.position + currentDelta.source.lines.size
        line = contextStart
        while (line < (contextStart + contextSize) && line < origLines.size) {
            buffer.add(" ${origLines[line]}")
            origTotal++
            revTotal++
            line++
        }

        writer("@@ -$origStart,$origTotal +$revStart,$revTotal @@")
        buffer.forEach { txt -> writer(txt) }
    }

    private fun getDeltaText(
        writer: (String) -> Unit,
        delta: Delta<String>,
    ) {
        for (line in delta.source.lines) writer("-$line")
        for (line in delta.target.lines) writer("+$line")
    }

    private fun writeOrNothing(
        writer: (String) -> Unit,
        str: String?,
    ) {
        str?.let { writer(it) }
    }
}
