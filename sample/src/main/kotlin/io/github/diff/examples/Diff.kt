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
package io.github.diff.examples

import io.github.diff.generatePatch
import io.github.diff.Delta

object Diff {

    @JvmStatic
    fun main(args: Array<String>) {
        val patch = generatePatch {
            this.original = listOf("line1", "line2", "line3", "line4", "line5")
            this.revised = listOf("line1", "line3", "line4 modified", "line5", "line6")
        }

        for (delta: Delta<String> in patch.getDeltas()) {
            println(delta)
        }
    }
}
