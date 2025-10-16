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

import io.github.diff.internal.algorithm.MyersDiff
import io.github.diff.internal.algorithm.MyersDiffWithLinearSpace

/**
 * Interface of a diff algorithm.
 *
 * @param T type of data that is diffed.
 */
interface DiffAlgorithm<T> {
    /**
     * Computes the changeset to patch the source list to the target list.
     *
     * @param source source data
     * @param target target data
     * @param progress progress listener
     * @return list of changes
     */
    fun computeDiff(
        source: List<T>,
        target: List<T>,
        progress: DiffAlgorithmListener?,
    ): List<Change>

    object Factory {
        fun <T> createMyers(equalizer: (T, T) -> Boolean = { a, b -> a == b }): DiffAlgorithm<T> = MyersDiff(equalizer)
        fun <T> createMyersLinearSpace(
            equalizer: (T, T) -> Boolean = { a, b -> a == b },
        ): DiffAlgorithm<T> = MyersDiffWithLinearSpace(equalizer)
    }
}
