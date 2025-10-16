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

import io.github.diff.Chunk
import io.github.diff.Delta
import io.github.diff.DeltaType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class UnifiedDiffReaderTest {
    private fun resourceText(name: String): String =
        this::class.java.getResourceAsStream("/io/github/diff/unifieddiff/$name")
            ?.reader()
            ?.use { it.readText() }
            ?: throw IllegalStateException("Resource not found: $name")

    private fun parse(name: String) = UnifiedDiffReader.parseUnifiedDiff(resourceText(name))

    @Test
    fun testSimpleParse() {
        val diff = parse("jsqlparser_patch_1.diff")
        assertEquals(2, diff.files.size)
        val file1 = diff.files[0]
        assertEquals("src/main/jjtree/net/sf/jsqlparser/parser/JSqlParserCC.jjt", file1.fromFile)
        assertEquals(3, file1.patch.getDeltas().size)
        assertEquals("2.17.1.windows.2\n", diff.tail)
    }

    @Test
    fun testParseDiffBlock() {
        val files =
            UnifiedDiffReader.parseFileNames(
                "diff --git a/src/test/java/net/sf/jsqlparser/statement/select/SelectTest.java b/src/test/java/net/sf/jsqlparser/statement/select/SelectTest.java",
            )

        arrayOf(
            "src/test/java/net/sf/jsqlparser/statement/select/SelectTest.java",
            "src/test/java/net/sf/jsqlparser/statement/select/SelectTest.java",
        ).forEachIndexed { index, it ->
            assertEquals(it, files[index])
        }
    }

    @Test
    fun testChunkHeaderParsing() {
        val matcher =
            UnifiedDiffReader.UNIFIED_DIFF_CHUNK_REGEX.find(
                "@@ -189,6 +189,7 @@ TOKEN: /* SQL Keywords. prefixed with K_ to avoid name clashes */",
            )
        assertTrue(matcher != null)
        matcher!!
        assertEquals("189", matcher.groupValues[1])
        assertEquals("189", matcher.groupValues[3])
    }

    @Test
    fun testChunkHeaderParsing2() {
        val matcher = UnifiedDiffReader.UNIFIED_DIFF_CHUNK_REGEX.find("@@ -189,6 +189,7 @@")
        assertTrue(matcher != null)
        matcher!!
        assertEquals("189", matcher.groupValues[1])
        assertEquals("189", matcher.groupValues[3])
    }

    @Test
    fun testChunkHeaderParsing3() {
        val matcher = UnifiedDiffReader.UNIFIED_DIFF_CHUNK_REGEX.find("@@ -1,27 +1,27 @@")
        assertTrue(matcher != null)
        matcher!!
        assertEquals("1", matcher.groupValues[1])
        assertEquals("1", matcher.groupValues[3])
    }

    @Test
    fun testSimpleParse2() {
        val diff = parse("jsqlparser_patch_1.diff")
        assertEquals(2, diff.files.size)
        val file1 = diff.files[0]
        assertEquals("src/main/jjtree/net/sf/jsqlparser/parser/JSqlParserCC.jjt", file1.fromFile)
        assertEquals(3, file1.patch.getDeltas().size)
        val first = file1.patch.getDeltas()[0]
        assertTrue(first.source.size() > 0)
        assertTrue(first.target.size() > 0)
        assertEquals("2.17.1.windows.2\n", diff.tail)
    }

    @Test
    fun testTimeStampRegexp() {
        assertTrue("2019-04-18 13:49:39.516149751 +0200".matches(UnifiedDiffReader.TIMESTAMP_REGEX))
    }

    @Test
    fun testSimplePattern() {
        assertTrue(Regex("^\\+\\+\\+\\s").containsMatchIn("+++ revised.txt"))
    }

    // Parity tests ported from Java UnifiedDiffReaderTest
    @Test
    fun testParseIssue201() {
        val diff = parse("jsqlparser_patch_1.diff")
        assertEquals(2, diff.files.size)
        val file1 = diff.files[0]
        assertEquals("src/main/jjtree/net/sf/jsqlparser/parser/JSqlParserCC.jjt", file1.fromFile)
        assertEquals(3, file1.patch.getDeltas().size)
        val first = file1.patch.getDeltas()[0]
        assertTrue(first.source.size() > 0)
        assertTrue(first.target.size() > 0)
        assertEquals("2.17.1.windows.2\n", diff.tail)
    }

    @Test
    fun testParseIssue46() {
        val diff = parse("problem_diff_issue46.diff")
        assertEquals(1, diff.files.size)
        val file1 = diff.files[0]
        assertEquals("a.vhd", file1.fromFile)
        assertEquals(1, file1.patch.getDeltas().size)
        assertNull(diff.tail)
    }

    @Test
    fun testParseIssue33() {
        val diff = parse("problem_diff_issue33.diff")
        assertEquals(1, diff.files.size)
        val file1 = diff.files[0]
        assertEquals("Main.java", file1.fromFile)
        assertEquals(1, file1.patch.getDeltas().size)
        assertNull(diff.tail)
        assertNull(diff.header)
    }

    @Test
    fun testParseIssue51() {
        val diff = parse("problem_diff_issue51.diff")
        assertEquals(2, diff.files.size)
        assertEquals("f1", diff.files[0].fromFile)
        assertEquals(1, diff.files[0].patch.getDeltas().size)
        assertEquals("f2", diff.files[1].fromFile)
        assertEquals(1, diff.files[1].patch.getDeltas().size)
        assertNull(diff.tail)
    }

    @Test
    fun testParseIssue79() {
        val diff = parse("problem_diff_issue79.diff")
        assertEquals(1, diff.files.size)
        val file1 = diff.files[0]
        assertEquals("test/Issue.java", file1.fromFile)
        assertEquals(0, file1.patch.getDeltas().size)
        assertNull(diff.tail)
        assertNull(diff.header)
    }

    @Test
    fun testParseIssue84() {
        val diff = parse("problem_diff_issue84.diff")
        assertEquals(2, diff.files.size)
        val file1 = diff.files[0]
        assertEquals("config/ant-phase-verify.xml", file1.fromFile)
        assertEquals(1, file1.patch.getDeltas().size)
        val file2 = diff.files[1]
        assertEquals("/dev/null", file2.fromFile)
        assertEquals(1, file2.patch.getDeltas().size)
        assertEquals("2.7.4", diff.tail)
        assertTrue(diff.header!!.startsWith("From b53e612a2ab5ff15d14860e252f84c0f343fe93a Mon Sep 17 00:00:00 2001"))
    }

    @Test
    fun testParseIssue85() {
        val diff = parse("problem_diff_issue85.diff")
        assertEquals(1, diff.files.size)
        val file1 = diff.files[0]
        assertEquals(
            "diff -r 83e41b73d115 -r a4438263b228 tests/test-check-pyflakes.t",
            file1.diffCommand,
        )
        assertEquals("tests/test-check-pyflakes.t", file1.fromFile)
        assertEquals("tests/test-check-pyflakes.t", file1.toFile)
        assertEquals(1, file1.patch.getDeltas().size)
        assertNull(diff.tail)
    }

    @Test
    fun testParseIssue98() {
        val diff = parse("problem_diff_issue98.diff")
        assertEquals(1, diff.files.size)
        val file1 = diff.files[0]
        assertEquals("100644", file1.deletedFileMode)
        assertEquals(
            "src/test/java/se/bjurr/violations/lib/model/ViolationTest.java",
            file1.fromFile,
        )
        assertEquals("2.25.1", diff.tail)
    }

    @Test
    fun testParseIssue104() {
        val diff = parse("problem_diff_parsing_issue104.diff")
        assertEquals(6, diff.files.size)
        val file = diff.files[2]
        assertEquals("/dev/null", file.fromFile)
        assertEquals("doc/samba_data_tool_path.xml.in", file.toFile)

        val expected =
            listOf(
                Delta(
                    DeltaType.CHANGE,
                    Chunk(0, emptyList<String>()),
                    Chunk(0, listOf("@SAMBA_DATA_TOOL@")),
                ),
            )

        expected.forEach {
            assertEquals(it, file.patch.getDeltas()[expected.indexOf(it)], "at index ${expected.indexOf(it)}")
        }
        assertEquals(expected.size, file.patch.getDeltas().size)
        assertEquals("2.14.4", diff.tail)
    }

    @Test
    fun testParseIssue107BazelDiff() {
        val diff = parse("01-bazel-strip-unused.patch_issue107.diff")
        assertEquals(450, diff.files.size)
        val file = diff.files[0]
        assertEquals("./src/main/java/com/amazonaws/AbortedException.java", file.fromFile)
        assertEquals(
            "/home/greg/projects/bazel/third_party/aws-sdk-auth-lite/src/main/java/com/amazonaws/AbortedException.java",
            file.toFile,
        )
        assertEquals(48, diff.files.count { it.isNoNewLineAtTheEndOfTheFile })
    }

    @Test
    fun testParseIssue107_2() {
        val diff = parse("problem_diff_issue107.diff")
        assertEquals(2, diff.files.size)
        val file1 = diff.files[0]
        assertEquals("Main.java", file1.fromFile)
        assertEquals(1, file1.patch.getDeltas().size)
    }

    @Test
    fun testParseIssue107_3() {
        val diff = parse("problem_diff_issue107_3.diff")
        assertEquals(1, diff.files.size)
        val file1 = diff.files[0]
        assertEquals("Billion laughs attack.md", file1.fromFile)
        assertEquals(1, file1.patch.getDeltas().size)
    }

    @Test
    fun testParseIssue107_4() {
        val diff = parse("problem_diff_issue107_4.diff")
        assertEquals(27, diff.files.size)
        assertTrue(diff.files.map { it.fromFile }.contains("README.md"))
    }

    @Test
    fun testParseIssue107_5() {
        val diff = parse("problem_diff_issue107_5.diff")
        assertEquals(22, diff.files.size)
        assertTrue(
            diff.files.map { it.fromFile }
                .contains(
                    "rt/management/src/test/java/org/apache/cxf/management/jmx/MBServerConnectorFactoryTest.java",
                ),
        )
    }

    @Test
    fun testParseIssue110() {
        val diff = parse("0001-avahi-python-Use-the-agnostic-DBM-interface.patch")
        assertEquals(5, diff.files.size)
        val file = diff.files[4]
        assertEquals(87, file.similarityIndex)
        assertEquals("service-type-database/build-db.in", file.renameFrom)
        assertEquals("service-type-database/build-db", file.renameTo)
        assertEquals("service-type-database/build-db.in", file.fromFile)
        assertEquals("service-type-database/build-db", file.toFile)
    }

    @Test
    fun testParseIssue117() {
        val diff = parse("problem_diff_issue117.diff")
        assertEquals(2, diff.files.size)
        val deltas = diff.files[0].patch.getDeltas()
        assertEquals(listOf(24, 27), deltas[0].source.changePosition)
        assertEquals(listOf(24, 27), deltas[0].target.changePosition)
        assertEquals(listOf(64), deltas[1].source.changePosition)
        assertEquals(
            listOf(64, 65, 66, 67, 68, 69, 70, 71, 72, 73, 74),
            deltas[1].target.changePosition,
        )
    }

    @Test
    fun testParseIssue122() {
        val diff = parse("problem_diff_issue122.diff")
        assertEquals(1, diff.files.size)
        assertTrue(diff.files.map { it.fromFile }.contains("coders/wpg.c"))
    }

    @Test
    fun testParseIssue123() {
        val diff = parse("problem_diff_issue123.diff")
        assertEquals(2, diff.files.size)
        assertTrue(
            diff.files.map { it.fromFile }
                .contains("src/java/main/org/apache/zookeeper/server/FinalRequestProcessor.java"),
        )
    }

    @Test
    fun testParseIssue141() {
        val diff = parse("problem_diff_issue141.diff")
        val file1 = diff.files[0]
        assertEquals("a.txt", file1.fromFile)
        assertEquals("a1.txt", file1.toFile)
    }

    @Test
    fun testParseIssue182add() {
        val diff = parse("problem_diff_issue182_add.diff")
        val file1 = diff.files[0]
        assertEquals("some-image.png", file1.binaryAdded)
    }

    @Test
    fun testParseIssue182delete() {
        val diff = parse("problem_diff_issue182_delete.diff")
        val file1 = diff.files[0]
        assertEquals("some-image.png", file1.binaryDeleted)
    }

    @Test
    fun testParseIssue182edit() {
        val diff = parse("problem_diff_issue182_edit.diff")
        val file1 = diff.files[0]
        assertEquals("some-image.png", file1.binaryEdited)
    }

    @Test
    fun testParseIssue182mode() {
        val diff = parse("problem_diff_issue182_mode.diff")
        val file1 = diff.files[0]
        assertEquals("100644", file1.oldMode)
        assertEquals("100755", file1.newMode)
    }

    @Test
    fun testParseIssue193Copy() {
        val diff = parse("problem_diff_parsing_issue193.diff")
        val file1 = diff.files[0]
        assertEquals(
            "modules/configuration/config/web/pcf/account/AccountContactCV.pcf",
            file1.copyFrom,
        )
        assertEquals(
            "modules/configuration/config/web/pcf/account/AccountContactCV.default.pcf",
            file1.copyTo,
        )
    }
}
