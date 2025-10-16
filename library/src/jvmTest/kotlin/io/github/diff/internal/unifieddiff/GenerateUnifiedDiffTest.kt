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
import io.github.diff.internal.helper.PatchHelper
import io.github.diff.internal.helper.PatchHelperImpl
import io.github.diff.utils.TestConstants
import io.github.diff.utils.UnifiedDiffUtils
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.test.fail

internal class GenerateUnifiedDiffTest {
    val patchHelper: PatchHelper<String> = PatchHelperImpl()

    companion object {
        fun fileToLines(filename: String): MutableList<String> = File(filename).readLines().toMutableList()
    }

    @Test
    fun testGenerateUnified() {
        val origLines = fileToLines(TestConstants.MOCK_FOLDER + "original.txt")
        val revLines = fileToLines(TestConstants.MOCK_FOLDER + "revised.txt")
        verify(origLines, revLines, "original.txt", "revised.txt")
    }

    @Test
    fun testGenerateUnifiedWithOneDelta() {
        val origLines = fileToLines(TestConstants.MOCK_FOLDER + "one_delta_test_original.txt")
        val revLines = fileToLines(TestConstants.MOCK_FOLDER + "one_delta_test_revised.txt")
        verify(origLines, revLines, "one_delta_test_original.txt", "one_delta_test_revised.txt")
    }

    @Test
    fun testGenerateUnifiedDiffWithoutAnyDeltas() {
        val test = listOf("abc")
        val testRevised = listOf("abc2")
        val patch = patchHelper.generate(test, testRevised)
        val unifiedDiffTxt = UnifiedDiffUtils.generateUnifiedDiff("abc1", "abc2", test, patch, 0).joinToString("\n")
        println(unifiedDiffTxt)
        assertTrue(unifiedDiffTxt.contains("--- abc1"))
        assertTrue(unifiedDiffTxt.contains("+++ abc2"))
    }

    @Test
    fun testDiff_Issue10() {
        val baseLines = fileToLines(TestConstants.MOCK_FOLDER + "issue10_base.txt")
        val patchLines = fileToLines(TestConstants.MOCK_FOLDER + "issue10_patch.txt")
        val p = UnifiedDiffUtils.parseUnifiedDiff(patchLines)
        try {
            patchHelper.applyTo(p, baseLines)
        } catch (e: PatchFailedException) {
            fail(e.message ?: "Patch failed")
        }
    }

    @Test
    fun testPatchWithNoDeltas() {
        val lines1 = fileToLines(TestConstants.MOCK_FOLDER + "issue11_1.txt")
        val lines2 = fileToLines(TestConstants.MOCK_FOLDER + "issue11_2.txt")
        verify(lines1, lines2, "issue11_1.txt", "issue11_2.txt")
    }

    @Test
    fun testDiff5() {
        val lines1 = fileToLines(TestConstants.MOCK_FOLDER + "5A.txt")
        val lines2 = fileToLines(TestConstants.MOCK_FOLDER + "5B.txt")
        verify(lines1, lines2, "5A.txt", "5B.txt")
    }

    @Test
    fun testDiffWithHeaderLineInText() {
        val original = mutableListOf("test line1", "test line2", "test line 4", "test line 5")
        val revised = mutableListOf("test line1", "test line2", "@@ -2,6 +2,7 @@", "test line 4", "test line 5")
        val patch = patchHelper.generate(original, revised)
        val udiff = UnifiedDiffUtils.generateUnifiedDiff("original", "revised", original, patch, 10)
        UnifiedDiffUtils.parseUnifiedDiff(udiff)
    }

    @Test
    fun testNewFileCreation() {
        val original = emptyList<String>()
        val revised = listOf("line1", "line2")
        val patch = patchHelper.generate(original, revised)
        val udiff = UnifiedDiffUtils.generateUnifiedDiff(null, "revised", original, patch, 10)
        assertEquals("--- /dev/null", udiff[0])
        assertEquals("+++ revised", udiff[1])
        assertEquals("@@ -0,0 +1,2 @@", udiff[2])
        UnifiedDiffUtils.parseUnifiedDiff(udiff)
    }

    @Test
    fun testChangePosition() {
        val patchLines = fileToLines(TestConstants.MOCK_FOLDER + "issue89_patch.txt")
        val patch = UnifiedDiffUtils.parseUnifiedDiff(patchLines)
        val realRemoveListOne = listOf(3)
        val realAddListOne = listOf(3, 7, 8, 9, 10, 11, 12, 13, 14)
        validateChangePosition(patch, 0, realRemoveListOne, realAddListOne)
        val realRemoveListTwo = emptyList<Int>()
        val realAddListTwo = listOf(27, 28)
        validateChangePosition(patch, 1, realRemoveListTwo, realAddListTwo)
    }

    private fun validateChangePosition(
        patch: Patch<String>,
        index: Int,
        realRemoveList: List<Int>,
        realAddList: List<Int>,
    ) {
        val delta = patch.getDeltas()[index]
        val originChunk = delta.source
        val removeList = originChunk.changePosition ?: emptyList()
        assertEquals(realRemoveList.size, removeList.size)
        for (ele in realRemoveList) {
            assertTrue(removeList.contains(ele))
        }
        for (ele in removeList) {
            assertTrue(realRemoveList.contains(ele))
        }
        val targetChunk = delta.target
        val addList = targetChunk.changePosition ?: emptyList()
        assertEquals(realAddList.size, addList.size)
        for (ele in realAddList) {
            assertTrue(addList.contains(ele))
        }
        for (ele in addList) {
            assertTrue(realAddList.contains(ele))
        }
    }

    private fun verify(
        origLines: List<String>,
        revLines: List<String>,
        originalFile: String,
        revisedFile: String,
    ) {
        val patch = patchHelper.generate(origLines, revLines)
        val unifiedDiff = UnifiedDiffUtils.generateUnifiedDiff(originalFile, revisedFile, origLines, patch, 10)
        println(unifiedDiff.joinToString("\n"))
        val fromUnifiedPatch = UnifiedDiffUtils.parseUnifiedDiff(unifiedDiff)
        try {
            val patchedLines = patchHelper.applyTo(fromUnifiedPatch, origLines)
            assertEquals(revLines.size, patchedLines.size)
            for (i in revLines.indices) {
                val l1 = revLines[i]
                val l2 = patchedLines[i]
                assertEquals(
                    l1,
                    l2,
                    "Line ${i + 1} of the patched file did not match the revised original"
                )
            }
        } catch (e: PatchFailedException) {
            fail(e.message ?: "Patch failed")
        }
    }

    @Test
    fun testFailingPatchByException() {
        val baseLines = fileToLines(TestConstants.MOCK_FOLDER + "issue10_base.txt")
        val patchLines = fileToLines(TestConstants.MOCK_FOLDER + "issue10_patch.txt")
        val p = UnifiedDiffUtils.parseUnifiedDiff(patchLines)
        baseLines[40] = baseLines[40] + " corrupted "
        assertFailsWith<PatchFailedException> {
            patchHelper.applyTo(p, baseLines)
        }
    }

    @Test
    fun testWrongContextLength() {
        val filename = "issue_119_original.txt"
        println(File(".").absolutePath)
        val original =
            fileToLines(File("").absolutePath + "/src/jvmTest/resources/io/github/diff/text/issue_119_original.txt")
        val revised =
            fileToLines(File(".").absolutePath + "/src/jvmTest/resources/io/github/diff/text/issue_119_revised.txt")
        val patch = patchHelper.generate(original, revised)
        val udiff = UnifiedDiffUtils.generateUnifiedDiff("a/$filename", "b/$filename", original, patch, 3)
        assertTrue(udiff.contains("@@ -1,4 +1,4 @@"))
    }
}
