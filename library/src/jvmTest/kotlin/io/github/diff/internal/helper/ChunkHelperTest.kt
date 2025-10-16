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
import kotlin.test.Test
import kotlin.test.assertEquals

internal class ChunkHelperTest {
    val helper: ChunkHelper<Char> = ChunkHelperImpl()

    @Test
    fun verifyChunk() {
        val chunk = Chunk(7, toCharList("test"))
        // normal check
        assertEquals(VerifyChunk.OK, helper.verifyChunk(chunk, toCharList("prefix test suffix")))
        assertEquals(
            VerifyChunk.CONTENT_DOES_NOT_MATCH_TARGET,
            helper.verifyChunk(chunk, toCharList("prefix  es  suffix"), 0, 7),
        )
        // position
        assertEquals(
            VerifyChunk.OK,
            helper.verifyChunk(chunk, toCharList("short test suffix"), 0, 6)
        )
        assertEquals(
            VerifyChunk.OK,
            helper.verifyChunk(chunk, toCharList("loonger test suffix"), 0, 8)
        )
        assertEquals(
            VerifyChunk.CONTENT_DOES_NOT_MATCH_TARGET,
            helper.verifyChunk(chunk, toCharList("prefix test suffix"), 0, 6),
        )
        assertEquals(
            VerifyChunk.CONTENT_DOES_NOT_MATCH_TARGET,
            helper.verifyChunk(chunk, toCharList("prefix test suffix"), 0, 8),
        )
        // fuzz
        assertEquals(
            VerifyChunk.OK,
            helper.verifyChunk(chunk, toCharList("prefix test suffix"), 1, 7)
        )
        assertEquals(
            VerifyChunk.OK,
            helper.verifyChunk(chunk, toCharList("prefix  es  suffix"), 1, 7)
        )
        assertEquals(
            VerifyChunk.CONTENT_DOES_NOT_MATCH_TARGET,
            helper.verifyChunk(chunk, toCharList("prefix      suffix"), 1, 7),
        )
    }

    private fun toCharList(str: String): List<Char> = str.toCharArray().toList()
}
