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
package io.github.diff.internal.algorithm

import io.github.diff.Change

internal data class MyersDiffWithLinearSpaceDiffData<T>(val source: List<T>, val target: List<T>) {
    val size: Int = source.size + target.size + 2
    val vDown: IntArray = IntArray(size)
    val vUp: IntArray = IntArray(size)
    val script: MutableList<Change> = mutableListOf()
}
