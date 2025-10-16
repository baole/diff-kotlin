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

import io.github.diff.Chunk
import io.github.diff.Delta
import io.github.diff.DeltaType
import io.github.diff.DiffAlgorithm
import io.github.diff.DiffAlgorithmListener
import io.github.diff.Patch
import io.github.diff.PatchFailedException
import io.github.diff.internal.patch.ConflictOutput
import io.github.diff.internal.patch.DefaultConflictOutput
import io.github.diff.internal.patch.PatchApplyingContext
import io.github.diff.internal.patch.PatchImpl
import io.github.diff.internal.patch.VerifyChunk

internal class PatchHelperImpl<T>(
    private val deltaHelper: DeltaHelper<T> = DeltaHelperImpl(),
    private val chunkHelper: ChunkHelper<T> = ChunkHelperImpl(),
) : PatchHelper<T> {
    private var conflictOutput: ConflictOutput<T> = DefaultConflictOutput()

    override fun generate(
        original: List<T>,
        revised: List<T>,
        algorithm: DiffAlgorithm<T>?,
        progress: DiffAlgorithmListener?,
        includeEqualParts: Boolean,
    ): Patch<T> {
        val diffAlgorithm = algorithm ?: getDefaultDiffAlgorithm()
        val changes = diffAlgorithm.computeDiff(original, revised, progress)

        val patch = PatchImpl<T>()
        var startOriginal = 0
        var startRevised = 0

        val changeList =
            if (includeEqualParts) {
                changes.sortedWith(compareBy { it.startOriginal })
            } else {
                changes
            }

        for (change in changeList) {
            if (includeEqualParts && startOriginal < change.startOriginal) {
                patch.addDelta(
                    Delta(
                        DeltaType.EQUAL,
                        buildChunk(startOriginal, change.startOriginal, original),
                        buildChunk(startRevised, change.startRevised, revised),
                    ),
                )
            }

            val orgChunk = buildChunk(change.startOriginal, change.endOriginal, original)
            val revChunk = buildChunk(change.startRevised, change.endRevised, revised)
            when (change.deltaType) {
                DeltaType.DELETE,
                DeltaType.INSERT,
                DeltaType.CHANGE,
                    ->
                    patch.addDelta(
                        Delta(
                            change.deltaType,
                            orgChunk,
                            revChunk,
                        ),
                    )

                else -> {}
            }

            startOriginal = change.endOriginal
            startRevised = change.endRevised
        }

        if (includeEqualParts && startOriginal < original.size) {
            patch.addDelta(
                Delta(
                    DeltaType.EQUAL,
                    buildChunk(startOriginal, original.size, original),
                    buildChunk(startRevised, revised.size, revised),
                ),
            )
        }

        return patch
    }

    private fun getDefaultDiffAlgorithm(): DiffAlgorithm<T> {
        return DiffAlgorithm.Factory.createMyers()
    }

    override fun restore(
        patch: Patch<T>,
        target: List<T>,
    ): List<T> {
        val result = target.toMutableList()
        restoreToExisting(patch, result)
        return result
    }

    @Throws(PatchFailedException::class)
    override fun applyTo(
        patch: Patch<T>,
        target: List<T>,
    ): List<T> {
        val result = target.toMutableList()
        applyToExisting(patch, result)
        return result
    }

    /**
     * Applies the patch to the supplied list.
     *
     * @param target The list to apply the changes to. This list has to be modifiable,
     *               otherwise exceptions may be thrown, depending on the used type of list.
     * @throws PatchFailedException if the patch cannot be applied
     * @throws RuntimeException (or similar) if the list is not modifiable.
     */
    @Throws(PatchFailedException::class)
    private fun applyToExisting(
        patch: Patch<T>,
        target: MutableList<T>,
    ) {
        val deltas = patch.getDeltas()
        val it = deltas.listIterator(deltas.size)
        while (it.hasPrevious()) {
            val delta = it.previous()
            val valid = deltaHelper.verifyAndApplyTo(delta, target)
            if (valid != VerifyChunk.OK &&
                // todo rework on conflict output  handling
                patch is PatchImpl
            ) {
                patch.conflictOutput.processConflict(valid, delta, target)
            }
        }
    }

    @Throws(PatchFailedException::class)
    override fun applyFuzzy(
        patch: Patch<T>,
        target: List<T>,
        maxFuzz: Int,
    ): List<T> {
        val ctx = PatchApplyingContext(target.toMutableList(), maxFuzz)

        // the difference between patch's position and actually applied position
        var lastPatchDelta = 0

        for (delta in patch.getDeltas()) {
            ctx.defaultPosition = delta.source.position + lastPatchDelta
            val patchPosition = findPositionFuzzy(ctx, delta)
            if (patchPosition >= 0) {
                deltaHelper.applyFuzzyToAt(delta, ctx.result, ctx.currentFuzz, patchPosition)
                lastPatchDelta = patchPosition - delta.source.position
                ctx.lastPatchEnd = delta.source.last() + lastPatchDelta
            } else {
                conflictOutput.processConflict(
                    VerifyChunk.CONTENT_DOES_NOT_MATCH_TARGET,
                    delta,
                    ctx.result,
                )
            }
        }

        return ctx.result
    }

    // negative for not found
    @Throws(PatchFailedException::class)
    private fun findPositionFuzzy(
        ctx: PatchApplyingContext<T>,
        delta: Delta<T>,
    ): Int {
        for (fuzz in 0..ctx.maxFuzz) {
            ctx.currentFuzz = fuzz
            val foundPosition = findPositionWithFuzz(ctx, delta, fuzz)
            if (foundPosition >= 0) {
                return foundPosition
            }
        }
        return -1
    }

    // negative for not found
    @Throws(PatchFailedException::class)
    private fun findPositionWithFuzz(
        ctx: PatchApplyingContext<T>,
        delta: Delta<T>,
        fuzz: Int,
    ): Int {
        if (chunkHelper.verifyChunk(delta.source, ctx.result, fuzz, ctx.defaultPosition) == VerifyChunk.OK) {
            return ctx.defaultPosition
        }

        ctx.beforeOutRange = false
        ctx.afterOutRange = false

        // moreDelta >= 0: just for overflow guard, not a normal condition
        var moreDelta = 0
        while (moreDelta >= 0) {
            val pos = findPositionWithFuzzAndMoreDelta(ctx, delta, fuzz, moreDelta)
            if (pos >= 0) {
                return pos
            }
            if (ctx.beforeOutRange && ctx.afterOutRange) {
                break
            }
            moreDelta++
        }

        return -1
    }

    // negative for not found
    @Throws(PatchFailedException::class)
    private fun findPositionWithFuzzAndMoreDelta(
        ctx: PatchApplyingContext<T>,
        delta: Delta<T>,
        fuzz: Int,
        moreDelta: Int,
    ): Int {
        // range check: can't apply before end of last patch
        if (!ctx.beforeOutRange) {
            val beginAt = ctx.defaultPosition - moreDelta + fuzz
            // We can't apply patch before end of last patch.
            if (beginAt <= ctx.lastPatchEnd) {
                ctx.beforeOutRange = true
            }
        }
        // range check: can't apply after end of result
        if (!ctx.afterOutRange) {
            val beginAt = ctx.defaultPosition + moreDelta + delta.source.size() - fuzz
            // We can't apply patch before end of last patch.
            if (ctx.result.size < beginAt) {
                ctx.afterOutRange = true
            }
        }

        if (!ctx.beforeOutRange) {
            val before = chunkHelper.verifyChunk(delta.source, ctx.result, fuzz, ctx.defaultPosition - moreDelta)
            if (before == VerifyChunk.OK) {
                return ctx.defaultPosition - moreDelta
            }
        }
        if (!ctx.afterOutRange) {
            val after = chunkHelper.verifyChunk(delta.source, ctx.result, fuzz, ctx.defaultPosition + moreDelta)
            if (after == VerifyChunk.OK) {
                return ctx.defaultPosition + moreDelta
            }
        }
        return -1
    }

    private fun <T> buildChunk(
        start: Int,
        end: Int,
        data: List<T>,
    ): Chunk<T> {
        return Chunk(start, data.subList(start, end).toList())
    }

    /**
     * Restores all changes within the given list.
     * Opposite to [applyToExisting] method.
     *
     * @param target The list to restore changes in. This list has to be modifiable,
     *               otherwise exceptions may be thrown, depending on the used type of list.
     * @throws RuntimeException (or similar) if the list is not modifiable.
     */
    private fun restoreToExisting(
        patch: Patch<T>,
        target: MutableList<T>,
    ) {
        val deltas = patch.getDeltas()
        val it = deltas.listIterator(deltas.size)
        while (it.hasPrevious()) {
            val delta = it.previous()
            deltaHelper.restore(delta, target)
        }
    }


    /**
     * Computes the difference between the given texts inline. This one uses the
     * "trick" to make out of texts lists of characters, like DiffRowGenerator
     * does and merges those changes at the end together again.
     *
     * @param original a [String] representing the original text. Must not be `null`.
     * @param revised a [String] representing the revised text. Must not be `null`.
     * @return The patch describing the difference between the original and
     * revised sequences. Never `null`.
     */

    fun diffInline(
        original: String,
        revised: String,
    ): Patch<String> {
        val origList = original.map { it.toString() }.toMutableList()
        val revList = revised.map { it.toString() }.toMutableList()
        // todo refactor and expose this API
        val helper = PatchHelperImpl<String>()
        val patch = helper.generate(origList, revList)

        val deltas =
            patch.getDeltas().map { delta ->
                Delta(
                    delta.type,
                    Chunk(
                        delta.source.position,
                        compressLines(delta.source.lines, ""),
                        delta.source.changePosition,
                    ),
                    Chunk(
                        delta.target.position,
                        compressLines(delta.target.lines, ""),
                        delta.target.changePosition,
                    ),
                )
            }

        patch.setDeltas(deltas)
        return patch
    }

    private fun compressLines(
        lines: List<String>,
        delimiter: String,
    ): List<String> = if (lines.isEmpty()) emptyList() else listOf(lines.joinToString(delimiter))

}
