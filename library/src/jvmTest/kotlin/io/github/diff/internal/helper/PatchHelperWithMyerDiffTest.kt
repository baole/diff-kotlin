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

import io.github.diff.utils.MergeConflictOutput
import io.github.diff.PatchFailedException
import io.github.diff.internal.patch.PatchImpl
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

internal class PatchHelperWithMyerDiffTest {
    val patchHelper: PatchHelper<String> = PatchHelperImpl()

    @Test
    fun testPatch_Change_withExceptionProcessor() {
        val from = mutableListOf("aaa", "bbb", "ccc", "ddd")
        val to = listOf("aaa", "bxb", "cxc", "ddd")
        val patch = patchHelper.generate(from, to)
        from[2] = "CDC"
        if (patch is PatchImpl) patch.withConflictOutput(MergeConflictOutput())
        try {
            val data = patchHelper.applyTo(patch, from)
            assertEquals(9, data.size)
            assertEquals(
                listOf("aaa", "<<<<<< HEAD", "bbb", "CDC", "======", "bbb", "ccc", ">>>>>>> PATCH", "ddd"),
                data,
            )
        } catch (e: PatchFailedException) {
            fail(e.message)
        }
    }

    @Test
    fun testPatchThreeWayIssue138() {
        val base = "Imagine there's no heaven".split("\\s+".toRegex())
        val left = "Imagine there's no HEAVEN".split("\\s+".toRegex())
        val right = "IMAGINE there's no heaven".split("\\s+".toRegex())
        val rightPatch = patchHelper.generate(base, right)
        if (rightPatch is PatchImpl) rightPatch.withConflictOutput(MergeConflictOutput())
        val applied = patchHelper.applyTo(rightPatch, left)
        assertEquals("IMAGINE there's no HEAVEN", applied.joinToString(" "))
    }
}
