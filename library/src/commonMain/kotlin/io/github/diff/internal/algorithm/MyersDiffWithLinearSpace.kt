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
package io.github.diff.internal.algorithm

import io.github.diff.Change
import io.github.diff.DeltaType
import io.github.diff.DiffAlgorithm
import io.github.diff.DiffAlgorithmListener

internal class MyersDiffWithLinearSpace<T>(
    val equalizer: (T, T) -> Boolean = { a, b -> a == b },
) : DiffAlgorithm<T> {
    override fun computeDiff(
        source: List<T>,
        target: List<T>,
        progress: DiffAlgorithmListener?,
    ): List<Change> {
        progress?.diffStart()
        val data = MyersDiffWithLinearSpaceDiffData(source, target)
        val maxIdx = source.size + target.size
        buildScript(data, 0, source.size, 0, target.size) { idx -> progress?.diffStep(idx, maxIdx) }
        progress?.diffEnd()
        return data.script
    }

    private fun buildScript(
        data: MyersDiffWithLinearSpaceDiffData<T>,
        start1: Int,
        end1: Int,
        start2: Int,
        end2: Int,
        progress: ((Int) -> Unit)?,
    ) {
        progress?.invoke((end1 - start1) / 2 + (end2 - start2) / 2)
        val middle = getMiddleSnake(data, start1, end1, start2, end2)
        if (middle == null ||
            (middle.start == end1 && middle.diag == end1 - end2) ||
            (middle.end == start1 && middle.diag == start1 - start2)
        ) {
            var i = start1
            var j = start2
            while (i < end1 || j < end2) {
                if (i < end1 && j < end2 && equalizer(data.source[i], data.target[j])) {
                    ++i
                    ++j
                } else {
                    if (end1 - start1 > end2 - start2) {
                        if (data.script.isEmpty() ||
                            data.script[data.script.size - 1].endOriginal != i ||
                            data.script[data.script.size - 1].deltaType != DeltaType.DELETE
                        ) {
                            data.script.add(Change(DeltaType.DELETE, i, i + 1, j, j))
                        } else {
                            data.script[data.script.size - 1] = data.script[data.script.size - 1].copy(endOriginal = i + 1)
                        }
                        ++i
                    } else {
                        if (data.script.isEmpty() ||
                            data.script[data.script.size - 1].endRevised != j ||
                            data.script[data.script.size - 1].deltaType != DeltaType.INSERT
                        ) {
                            data.script.add(Change(DeltaType.INSERT, i, i, j, j + 1))
                        } else {
                            data.script[data.script.size - 1] = data.script[data.script.size - 1].copy(endRevised = j + 1)
                        }
                        ++j
                    }
                }
            }
        } else {
            buildScript(data, start1, middle.start, start2, middle.start - middle.diag, progress)
            buildScript(data, middle.end, end1, middle.end - middle.diag, end2, progress)
        }
    }

    private fun getMiddleSnake(
        data: MyersDiffWithLinearSpaceDiffData<T>,
        start1: Int,
        end1: Int,
        start2: Int,
        end2: Int,
    ): MyersDiffWithLinearSpaceSnake? {
        val m = end1 - start1
        val n = end2 - start2
        if (m == 0 || n == 0) return null
        val delta = m - n
        val sum = n + m
        val offset = (if (sum % 2 == 0) sum else sum + 1) / 2
        data.vDown[1 + offset] = start1
        data.vUp[1 + offset] = end1 + 1
        for (d in 0..offset) {
            var k = -d
            while (k <= d) {
                val kmiddle = k + offset
                if (k == -d || (k != d && data.vDown[kmiddle - 1] < data.vDown[kmiddle + 1])) {
                    data.vDown[kmiddle] = data.vDown[kmiddle + 1]
                } else {
                    data.vDown[kmiddle] = data.vDown[kmiddle - 1] + 1
                }
                var x = data.vDown[kmiddle]
                var y = x - start1 + start2 - k
                while (x < end1 && y < end2 && equalizer(data.source[x], data.target[y])) {
                    data.vDown[kmiddle] = ++x
                    ++y
                }
                if (delta % 2 != 0 && delta - d <= k && k <= delta + d) {
                    if (data.vUp[kmiddle - delta] <= data.vDown[kmiddle]) {
                        return buildSnake(data, data.vUp[kmiddle - delta], k + start1 - start2, end1, end2)
                    }
                }
                k += 2
            }
            k = delta - d
            while (k <= delta + d) {
                val kmiddle = k + offset - delta
                if (k == delta - d || (k != delta + d && data.vUp[kmiddle + 1] <= data.vUp[kmiddle - 1])) {
                    data.vUp[kmiddle] = data.vUp[kmiddle + 1] - 1
                } else {
                    data.vUp[kmiddle] = data.vUp[kmiddle - 1]
                }
                var x = data.vUp[kmiddle] - 1
                var y = x - start1 + start2 - k
                while (x >= start1 && y >= start2 && equalizer(data.source[x], data.target[y])) {
                    data.vUp[kmiddle] = x--
                    y--
                }
                if (delta % 2 == 0 && -d <= k && k <= d) {
                    if (data.vUp[kmiddle] <= data.vDown[kmiddle + delta]) {
                        return buildSnake(data, data.vUp[kmiddle], k + start1 - start2, end1, end2)
                    }
                }
                k += 2
            }
        }
        throw IllegalStateException("could not find a diff path")
    }

    private fun buildSnake(
        data: MyersDiffWithLinearSpaceDiffData<T>,
        start: Int,
        diag: Int,
        end1: Int,
        end2: Int,
    ): MyersDiffWithLinearSpaceSnake {
        var end = start
        while (end - diag < end2 && end < end1 && equalizer(data.source[end], data.target[end - diag])) {
            ++end
        }
        return MyersDiffWithLinearSpaceSnake(start, end, diag)
    }
}
