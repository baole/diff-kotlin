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

/**
 * Utility class for String operations.
 * todo refactoring needed
 */
internal object StringUtils {
    /**
     * Replaces all opening and closing tags with `&lt;` or `&gt;`.
     *
     * @param str the string to escape
     * @return str with some HTML meta characters escaped.
     */

    fun htmlEntites(str: String): String {
        return str.replace("<", "&lt;").replace(">", "&gt;")
    }

    fun normalize(str: String): String {
        return htmlEntites(str).replace("\t", "    ")
    }

    fun wrapText(
        list: List<String>,
        columnWidth: Int,
    ): List<String> {
        return list.map { line -> wrapText(line, columnWidth) }
    }

    /**
     * Wrap the text with the given column width
     *
     * @param line the text
     * @param columnWidth the given column
     * @return the wrapped text
     */

    fun wrapText(
        line: String,
        columnWidth: Int,
    ): String {
        require(columnWidth >= 0) { "columnWidth may not be less 0" }
        if (columnWidth == 0) {
            return line
        }
        val length = line.length
        val delimiter = "<br/>".length
        var widthIndex = columnWidth

        val b = StringBuilder(line)

        var count = 0
        while (length > widthIndex) {
            var breakPoint = widthIndex + delimiter * count
            if (breakPoint > 0 && breakPoint < b.length &&
                b[breakPoint - 1].isHighSurrogate() &&
                b[breakPoint].isLowSurrogate()
            ) {
                // Shift a breakpoint that would split a supplemental code-point.
                breakPoint += 1
                if (breakPoint == b.length) {
                    // Break before instead of after if this is the last code-point.
                    breakPoint -= 2
                }
            }
            b.insert(breakPoint, "<br/>")
            widthIndex += columnWidth
            count++
        }

        return b.toString()
    }
}
