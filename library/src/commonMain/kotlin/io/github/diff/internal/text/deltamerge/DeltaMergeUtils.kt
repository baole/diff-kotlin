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
package io.github.diff.internal.text.deltamerge

import io.github.diff.Chunk
import io.github.diff.Delta
import io.github.diff.DeltaType

/**
 * Provides utility features for merge inline deltas
 * todo refactoring needed
 */
internal object DeltaMergeUtils {
    fun mergeInlineDeltas(
        deltaMergeInfo: InlineDeltaMergeInfo,
        replaceEquality: (List<String>) -> Boolean,
    ): List<Delta<String>> {
        val originalDeltas = deltaMergeInfo.deltas
        if (originalDeltas.size < 2) {
            return originalDeltas
        }

        val newDeltas = mutableListOf<Delta<String>>()
        newDeltas.add(originalDeltas[0])
        for (i in 1 until originalDeltas.size) {
            val previousDelta = newDeltas[newDeltas.size - 1]
            val currentDelta = originalDeltas[i]

            val equalities =
                deltaMergeInfo.origList.subList(
                    previousDelta.source.position + previousDelta.source.size(),
                    currentDelta.source.position,
                )

            if (replaceEquality(equalities)) {
                // Merge the previous delta, the equality and the current delta into one
                // ChangeDelta and replace the previous delta by this new ChangeDelta.
                val allSourceLines = mutableListOf<String>()
                allSourceLines.addAll(previousDelta.source.lines)
                allSourceLines.addAll(equalities)
                allSourceLines.addAll(currentDelta.source.lines)

                val allTargetLines = mutableListOf<String>()
                allTargetLines.addAll(previousDelta.target.lines)
                allTargetLines.addAll(equalities)
                allTargetLines.addAll(currentDelta.target.lines)

                val replacement =
                    Delta(
                        DeltaType.CHANGE,
                        Chunk(previousDelta.source.position, allSourceLines),
                        Chunk(previousDelta.target.position, allTargetLines),
                    )

                newDeltas.removeAt(newDeltas.size - 1)
                newDeltas.add(replacement)
            } else {
                newDeltas.add(currentDelta)
            }
        }

        return newDeltas
    }
}
