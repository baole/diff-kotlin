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
import io.github.diff.internal.algorithm.MyersDiffWithLinearSpace
import io.github.diff.internal.patch.PatchImpl
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

internal class PatchHelperWithMyersDiffWithLinearSpaceTest {
    private val deltaFrom = listOf("aaa", "bbb", "ccc", "ddd", "eee", "fff")
    private val deltaTo = listOf("aaa", "bbb", "cxc", "dxd", "eee", "fff")
    val patchHelper: PatchHelper<String> = PatchHelperImpl()

    @Test
    fun testPatch_Insert() {
        val from = listOf("hhh")
        val to = listOf("hhh", "jjj", "kkk", "lll")
        val patch = patchHelper.generate(from, to, MyersDiffWithLinearSpace<String>())
        val result = patchHelper.applyTo(patch, from)
        assertEquals(to, result)
    }

    @Test
    fun testPatch_Delete() {
        val from = listOf("ddd", "fff", "ggg", "hhh")
        val to = listOf("ggg")
        val patch = patchHelper.generate(from, to, MyersDiffWithLinearSpace<String>())
        val result = patchHelper.applyTo(patch, from)
        assertEquals(to, result)
    }

    @Test
    fun testPatch_Change() {
        val from = listOf("aaa", "bbb", "ccc", "ddd")
        val to = listOf("aaa", "bxb", "cxc", "ddd")
        val patch = patchHelper.generate(from, to, MyersDiffWithLinearSpace<String>())
        val result = patchHelper.applyTo(patch, from)
        assertEquals(to, result)
    }

    private fun intRange(count: Int): List<String> = List(count) { it.toString() }

    private fun join(vararg lists: List<String>): List<String> = lists.flatMap { it }

    private data class FuzzyApplyTestPair(
        val from: List<String>,
        val to: List<String>,
        val requiredFuzz: Int,
    )

    private val fuzzyApplyTestPairs =
        listOf(
            FuzzyApplyTestPair(
                listOf("aaa", "bbb", "ccc", "ddd", "eee", "fff"),
                listOf("aaa", "bbb", "cxc", "dxd", "eee", "fff"),
                0,
            ),
            FuzzyApplyTestPair(
                listOf("axa", "bbb", "ccc", "ddd", "eee", "fff"),
                listOf("axa", "bbb", "cxc", "dxd", "eee", "fff"),
                1,
            ),
            FuzzyApplyTestPair(
                listOf("aaa", "bbb", "ccc", "ddd", "eee", "fxf"),
                listOf("aaa", "bbb", "cxc", "dxd", "eee", "fxf"),
                1,
            ),
            FuzzyApplyTestPair(
                listOf("axa", "bbb", "ccc", "ddd", "eee", "fxf"),
                listOf("axa", "bbb", "cxc", "dxd", "eee", "fxf"),
                1,
            ),
            FuzzyApplyTestPair(
                listOf("aaa", "bxb", "ccc", "ddd", "eee", "fff"),
                listOf("aaa", "bxb", "cxc", "dxd", "eee", "fff"),
                2,
            ),
            FuzzyApplyTestPair(
                listOf("axa", "bxb", "ccc", "ddd", "eee", "fff"),
                listOf("axa", "bxb", "cxc", "dxd", "eee", "fff"),
                2,
            ),
            FuzzyApplyTestPair(
                listOf("aaa", "bbb", "ccc", "ddd", "exe", "fff"),
                listOf("aaa", "bbb", "cxc", "dxd", "exe", "fff"),
                2,
            ),
            FuzzyApplyTestPair(
                listOf("axa", "bbb", "ccc", "ddd", "exe", "fff"),
                listOf("axa", "bbb", "cxc", "dxd", "exe", "fff"),
                2,
            ),
            FuzzyApplyTestPair(
                listOf("aaa", "bxb", "ccc", "ddd", "exe", "fff"),
                listOf("aaa", "bxb", "cxc", "dxd", "exe", "fff"),
                2,
            ),
            FuzzyApplyTestPair(
                listOf("axa", "bxb", "ccc", "ddd", "exe", "fff"),
                listOf("axa", "bxb", "cxc", "dxd", "exe", "fff"),
                2,
            ),
            FuzzyApplyTestPair(
                listOf("aaa", "bxb", "ccc", "ddd", "eee", "fxf"),
                listOf("aaa", "bxb", "cxc", "dxd", "eee", "fxf"),
                2,
            ),
            FuzzyApplyTestPair(
                listOf("axa", "bxb", "ccc", "ddd", "eee", "fxf"),
                listOf("axa", "bxb", "cxc", "dxd", "eee", "fxf"),
                2,
            ),
            FuzzyApplyTestPair(
                listOf("aaa", "bbb", "ccc", "ddd", "exe", "fxf"),
                listOf("aaa", "bbb", "cxc", "dxd", "exe", "fxf"),
                2,
            ),
            FuzzyApplyTestPair(
                listOf("axa", "bbb", "ccc", "ddd", "exe", "fxf"),
                listOf("axa", "bbb", "cxc", "dxd", "exe", "fxf"),
                2,
            ),
            FuzzyApplyTestPair(
                listOf("aaa", "bxb", "ccc", "ddd", "exe", "fxf"),
                listOf("aaa", "bxb", "cxc", "dxd", "exe", "fxf"),
                2,
            ),
            FuzzyApplyTestPair(
                listOf("axa", "bxb", "ccc", "ddd", "exe", "fxf"),
                listOf("axa", "bxb", "cxc", "dxd", "exe", "fxf"),
                2,
            ),
            FuzzyApplyTestPair(
                listOf("aaa", "bbb", "czc", "dzd", "eee", "fff"),
                listOf("aaa", "bbb", "czc", "dzd", "eee", "fff"),
                3,
            ),
            FuzzyApplyTestPair(
                listOf("axa", "bbb", "czc", "dzd", "eee", "fff"),
                listOf("axa", "bbb", "czc", "dzd", "eee", "fff"),
                3,
            ),
            FuzzyApplyTestPair(
                listOf("aaa", "bxb", "czc", "dzd", "eee", "fff"),
                listOf("aaa", "bxb", "czc", "dzd", "eee", "fff"),
                3,
            ),
            FuzzyApplyTestPair(
                listOf("axa", "bxb", "czc", "dzd", "eee", "fff"),
                listOf("axa", "bxb", "czc", "dzd", "eee", "fff"),
                3,
            ),
            FuzzyApplyTestPair(
                listOf("aaa", "bbb", "czc", "dzd", "exe", "fff"),
                listOf("aaa", "bbb", "czc", "dzd", "exe", "fff"),
                3,
            ),
            FuzzyApplyTestPair(
                listOf("axa", "bbb", "czc", "dzd", "exe", "fff"),
                listOf("axa", "bbb", "czc", "dzd", "exe", "fff"),
                3,
            ),
            FuzzyApplyTestPair(
                listOf("aaa", "bxb", "czc", "dzd", "exe", "fff"),
                listOf("aaa", "bxb", "czc", "dzd", "exe", "fff"),
                3,
            ),
            FuzzyApplyTestPair(
                listOf("axa", "bxb", "czc", "dzd", "exe", "fff"),
                listOf("axa", "bxb", "cxc", "dxd", "exe", "fff"),
                3,
            ),
        )

    @Test
    @Ignore("Fuzzy patch apply not implemented yet")
    fun fuzzyApply() {
        val patch = PatchImpl<String>()
        patch.addDelta(Delta(DeltaType.CHANGE, Chunk(6, deltaFrom), Chunk(6, deltaTo)))
        val moves = arrayOf(intRange(6), intRange(3), intRange(9), intRange(0))
        for (pair in fuzzyApplyTestPairs) {
            for (move in moves) {
                val from = join(move, pair.from)
                val to = join(move, pair.to)
                for (i in 0 until pair.requiredFuzz) {
                    val maxFuzz = i
                    assertFailsWith<PatchFailedException> {
                        patchHelper.applyFuzzy(
                            patch,
                            from,
                            maxFuzz,
                        )
                    }
                }
                for (i in pair.requiredFuzz until 4) {
                    val maxFuzz = i
                    assertEquals(to, patchHelper.applyFuzzy(patch, from, maxFuzz), "with $maxFuzz")
                }
            }
        }
    }

    @Test
    fun fuzzyApplyTwoSideBySidePatches() {
        val patch = PatchImpl<String>()
        patch.addDelta(Delta(DeltaType.CHANGE, Chunk(0, deltaFrom), Chunk(0, deltaTo)))
        patch.addDelta(Delta(DeltaType.CHANGE, Chunk(6, deltaFrom), Chunk(6, deltaTo)))
        assertEquals(join(deltaTo, deltaTo), patchHelper.applyFuzzy(patch, join(deltaFrom, deltaFrom), 0))
    }

    @Test
    fun fuzzyApplyToNearest() {
        val patch = PatchImpl<String>()
        patch.addDelta(Delta(DeltaType.CHANGE, Chunk(0, deltaFrom), Chunk(0, deltaTo)))
        patch.addDelta(Delta(DeltaType.CHANGE, Chunk(10, deltaFrom), Chunk(10, deltaTo)))
        assertEquals(
            join(deltaTo, deltaFrom, deltaTo),
            patchHelper.applyFuzzy(patch, join(deltaFrom, deltaFrom, deltaFrom), 0),
        )
        assertEquals(
            join(intRange(1), deltaTo, deltaFrom, deltaTo),
            patchHelper.applyFuzzy(patch, join(intRange(1), deltaFrom, deltaFrom, deltaFrom), 0),
        )
    }
}
