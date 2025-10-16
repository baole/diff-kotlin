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
import io.github.diff.PatchFailedException

internal class DefaultConflictOutput<T> : ConflictOutput<T> {
    /**
     * Default Patch behaviour to throw an exception for patching conflicts.
     */
    override fun processConflict(
        verifyChunk: VerifyChunk,
        delta: Delta<T>,
        result: MutableList<T>,
    ) {
        throw PatchFailedException("could not apply patch due to $verifyChunk")
    }
}
