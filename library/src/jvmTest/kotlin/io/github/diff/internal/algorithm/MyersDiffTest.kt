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
package io.github.diff.internal.algorithm

import io.github.diff.Chunk
import io.github.diff.Delta
import io.github.diff.DeltaType
import io.github.diff.DiffAlgorithm
import io.github.diff.DiffAlgorithmListener
import io.github.diff.Patch
import io.github.diff.internal.helper.PatchHelper
import io.github.diff.internal.helper.PatchHelperImpl
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

internal class MyersDiffTest {
    val helper: PatchHelper<String> = PatchHelperImpl()

    @Test
    fun testDiffMyersExample1Forward() {
        val original = listOf("A", "B", "C", "A", "B", "B", "A")
        val revised = listOf("C", "B", "A", "B", "A", "C")
        val patch: Patch<String> =
            helper.generate(
                original,
                revised,
                DiffAlgorithm.Factory.createMyers(),
            )
        assertNotNull(patch)

        val expected =
            listOf(
                Delta(
                    DeltaType.DELETE,
                    Chunk(0, listOf("A", "B")),
                    Chunk(0, emptyList<String>()),
                ),
                Delta(
                    DeltaType.INSERT,
                    Chunk(3, emptyList<String>()),
                    Chunk(1, listOf("B")),
                ),
                Delta(
                    DeltaType.DELETE,
                    Chunk(5, listOf("B")),
                    Chunk(4, emptyList<String>()),
                ),
                Delta(
                    DeltaType.INSERT,
                    Chunk(7, emptyList<String>()),
                    Chunk(5, listOf("C")),
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

    @Test
    fun testDiffMyersExample1ForwardWithListener() {
        val original = listOf("A", "B", "C", "A", "B", "B", "A")
        val revised = listOf("C", "B", "A", "B", "A", "C")
        val logdata = mutableListOf<String>()
        val patch =
            helper.generate(
                original,
                revised,
                DiffAlgorithm.Factory.createMyers(),
                progress =
                    object : DiffAlgorithmListener {
                        override fun diffStart() {
                            logdata.add("start")
                        }

                        override fun diffStep(
                            value: Int,
                            max: Int,
                        ) {
                            logdata.add("$value - $max")
                        }

                        override fun diffEnd() {
                            logdata.add("end")
                        }
                    },
            )
        assertNotNull(patch)

        val expected =
            listOf(
                Delta(
                    DeltaType.DELETE,
                    Chunk(0, listOf("A", "B")),
                    Chunk(0, emptyList<String>()),
                ),
                Delta(
                    DeltaType.INSERT,
                    Chunk(3, emptyList<String>()),
                    Chunk(1, listOf("B")),
                ),
                Delta(
                    DeltaType.DELETE,
                    Chunk(5, listOf("B")),
                    Chunk(4, emptyList<String>()),
                ),
                Delta(
                    DeltaType.INSERT,
                    Chunk(7, emptyList<String>()),
                    Chunk(5, listOf("C")),
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
        assertEquals(8, logdata.size)
    }
}
