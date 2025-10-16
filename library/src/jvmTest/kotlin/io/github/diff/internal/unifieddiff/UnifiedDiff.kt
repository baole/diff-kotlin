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
package io.github.diff.internal.unifieddiff

import io.github.diff.PatchFailedException
import io.github.diff.internal.helper.PatchHelper
import io.github.diff.internal.helper.PatchHelperImpl

/**
 *
 */
internal class UnifiedDiff {
    var header: String? = null
    var tail: String? = null
    private val _files = mutableListOf<UnifiedDiffFile>()

    val files: List<UnifiedDiffFile>
        get() = _files.toList()

    fun addFile(file: UnifiedDiffFile) {
        _files.add(file)
    }

    internal fun setTailTxt(tailTxt: String?) {
        this.tail = tailTxt
    }

    @Throws(PatchFailedException::class)
    fun applyPatchTo(
        findFile: (String) -> Boolean,
        originalLines: List<String>,
    ): List<String> {
        val file = files.firstOrNull { findFile(it.fromFile ?: "") }
        val patchHelper: PatchHelper<String> = PatchHelperImpl()
        return file?.patch?.let { patchHelper.applyTo(it, originalLines) } ?: originalLines
    }

    companion object {
        fun from(
            header: String?,
            tail: String?,
            vararg files: UnifiedDiffFile,
        ): UnifiedDiff {
            val diff = UnifiedDiff()
            diff.header = header
            diff.setTailTxt(tail)
            for (file in files) {
                diff.addFile(file)
            }
            return diff
        }
    }
}
