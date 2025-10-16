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
package io.github.diff.internal.patch

import io.github.diff.Delta
import io.github.diff.Patch

/**
 * Describes the patch holding all deltas between the original and revised
 * texts.
 *
 * @param T The type of the compared elements in the 'lines'.
 */
internal class PatchImpl<T>() : Patch<T> {
    private var deltas: MutableList<Delta<T>> = mutableListOf()

    // todo rework on conflict output  handling to expose for testing
    internal var conflictOutput: ConflictOutput<T> = DefaultConflictOutput()

    /**
     * Alter normal conflict output behaviour to e.g. include some conflict
     * statements in the result, like git does it.
     */
    internal fun withConflictOutput(conflictOutput: ConflictOutput<T>): Patch<T> {
        this.conflictOutput = conflictOutput
        return this
    }

    /**
     * Add the given delta to this patch
     *
     * @param delta the given delta
     */
    override fun addDelta(delta: Delta<T>) {
        deltas.add(delta)
    }

    /**
     * Get the list of computed deltas
     *
     * @return the deltas
     */
    override fun getDeltas(): List<Delta<T>> {
        deltas.sortWith(compareBy { it.source.position })
        return deltas.toList() // Return a copy to prevent external modification
    }

    override fun setDeltas(deltas: List<Delta<T>>) {
        this.deltas = deltas.toMutableList()
    }
}
