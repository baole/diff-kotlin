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
package io.github.diff.internal.unifieddiff

import io.github.diff.Patch
import io.github.diff.PatchFailedException
import io.github.diff.utils.TestConstants
import io.github.diff.internal.helper.PatchHelper
import io.github.diff.internal.helper.PatchHelperImpl
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

internal class UnifiedDiffRoundTripTest {
    companion object {
        fun fileToLines(filename: String): List<String> = File(filename).readLines()
    }

    val patchHelper: PatchHelper<String> = PatchHelperImpl()

    @Test
    fun testGenerateUnified() {
        val orig = fileToLines(TestConstants.MOCK_FOLDER + "original.txt")
        val rev = fileToLines(TestConstants.MOCK_FOLDER + "revised.txt")
        verify(orig, rev, "original.txt", "revised.txt")
    }

    @Test
    fun testGenerateUnifiedWithOneDelta() {
        val orig = fileToLines(TestConstants.MOCK_FOLDER + "one_delta_test_original.txt")
        val rev = fileToLines(TestConstants.MOCK_FOLDER + "one_delta_test_revised.txt")
        verify(orig, rev, "one_delta_test_original.txt", "one_delta_test_revised.txt")
    }

    @Test
    fun testDiff_Issue10() {
        val base = fileToLines(TestConstants.MOCK_FOLDER + "issue10_base.txt")
        val patchLines = fileToLines(TestConstants.MOCK_FOLDER + "issue10_patch.txt")
        val unifiedDiff = UnifiedDiffReader.parseUnifiedDiff(patchLines.joinToString("\n"))
        val p: Patch<String> = unifiedDiff.files[0].patch
        try {
            patchHelper.applyTo(p, base)
        } catch (e: PatchFailedException) {
            fail(e.message)
        }
    }

    @Test
    fun testDiff5() {
        val lines1 = fileToLines(TestConstants.MOCK_FOLDER + "5A.txt")
        val lines2 = fileToLines(TestConstants.MOCK_FOLDER + "5B.txt")
        verify(lines1, lines2, "5A.txt", "5B.txt")
    }

    private fun verify(
        orig: List<String>,
        rev: List<String>,
        originalFile: String,
        revisedFile: String,
    ) {
        val patch: Patch<String> = patchHelper.generate(orig, rev)
        val diff = UnifiedDiff.from("header", "tail", UnifiedDiffFile.from(originalFile, revisedFile, patch))
        val provider: (String?) -> List<String> = { _: String? -> orig }
        val unifiedDiffText =
            UnifiedDiffWriter.writeToString(
                diff = diff,
                originalLinesProvider = provider,
                contextSize = 10,
            )
        val unified = UnifiedDiffReader.parseUnifiedDiff(unifiedDiffText)
        try {
            val patched = unified.applyPatchTo({ it == originalFile }, orig)
            assertEquals(rev.size, patched.size)
            for (i in rev.indices) {
                if (rev[i] != patched[i]) fail("Line ${i + 1} of the patched file did not match the revised original")
            }
        } catch (e: PatchFailedException) {
            fail(e.message)
        }
    }
}
