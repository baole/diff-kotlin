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

import io.github.diff.internal.helper.PatchHelper
import io.github.diff.internal.helper.PatchHelperImpl
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals

@Ignore("for next release")
internal class UnifiedDiffRoundTripNewLineTest {
    val patchHelper: PatchHelper<String> = PatchHelperImpl()

    @Test
    fun testIssue135MissingNoNewLineInPatched() {
        val beforeContent = "rootProject.name = \"sample-repo\""
        val afterContent = "rootProject.name = \"sample-repo\"\n"
        val patch =
            buildString {
                append("diff --git a/settings.gradle b/settings.gradle\n")
                append("index ef3b8e2..ab30124 100644\n")
                append("--- a/settings.gradle\n")
                append("+++ b/settings.gradle\n")
                append("@@ -1 +1 @@\n")
                append("-rootProject.name = \"sample-repo\"\n")
                append("\\n\n")
                append("+rootProject.name = \"sample-repo\"\n")
            }
        val unifiedDiff = UnifiedDiffReader.parseUnifiedDiff(patch)
        val unifiedAfterContent =
            patchHelper.applyTo(
                unifiedDiff.files[0].patch,
                beforeContent.split("\n"),
            ).joinToString("\n")
        assertEquals(afterContent, unifiedAfterContent)
    }
}
