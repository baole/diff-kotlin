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
package io.github.diff.internal.text

import io.github.diff.utils.TestConstants
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DiffRowGeneratorTest {
    @Test
    fun testGenerator_Default() {
        val first = "anything \n \nother"
        val second = "anything\n\nother"
        val generator = DiffRowGenerator.create().columnWidth(Int.MAX_VALUE).build()
        val rows = generator.generateDiffRows(split(first), split(second))
        print(rows)
        assertEquals(3, rows.size)
    }

    @Test
    fun testNormalize_List() {
        val generator = DiffRowGenerator.create().build()
        assertEquals(listOf("    test"), generator.normalizeLines(listOf("\ttest")))
    }

    @Test
    fun testGenerator_Default2() {
        val first = "anything \n \nother"
        val second = "anything\n\nother"
        val generator = DiffRowGenerator.create().columnWidth(0).build()
        val rows = generator.generateDiffRows(split(first), split(second))
        print(rows)
        assertEquals(3, rows.size)
    }

    @Test
    fun testGenerator_InlineDiff() {
        val first = "anything \n \nother"
        val second = "anything\n\nother"
        val generator = DiffRowGenerator.create().showInlineDiffs(true).columnWidth(Int.MAX_VALUE).build()
        val rows = generator.generateDiffRows(split(first), split(second))
        print(rows)
        assertEquals(3, rows.size)
        assertTrue(rows[0].oldLine.contains("<span"))
    }

    @Test
    fun testGenerator_IgnoreWhitespaces() {
        val first = "anything \n \nother\nmore lines"
        val second = "anything\n\nother\nsome more lines"
        val generator = DiffRowGenerator.create().ignoreWhiteSpaces(true).columnWidth(Int.MAX_VALUE).build()
        val rows = generator.generateDiffRows(split(first), split(second))
        print(rows)
        assertEquals(4, rows.size)
        assertEquals(DiffRow.Tag.EQUAL, rows[0].tag)
        assertEquals(DiffRow.Tag.EQUAL, rows[1].tag)
        assertEquals(DiffRow.Tag.EQUAL, rows[2].tag)
        assertEquals(DiffRow.Tag.CHANGE, rows[3].tag)
    }

    @Test
    fun testGeneratorWithWordWrap() {
        val first = "anything \n \nother"
        val second = "anything\n\nother"
        val generator = DiffRowGenerator.create().columnWidth(5).build()
        val rows = generator.generateDiffRows(split(first), split(second))
        print(rows)
        assertEquals(3, rows.size)
        assertEquals("[CHANGE,anyth<br/>ing ,anyth<br/>ing]", rows[0].toString())
        assertEquals("[CHANGE, ,]", rows[1].toString())
        assertEquals("[EQUAL,other,other]", rows[2].toString())
    }

    @Test
    fun testGeneratorWithMerge() {
        val first = "anything \n \nother"
        val second = "anything\n\nother"
        val generator = DiffRowGenerator.create().showInlineDiffs(true).mergeOriginalRevised(true).build()
        val rows = generator.generateDiffRows(split(first), split(second))
        print(rows)
        assertEquals(3, rows.size)
        assertEquals("[CHANGE,anything<span class=\"editOldInline\"> </span>,anything]", rows[0].toString())
        assertEquals("[CHANGE,<span class=\"editOldInline\"> </span>,]", rows[1].toString())
        assertEquals("[EQUAL,other,other]", rows[2].toString())
    }

    @Test
    fun testGeneratorWithMerge2() {
        val generator = DiffRowGenerator.create().showInlineDiffs(true).mergeOriginalRevised(true).build()
        val rows = generator.generateDiffRows(listOf("Test"), listOf("ester"))
        print(rows)
        assertEquals(1, rows.size)
        assertEquals(
            "[CHANGE,<span class=\"editOldInline\">T</span>est<span class=\"editNewInline\">er</span>,ester]",
            rows[0].toString(),
        )
    }

    @Test
    fun testGeneratorWithMerge3() {
        val first = "test\nanything \n \nother"
        val second = "anything\n\nother\ntest\ntest2"
        val generator = DiffRowGenerator.create().showInlineDiffs(true).mergeOriginalRevised(true).build()
        val rows = generator.generateDiffRows(split(first), split(second))
        print(rows)
        assertEquals(6, rows.size)
    }

    @Test
    fun testGeneratorWithMergeByWord4() {
        val generator =
            DiffRowGenerator.create().showInlineDiffs(
                true,
            ).mergeOriginalRevised(true).inlineDiffByWord(true).build()
        val rows = generator.generateDiffRows(listOf("Test"), listOf("ester"))
        print(rows)
        assertEquals(1, rows.size)
        assertEquals(
            "[CHANGE,<span class=\"editOldInline\">Test</span><span class=\"editNewInline\">ester</span>,ester]",
            rows[0].toString(),
        )
    }

    @Test
    fun testGeneratorWithMergeByWord5() {
        val generator =
            DiffRowGenerator.create().showInlineDiffs(
                true,
            ).mergeOriginalRevised(true).inlineDiffByWord(true).columnWidth(80).build()
        val rows = generator.generateDiffRows(listOf("Test feature"), listOf("ester feature best"))
        print(rows)
        assertEquals(1, rows.size)
        assertEquals(
            "[CHANGE,<span class=\"editOldInline\">Test</span><span class=\"editNewInline\">ester</span> <br/>feature<span class=\"editNewInline\"> best</span>,ester feature best]",
            rows[0].toString(),
        )
    }

    @Test
    fun testSplitString() {
        val list = DiffRowGenerator.splitStringPreserveDelimiter("test,test2", Regex(","))
        assertEquals(3, list.size)
        assertEquals("[test, ,, test2]", list.toString())
    }

    @Test
    fun testSplitString2() {
        val list = DiffRowGenerator.splitStringPreserveDelimiter("test , test2", Regex(","))
        assertEquals(5, list.size)
        assertEquals("[test,  , ,,  , test2]", list.toString())
    }

    @Test
    fun testSplitString3() {
        val list = DiffRowGenerator.splitStringPreserveDelimiter("test,test2,", Regex(","))
        assertEquals(4, list.size)
        assertEquals("[test, ,, test2, ,]", list.toString())
    }

    @Test
    fun testGeneratorExample1() {
        val generator =
            DiffRowGenerator.create().showInlineDiffs(
                true,
            ).mergeOriginalRevised(true).inlineDiffByWord(true).oldTag {
                "~"
            }.newTag { "**" }.build()
        val rows =
            generator.generateDiffRows(
                listOf("This is a test senctence."),
                listOf("This is a test for diffutils."),
            )
        assertEquals(1, rows.size)
        assertEquals("This is a test ~senctence~**for diffutils**.", rows[0].oldLine)
    }

    @Test
    fun testGeneratorExample2() {
        val generator =
            DiffRowGenerator.create().showInlineDiffs(true).inlineDiffByWord(true).oldTag {
                "~"
            }.newTag { "**" }.build()
        val rows =
            generator.generateDiffRows(
                listOf("This is a test senctence.", "This is the second line.", "And here is the finish."),
                listOf("This is a test for diffutils.", "This is the second line."),
            )
        assertEquals(3, rows.size)
        assertEquals("This is a test ~senctence~.", rows[0].oldLine)
        assertEquals("This is a test **for diffutils**.", rows[0].newLine)
    }

    @Test
    fun testGeneratorUnchanged() {
        val first = "anything \n \nother"
        val second = "anything\n\nother"
        val generator = DiffRowGenerator.create().columnWidth(5).reportLinesUnchanged(true).build()
        val rows = generator.generateDiffRows(split(first), split(second))
        assertEquals(3, rows.size)
    }

    @Test
    fun testGeneratorIssue14() {
        val generator =
            DiffRowGenerator.create().showInlineDiffs(
                true,
            ).mergeOriginalRevised(true).inlineDiffBySplitter {
                    line ->
                DiffRowGenerator.splitStringPreserveDelimiter(line, Regex(","))
            }.oldTag { "~" }.newTag { "**" }.build()
        val rows = generator.generateDiffRows(listOf("J. G. Feldstein, Chair"), listOf("T. P. Pastor, Chair"))
        assertEquals(1, rows.size)
        assertEquals("~J. G. Feldstein~**T. P. Pastor**, Chair", rows[0].oldLine)
    }

    @Test
    fun testGeneratorIssue15() {
        val generator =
            DiffRowGenerator.create().showInlineDiffs(true).inlineDiffByWord(true).oldTag {
                "~"
            }.newTag { "**" }.build()
        val listOne = File(TestConstants.MOCK_FOLDER + "issue15_1.txt").readLines()
        val listTwo = File(TestConstants.MOCK_FOLDER + "issue15_2.txt").readLines()
        val rows = generator.generateDiffRows(listOne, listTwo)
        assertEquals(9, rows.size)
        for (row in rows) {
            if (!row.oldLine.startsWith("TABLE_NAME")) {
                assertTrue(row.newLine.startsWith("**ACTIONS_C16913**"))
                assertTrue(row.oldLine.startsWith("~ACTIONS_C1700"))
            }
        }
    }

    @Test
    fun testGeneratorIssue22() {
        val generator =
            DiffRowGenerator.create().showInlineDiffs(true).inlineDiffByWord(true).oldTag {
                "~"
            }.newTag { "**" }.build()
        val aa = "This is a test senctence."
        val bb = "This is a test for diffutils.\nThis is the second line."
        val rows = generator.generateDiffRows(aa.split("\n"), bb.split("\n"))
        assertEquals(
            "[[CHANGE,This is a test ~senctence~.,This is a test **for diffutils**.], [INSERT,,**This is the second line.**]]",
            rows.toString(),
        )
    }

    @Test
    fun testGeneratorIssue22_2() {
        val generator =
            DiffRowGenerator.create().showInlineDiffs(true).inlineDiffByWord(true).oldTag {
                "~"
            }.newTag { "**" }.build()
        val aa = "This is a test for diffutils.\nThis is the second line."
        val bb = "This is a test senctence."
        val rows = generator.generateDiffRows(aa.split("\n"), bb.split("\n"))
        assertEquals(
            "[[CHANGE,This is a test ~for diffutils~.,This is a test **senctence**.], [DELETE,~This is the second line.~,]]",
            rows.toString(),
        )
    }

    private fun split(content: String) = content.split("\n")

    private fun print(rows: List<DiffRow>) {
        rows.forEach { println(it) }
    }
}
