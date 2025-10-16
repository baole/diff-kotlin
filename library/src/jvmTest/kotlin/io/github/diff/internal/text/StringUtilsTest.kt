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

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class StringUtilsTest {
    @Test
    fun testHtmlEntities() {
        assertEquals("&lt;test&gt;", StringUtils.htmlEntites("<test>"))
    }

    @Test
    fun testNormalizeString() {
        assertEquals("    test", StringUtils.normalize("\ttest"))
    }

    @Test
    fun testWrapText() {
        assertEquals("te<br/>st", StringUtils.wrapText("test", 2))
        assertEquals("tes<br/>t", StringUtils.wrapText("test", 3))
        assertEquals("test", StringUtils.wrapText("test", 10))
        assertEquals(".\uD800\uDC01<br/>.", StringUtils.wrapText(".\uD800\uDC01.", 2))
        assertEquals("..<br/>\uD800\uDC01", StringUtils.wrapText("..\uD800\uDC01", 3))
    }

    @Test
    fun testWrapTextInvalid() {
        assertFailsWith<IllegalArgumentException> { StringUtils.wrapText("test", -1) }
    }
}
