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
package io.github.diff

/**
 * Holds the information about the part of text involved in the diff process
 *
 * Text is represented as `List` because the diff engine is
 * capable of handling more than plain ascii. In fact, arrays or lists of any
 * type that implements [hashCode] and [equals] correctly can be subject to
 * differencing using this library.
 *
 * @param T The type of the compared elements in the 'lines'.
 */
data class Chunk<T>(
    val position: Int,
    val lines: List<T>,
    val changePosition: List<Int>? = null,
) {
    fun size(): Int = lines.size

    /**
     * Returns the index of the last line of the chunk.
     */
    fun last(): Int = position + size() - 1

    /**
     * todo get rid of custom equals and hashcode
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null) return false

        other as Chunk<*>

        if (position != other.position) return false
        if (lines != other.lines) return false

        return true
    }

    override fun hashCode(): Int {
        var result = position
        result = 31 * result + lines.hashCode()
        return result
    }
}
