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

import io.github.diff.DiffAlgorithm
import io.github.diff.DiffAlgorithmListener
import io.github.diff.Patch
import io.github.diff.PatchFailedException

internal interface PatchHelper<T> {
    /**
     * Computes the difference between the original and revised list of elements
     * with default diff algorithm
     *
     * @param original a [List] representing the original text. Must not be `null`.
     * @param revised a [List] representing the revised text. Must not be `null`.
     * @param algorithm a [DiffAlgorithm] representing the diff algorithm. Must not be `null`.
     * @param progress a [DiffAlgorithmListener] representing the diff algorithm listener.
     * @param includeEqualParts Include equal data parts into the patch.
     * @return The patch describing the difference between the original and
     * revised sequences. Never `null`.
     */
    fun generate(
        original: List<T>,
        revised: List<T>,
        algorithm: DiffAlgorithm<T>? = null,
        progress: DiffAlgorithmListener? = null,
        includeEqualParts: Boolean = false,
    ): Patch<T>

    /**
     * Creates a new list, containing the restored state of the given list.
     * Opposite to [applyTo] method.
     *
     * @param target The list to copy and apply changes to.
     * @return A new list, containing the restored state.
     */
    fun restore(
        patch: Patch<T>,
        target: List<T>,
    ): List<T>

    /**
     * Creates a new list, the patch is being applied to.
     *
     * @param target The list to apply the changes to.
     * @return A new list containing the applied patch.
     * @throws io.github.diff.PatchFailedException if the patch cannot be applied
     */
    @Throws(PatchFailedException::class)
    fun applyTo(
        patch: Patch<T>,
        target: List<T>,
    ): List<T>

    @Throws(PatchFailedException::class)
    fun applyFuzzy(
        patch: Patch<T>,
        target: List<T>,
        maxFuzz: Int,
    ): List<T>
}
