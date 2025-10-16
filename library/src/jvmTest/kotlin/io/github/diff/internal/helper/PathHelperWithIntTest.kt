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
import kotlin.test.Test
import kotlin.test.assertEquals

internal class PathHelperWithIntTest {
    val helper: PatchHelper<Int> = PatchHelperImpl()

    @Test
    fun testDiffIntegerList() {
        val original = listOf(1, 2, 3, 4, 5)
        val revised = listOf(2, 3, 4, 6)
        val patch = helper.generate(original, revised, algorithm = null, progress = null, false)
        assertEquals(2, patch.getDeltas().size)

        val expected =
            listOf(
                Delta(
                    DeltaType.DELETE,
                    Chunk(0, listOf(1)),
                    Chunk(0, emptyList<Int>()),
                ),
                Delta(
                    DeltaType.CHANGE,
                    Chunk(4, listOf(5)),
                    Chunk(3, listOf(6)),
                ),
            )

        expected.forEach {
            assertEquals(
                it,
                patch.getDeltas()[expected.indexOf(it)],
                "at index ${expected.indexOf(it)}"
            )
        }
        assertEquals(expected.size, patch.getDeltas().size)
    }
}
