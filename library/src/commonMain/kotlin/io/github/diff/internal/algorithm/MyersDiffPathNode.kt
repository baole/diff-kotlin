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

/**
 * A node in a diffpath.
 *
 */
internal data class MyersDiffPathNode(
    /**
     * Position in the original sequence.
     */
    val i: Int,
    /**
     * Position in the revised sequence.
     */
    val j: Int,
    val snake: Boolean,
    val bootstrap: Boolean,
    /**
     * The previous node in the path.
     */
    val prev: MyersDiffPathNode?,
) {
    fun isSnake(): Boolean = snake

    /**
     * Is this a bootstrap node?
     *
     * In bottstrap nodes one of the two corrdinates is less than zero.
     *
     * @return tru if this is a bootstrap node.
     */
    fun isBootstrap(): Boolean = bootstrap

    /**
     * Skips sequences of [MyersDiffPathNode]s until a snake or bootstrap node is found, or the end of the
     * path is reached.
     *
     * @return The next first [MyersDiffPathNode] or bootstrap node in the path, or `null` if none found.
     */
    fun previousSnake(): MyersDiffPathNode? {
        if (isBootstrap()) {
            return null
        }
        if (!isSnake() && prev != null) {
            return prev.previousSnake()
        }
        return this
    }
}
