# diff-kotlin

A Kotlin Multiplatform (KMP) library for computing text diffs, supporting multiple algorithms including Myers and Myers with linear space optimization. Designed for JVM, Android, iOS, and other Kotlin targets. No additional dependencies required.

## Features

- Kotlin Multiplatform (KMP) support: JVM, Android, iOS, and more
- Efficient diff computation for text and sequences
- Multiple algorithms: Myers, Myers with linear space
- Extensible via factory and builder patterns
- Simple DSL for diff operations
- Well-tested and production-ready
- **No additional dependencies** (other than Kotlin stdlib)

## Installation

Add the following to your `build.gradle.kts`:

```kotlin
repositories {
    mavenCentral()
}

dependencies {
    implementation("io.github.baole:diff-kotlin:<latest-version>")
}
```

Replace `<latest-version>` with the version published on [Maven Central](https://search.maven.org/).

## Usage

### Computing Diff

```kotlin
import io.github.diff.generatePatch
import io.github.diff.Delta

val patch = generatePatch {
    this.original = listOf("line1", "line2", "line3", "line4", "line5")
    this.revised = listOf("line1", "line3", "line4 modified", "line5", "line6")
}

for (delta: Delta<String> in patch.getDeltas()) {
    println(delta)
}
```

### Applying Patch

```kotlin
import io.github.diff.generatePatch
import io.github.diff.PatchFailedException
import io.github.diff.patch

val original = listOf("line1", "line2", "line3", "line4", "line5")
val revised = listOf("line1", "line3", "line4 modified", "line5", "line6")

try {
    val patch = generatePatch {
        this.original = original
        this.revised = revised
    }
    val result = patch.patch(original)
    println(result.joinToString("\n"))
} catch (e: PatchFailedException) {
    e.printStackTrace()
}
```

## Algorithms

- **MyersDiff**: Standard Myers diff algorithm
- **MyersDiffWithLinearSpace**: Optimized for linear space usage

You can select the algorithm when generating patches.

## Code Quality

This project uses:
- **ktlint** for code formatting and style checking
- Comprehensive test coverage with MockK for mocking

Run code quality checks:
```bash
./gradlew ktlintCheck
```

Auto-fix formatting issues:
```bash
./gradlew ktlintFormat
```

## Contributing

Contributions are welcome! Please open issues or submit pull requests.

## License

This project is licensed under the Apache License 2.0. See the [LICENSE](LICENSE) file for details.

