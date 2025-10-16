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

import io.github.diff.DiffAlgorithm
import io.github.diff.PatchFailedException
import io.github.diff.internal.algorithm.MyersDiff
import io.github.diff.internal.algorithm.MyersDiffWithLinearSpace
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

internal class PatchHelperWithAllDiffAlgorithmsTest {
    val patchHelper: PatchHelper<String> = PatchHelperImpl()

    companion object Companion {
        internal fun provideAlgorithms(): List<DiffAlgorithm<String>> =
            listOf(
                DiffAlgorithm.Factory.createMyers(),
                DiffAlgorithm.Factory.createMyersLinearSpace(),
            )
    }

    @Test
    fun testPatch_Insert() {
        for (algorithm in provideAlgorithms()) {
            val from = listOf("hhh")
            val to = listOf("hhh", "jjj", "kkk", "lll")
            val patch = patchHelper.generate(from, to, algorithm)
            try {
                val result = patchHelper.applyTo(patch, from)
                assertEquals(to, result)
            } catch (e: PatchFailedException) {
                fail(e.message)
            }
        }
    }

    @Test
    fun testPatch_Delete() {
        for (algorithm in provideAlgorithms()) {
            val from = listOf("ddd", "fff", "ggg", "hhh")
            val to = listOf("ggg")
            val patch = patchHelper.generate(from, to, algorithm)
            try {
                val result = patchHelper.applyTo(patch, from)
                assertEquals(to, result)
            } catch (e: PatchFailedException) {
                fail(e.message)
            }
        }
    }

    @Test
    fun testPatch_Change() {
        for (algorithm in provideAlgorithms()) {
            val from = listOf("aaa", "bbb", "ccc", "ddd")
            val to = listOf("aaa", "bxb", "cxc", "ddd")
            val patch = patchHelper.generate(from, to, algorithm = algorithm)
            try {
                val result = patchHelper.applyTo(patch, from)
                assertEquals(to, result)
            } catch (e: PatchFailedException) {
                fail(e.message)
            }
        }
    }
}
