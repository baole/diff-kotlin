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

import io.github.diff.Patch
import io.github.diff.internal.patch.PatchImpl

/**
 * Data structure for one patched file from a unified diff file.
 *
 */
internal class UnifiedDiffFile {
    var diffCommand: String? = null
    var fromFile: String? = null
    var fromTimestamp: String? = null
    var toFile: String? = null
    var renameFrom: String? = null
    var renameTo: String? = null
    var copyFrom: String? = null
    var copyTo: String? = null
    var toTimestamp: String? = null
    var index: String? = null
    var newFileMode: String? = null
    var oldMode: String? = null
    var newMode: String? = null
    var deletedFileMode: String? = null
    var binaryAdded: String? = null
    var binaryDeleted: String? = null
    var binaryEdited: String? = null
    var patch: Patch<String> = PatchImpl()
    var isNoNewLineAtTheEndOfTheFile: Boolean = false
    var similarityIndex: Int? = null

    companion object {
        fun from(
            fromFile: String?,
            toFile: String?,
            patch: Patch<String>,
        ): UnifiedDiffFile {
            val file = UnifiedDiffFile()
            file.fromFile = fromFile
            file.toFile = toFile
            file.patch = patch
            return file
        }
    }
}
