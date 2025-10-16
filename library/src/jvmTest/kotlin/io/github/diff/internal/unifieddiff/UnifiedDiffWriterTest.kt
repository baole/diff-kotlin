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
import io.github.diff.internal.helper.PatchHelper
import io.github.diff.internal.helper.PatchHelperImpl
import kotlin.test.Test
import kotlin.test.assertEquals

internal class UnifiedDiffWriterTest {
    val patchHelper: PatchHelper<String> = PatchHelperImpl()

    @Test
    fun testWrite() {
        val url =
            this::class.java.getResource("/io/github/diff/unifieddiff/jsqlparser_patch_1.diff")
                ?: throw IllegalStateException("Resource not found")
        val str = url.openStream().reader().use { it.readText() }
        val diff = UnifiedDiffReader.parseUnifiedDiff(str)
        val output = UnifiedDiffWriter.writeToString(diff, { emptyList() }, 5)
        println(output)
    }

    @Test
    fun testWriteWithNewFile() {
        val original = emptyList<String>()
        val revised = listOf("line1", "line2")
        val patch: Patch<String> = patchHelper.generate(original, revised)
        val diff = UnifiedDiff()
        diff.addFile(UnifiedDiffFile.from(null, "revised", patch))
        val output = UnifiedDiffWriter.writeToString(diff, { original }, 5)
        val lines = output.split("\n")
        assertEquals("--- /dev/null", lines[0])
        assertEquals("+++ revised", lines[1])
        assertEquals("@@ -0,0 +1,2 @@", lines[2])
    }
}
