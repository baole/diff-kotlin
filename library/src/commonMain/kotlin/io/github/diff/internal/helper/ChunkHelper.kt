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
import io.github.diff.PatchFailedException
import io.github.diff.internal.patch.VerifyChunk

internal interface ChunkHelper<T> {
    /**
     * Verifies that this chunk's saved text matches the corresponding text in
     * the given sequence.
     *
     * @param target the sequence to verify against.
     * @throws PatchFailedException
     */
    fun verifyChunk(
        chunk: Chunk<T>,
        target: List<T>,
    ): VerifyChunk

    /**
     * Verifies that this chunk's saved text matches the corresponding text in
     * the given sequence.
     *
     * @param target the sequence to verify against.
     * @param fuzz the count of ignored prefix/suffix
     * @param position the position of target
     * @throws PatchFailedException
     */
    @Throws(PatchFailedException::class)
    fun verifyChunk(
        chunk: Chunk<T>,
        target: List<T>,
        fuzz: Int,
        position: Int,
    ): VerifyChunk
}
