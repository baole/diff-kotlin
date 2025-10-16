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

internal class PatchApplyingContext<T>(
    val result: MutableList<T>,
    val maxFuzz: Int,
) {
    // the position last patch applied to.
    var lastPatchEnd = -1

    // /// passing values from find to apply
    var currentFuzz = 0

    var defaultPosition = 0
    var beforeOutRange = false
    var afterOutRange = false
}
