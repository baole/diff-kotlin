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
import io.github.diff.internal.patch.VerifyChunk

internal class ChunkHelperImpl<T> : ChunkHelper<T> {
    override fun verifyChunk(
        chunk: Chunk<T>,
        target: List<T>,
        fuzz: Int,
        position: Int,
    ): VerifyChunk {
        val startIndex = fuzz
        val lastIndex = chunk.size() - fuzz
        val last = position + chunk.size() - 1

        if (position + fuzz > target.size || last - fuzz > target.size) {
            return VerifyChunk.POSITION_OUT_OF_TARGET
        }
        for (i in startIndex until lastIndex) {
            if (target[position + i] != chunk.lines[i]) {
                return VerifyChunk.CONTENT_DOES_NOT_MATCH_TARGET
            }
        }
        return VerifyChunk.OK
    }

    override fun verifyChunk(
        chunk: Chunk<T>,
        target: List<T>,
    ): VerifyChunk {
        return verifyChunk(chunk, target, 0, chunk.position)
    }
}
