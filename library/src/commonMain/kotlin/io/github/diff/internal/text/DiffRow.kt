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
 * Describes the diff row in form [tag, oldLine, newLine) for showing the difference between two texts
 * todo refactoring needed
 */
internal data class DiffRow(
    var tag: Tag,
    val oldLine: String,
    val newLine: String,
) {
    enum class Tag {
        INSERT,
        DELETE,
        CHANGE,
        EQUAL,
    }

    override fun toString(): String {
        return "[$tag,$oldLine,$newLine]"
    }
}
