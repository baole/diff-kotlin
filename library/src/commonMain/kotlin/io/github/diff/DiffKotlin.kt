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

import io.github.diff.internal.helper.PatchHelper
import io.github.diff.internal.helper.PatchHelperImpl

/**
 * DSL builder for batch operations.
 */
class PatchBuilder<T> {
    var original: List<T>? = null
    var revised: List<T>? = null
    var algorithm: DiffAlgorithm<T>? = null
    var progress: DiffAlgorithmListener? = null
    var includeEqualParts: Boolean = false
    var equalizer: ((T, T) -> Boolean)? = null

    fun build(): Patch<T> {
        val orig = requireNotNull(original) { "original must not be null" }
        val rev = requireNotNull(revised) { "revised must not be null" }
        val algo =
            when {
                algorithm != null -> algorithm!!
                equalizer != null -> DiffAlgorithm.Factory.createMyers(equalizer = equalizer!!)
                else -> DiffAlgorithm.Factory.createMyers()
            }
        val helper: PatchHelper<T> = PatchHelperImpl()
        return helper.generate(orig, rev, algo, progress, includeEqualParts)
    }
}

/**
 * Generate a patch between the original and revised lists using a DSL-style builder.
 */
fun <T> generatePatch(block: PatchBuilder<T>.() -> Unit): Patch<T> {
    return PatchBuilder<T>().apply(block).build()
}

/**
 * Applies the given patch to the original list and returns the revised list.
 *
 * @param original a [List] representing the original list.
 * @param patch a [Patch] representing the patch to apply.
 * @return the revised list.
 * @throws PatchFailedException if the patch cannot be applied.
 */

@Throws(PatchFailedException::class)
fun <T> Patch<T>.patch(
    original: List<T>,
): List<T> {
    val patchHelper: PatchHelper<T> = PatchHelperImpl()
    return patchHelper.applyTo(this, original)
}

/**
 * Applies the given patch to the revised list and returns the original list.
 *
 * @param revised a [List] representing the revised list.
 * @param patch a [Patch] representing the patch to apply.
 * @return the original list.
 */

fun <T> Patch<T>.unpatch(
    revised: List<T>,
): List<T> {
    val patchHelper: PatchHelper<T> = PatchHelperImpl()
    return patchHelper.restore(this, revised)
}
