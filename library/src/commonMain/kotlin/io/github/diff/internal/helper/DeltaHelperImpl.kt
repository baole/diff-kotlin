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
package io.github.diff.internal.helper

import io.github.diff.Chunk
import io.github.diff.Delta
import io.github.diff.DeltaType
import io.github.diff.PatchFailedException
import io.github.diff.internal.patch.VerifyChunk

internal class DeltaHelperImpl<T>(
    private val chunkHelper: ChunkHelper<T> = ChunkHelperImpl(),
) : DeltaHelper<T> {
    @Throws(PatchFailedException::class)
    override fun applyTo(
        delta: Delta<T>,
        target: MutableList<T>,
    ) {
        when (delta.type) {
            DeltaType.EQUAL -> {}
            DeltaType.DELETE -> {
                val position = delta.source.position
                val size = delta.source.size()
                repeat(size) {
                    target.removeAt(position)
                }
            }

            DeltaType.INSERT -> {
                val position = delta.source.position
                val lines = delta.target.lines
                for (i in lines.indices) {
                    target.add(position + i, lines[i])
                }
            }

            DeltaType.CHANGE -> {
                val position = delta.source.position
                val size = delta.source.size()
                repeat(size) {
                    target.removeAt(position)
                }
                var i = 0
                for (line in delta.target.lines) {
                    target.add(position + i, line)
                    i++
                }
            }
        }
    }

    @Throws(PatchFailedException::class)
    override fun restore(
        delta: Delta<T>,
        target: MutableList<T>,
    ) {
        when (delta.type) {
            DeltaType.EQUAL -> {}
            DeltaType.DELETE -> {
                val position = delta.target.position
                val lines = delta.source.lines
                for (i in lines.indices) {
                    target.add(position + i, lines[i])
                }
            }

            DeltaType.INSERT -> {
                val position = delta.target.position
                val size = delta.target.size()
                repeat(size) {
                    target.removeAt(position)
                }
            }

            DeltaType.CHANGE -> {
                val position = delta.target.position
                val size = delta.target.size()
                repeat(size) {
                    target.removeAt(position)
                }
                var i = 0
                for (line in delta.source.lines) {
                    target.add(position + i, line)
                    i++
                }
            }
        }
    }

    @Throws(PatchFailedException::class)
    override fun applyFuzzyToAt(
        delta: Delta<T>,
        target: MutableList<T>,
        fuzz: Int,
        position: Int,
    ) {
        when (delta.type) {
            DeltaType.EQUAL -> {
            }

            DeltaType.DELETE -> {}
            DeltaType.INSERT -> {
            }

            DeltaType.CHANGE -> {
                val size = delta.source.size()
                for (i in fuzz until size - fuzz) {
                    target.removeAt(position + fuzz)
                }

                var i = fuzz
                for (line in delta.target.lines.subList(fuzz, delta.target.size() - fuzz)) {
                    target.add(position + i, line)
                    i++
                }
            }
        }
    }

    @Throws(PatchFailedException::class)
    override fun withChunks(
        delta: Delta<T>,
        original: Chunk<T>,
        revised: Chunk<T>,
    ): Delta<T> {
        return Delta(delta.type, original, revised)
    }

    /**
     * Verify the chunk of this delta, to fit the target.
     * @param target
     * @throws PatchFailedException
     */
    @Throws(PatchFailedException::class)
    private fun verifyChunkToFitTarget(
        delta: Delta<T>,
        target: List<T>,
    ): VerifyChunk {
        return chunkHelper.verifyChunk(delta.source, target)
    }

    @Throws(PatchFailedException::class)
    override fun verifyAndApplyTo(
        delta: Delta<T>,
        target: MutableList<T>,
    ): VerifyChunk {
        val verify = verifyChunkToFitTarget(delta, target)
        if (verify == VerifyChunk.OK) {
            applyTo(delta, target)
        }
        return verify
    }
}
