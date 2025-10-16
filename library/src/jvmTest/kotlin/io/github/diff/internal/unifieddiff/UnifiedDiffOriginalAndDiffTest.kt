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

import io.github.diff.utils.TestConstants
import io.github.diff.utils.UnifiedDiffUtils
import java.io.File
import kotlin.test.Test
import kotlin.test.fail

class UnifiedDiffOriginalAndDiffTest {
    @Test
    fun testGenerateOriginalAndDiff() {
        try {
            val orig = fileToLines(TestConstants.MOCK_FOLDER + "original.txt")
            val rev = fileToLines(TestConstants.MOCK_FOLDER + "revised.txt")
            val originalAndDiff = UnifiedDiffUtils.generateOriginalAndDiff(orig, rev)
            println(originalAndDiff.joinToString("\n"))
        } catch (e: Exception) {
            fail(e.message)
        }
    }

    @Test
    fun testGenerateOriginalAndDiffFirstLineChange() {
        try {
            val orig = fileToLines(TestConstants.MOCK_FOLDER + "issue_170_original.txt")
            val rev = fileToLines(TestConstants.MOCK_FOLDER + "issue_170_revised.txt")
            val originalAndDiff = UnifiedDiffUtils.generateOriginalAndDiff(orig, rev)
            println(originalAndDiff.joinToString("\n"))
        } catch (e: Exception) {
            fail(e.message)
        }
    }

    companion object Companion {
        fun fileToLines(filename: String): List<String> = File(filename).readLines()
    }
}
