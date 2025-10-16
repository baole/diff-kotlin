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
import io.github.diff.utils.TestConstants
import java.io.File
import java.io.InputStream
import java.util.zip.ZipFile
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

internal class PathHelperGenerateTest {
    val helper: PatchHelper<String> = PatchHelperImpl()

    @Test
    fun testDiff_Insert() {
        val patch = helper.generate(listOf("hhh"), listOf("hhh", "jjj", "kkk"))
        assertNotNull(patch)
        assertEquals(1, patch.getDeltas().size)
        val delta = patch.getDeltas()[0]
        assertTrue(delta.type == DeltaType.INSERT)
        assertEquals(Chunk(1, emptyList<String>()), delta.source)
        assertEquals(Chunk(1, listOf("jjj", "kkk")), delta.target)
    }

    @Test
    fun testDiff_Delete() {
        val patch = helper.generate(listOf("ddd", "fff", "ggg"), listOf("ggg"))
        assertNotNull(patch)
        assertEquals(1, patch.getDeltas().size)
        val delta = patch.getDeltas()[0]
        assertTrue(delta.type == DeltaType.DELETE)
        assertEquals(Chunk(0, listOf("ddd", "fff")), delta.source)
        assertEquals(Chunk(0, emptyList<String>()), delta.target)
    }

    @Test
    fun testDiff_Change() {
        val from = listOf("aaa", "bbb", "ccc")
        val to = listOf("aaa", "zzz", "ccc")
        val patch = helper.generate(from, to)
        assertNotNull(patch)
        assertEquals(1, patch.getDeltas().size)
        val delta = patch.getDeltas()[0]
        assertTrue(delta.type == DeltaType.CHANGE)
        assertEquals(Chunk(1, listOf("bbb")), delta.source)
        assertEquals(Chunk(1, listOf("zzz")), delta.target)
    }

    @Test
    fun testDiff_EmptyList() {
        val patch = helper.generate(emptyList<String>(), emptyList())
        assertNotNull(patch)
        assertEquals(0, patch.getDeltas().size)
    }

    @Test
    fun testDiff_EmptyListWithNonEmpty() {
        val patch = helper.generate(emptyList<String>(), listOf("aaa"))
        assertNotNull(patch)
        assertEquals(1, patch.getDeltas().size)
        val delta = patch.getDeltas()[0]
        assertTrue(delta.type == DeltaType.INSERT)
    }
//
//    @Test
//    fun testDiffInline() {
//        val diffHelper: DiffHelper<String> = DiffHelperImpl()
//        val patch = diffHelper.diffInline("", "test")
//        assertEquals(1, patch.getDeltas().size)
//        assertTrue(patch.getDeltas()[0].type == DeltaType.INSERT)
//        assertEquals(0, patch.getDeltas()[0].source.position)
//        assertEquals(0, patch.getDeltas()[0].source.lines.size)
//        assertEquals("test", patch.getDeltas()[0].target.lines[0])
//    }
//
//    @Test
//    fun testDiffInline2() {
//        val patch = DiffUtils.diffInline("es", "fest")
//        assertEquals(2, patch.getDeltas().size)
//        assertTrue(patch.getDeltas()[0].type == DeltaType.INSERT)
//        assertEquals(0, patch.getDeltas()[0].source.position)
//        assertEquals(2, patch.getDeltas()[1].source.position)
//        assertEquals(0, patch.getDeltas()[0].source.lines.size)
//        assertEquals(0, patch.getDeltas()[1].source.lines.size)
//        assertEquals("f", patch.getDeltas()[0].target.lines[0])
//        assertEquals("t", patch.getDeltas()[1].target.lines[0])
//    }

    @Test
    fun testDiffMissesChangeForkDnaumenkoIssue31() {
        val original = listOf("line1", "line2", "line3")
        val revised = listOf("line1", "line2-2", "line4")
        val patch = helper.generate(original, revised)
        assertEquals(1, patch.getDeltas().size)
        assertEquals(
            Delta(
                DeltaType.CHANGE,
                Chunk(1, listOf("line2", "line3")),
                Chunk(1, listOf("line2-2", "line4")),
            ),
            patch.getDeltas()[0],
        )
    }

    @Test
    fun testDiffMyersExample1() {
        val patch = helper.generate(listOf("A", "B", "C", "A", "B", "B", "A"), listOf("C", "B", "A", "B", "A", "C"))
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
    fun testDiff_Equal() {
        val patch = helper.generate(listOf("hhh", "jjj", "kkk"), listOf("hhh", "jjj", "kkk"), algorithm = null, progress = null, true)
        assertNotNull(patch)
        assertEquals(1, patch.getDeltas().size)
        val delta = patch.getDeltas()[0]
        assertTrue(delta.type == DeltaType.EQUAL)
        assertEquals(Chunk(0, listOf("hhh", "jjj", "kkk")), delta.source)
        assertEquals(Chunk(0, listOf("hhh", "jjj", "kkk")), delta.target)
    }

    @Test
    fun testDiff_InsertWithEqual() {
        val patch = helper.generate(listOf("hhh"), listOf("hhh", "jjj", "kkk"), algorithm = null, progress = null, true)
        assertNotNull(patch)
        assertEquals(2, patch.getDeltas().size)
        var delta: Delta<String> = patch.getDeltas()[0]
        assertTrue(delta.type == DeltaType.EQUAL)
        assertEquals(Chunk(0, listOf("hhh")), delta.source)
        assertEquals(Chunk(0, listOf("hhh")), delta.target)
        delta = patch.getDeltas()[1]
        assertTrue(delta.type == DeltaType.INSERT)
        assertEquals(Chunk(1, emptyList<String>()), delta.source)
        assertEquals(Chunk(1, listOf("jjj", "kkk")), delta.target)
    }

    @Test
    fun testDiff_ProblemIssue42() {
        val patch = helper.generate(listOf("The", "dog", "is", "brown"), listOf("The", "fox", "is", "down"), algorithm = null, progress = null, true)
        assertNotNull(patch)
        assertEquals(4, patch.getDeltas().size)
        assertEquals(
            listOf(
                DeltaType.EQUAL,
                DeltaType.CHANGE,
                DeltaType.EQUAL,
                DeltaType.CHANGE,
            ),
            patch.getDeltas().map {
                it.type
            },
        )
    }

    @Test
    fun testDiffPatchIssue189Problem() {
        val originalStream = this::class.java.getResourceAsStream("/io/github/diff/text/issue_189_insert_original.txt")
        val revisedStream = this::class.java.getResourceAsStream("/io/github/diff/text/issue_189_insert_revised.txt")
        requireNotNull(originalStream)
        requireNotNull(revisedStream)
        val originalLines = readStringListFromInputStream(originalStream)
        val revisedLines = readStringListFromInputStream(revisedStream)
        val patch = helper.generate(originalLines, revisedLines)
        assertEquals(1, patch.getDeltas().size)
    }

    @Ignore
    @Test
    fun testPossibleDiffHangOnLargeDatasetDnaumenkoIssue26() {
        val zipPath = TestConstants.MOCK_FOLDER + "large_dataset1.zip"
        val zipFile = File(zipPath)
        if (!zipFile.exists()) return
        ZipFile(zipFile).use { zip ->
            val originalLines = readStringListFromInputStream(zip.getInputStream(zip.getEntry("ta")))
            val revisedLines = readStringListFromInputStream(zip.getInputStream(zip.getEntry("tb")))
            val patch = helper.generate(originalLines, revisedLines)
            assertEquals(1, patch.getDeltas().size)
        }
    }

    companion object Companion {
        private fun readStringListFromInputStream(inputStream: InputStream): List<String> =
            inputStream.bufferedReader(Charsets.UTF_8).use { it.readLines() }
    }
}
