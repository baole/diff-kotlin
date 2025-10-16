plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.maven.publish)
}

// Maven coordinates
val mavenGroup = "io.github.baole"
val mavenArtifactId = "diff-kotlin"
val mavenVersion = "0.0.3"

android {
    namespace = "io.github.baole.diffkotlin"
    compileSdk = libs.versions.android.compile.sdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.android.min.sdk.get().toInt()
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

kotlin {
    androidTarget()
    jvm() {
        testRuns.getByName("test") {
            executionTask.configure {
                useJUnitPlatform()
            }
        }
    }
    js(IR) {
        browser()
    }
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        val commonMain by getting {
            dependencies {
                // kotlin-stdlib-common is included automatically by Kotlin Multiplatform
                // No need to explicitly declare it
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        val jvmMain by getting {
            dependencies {
                // kotlin-stdlib is included automatically
                // No need to explicitly declare it
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(libs.mockk)
            }
        }
        val androidMain by getting {
        }
        val jsMain by getting {
        }
        val iosX64Main by getting {
        }
        val iosArm64Main by getting {
        }
        val iosSimulatorArm64Main by getting {
        }
    }
}

//import com.vanniktech.maven.publish.SonatypeHost

mavenPublishing {
    publishToMavenCentral()

    // Configure signing to work with both local keyring and GitHub Actions in-memory keys
    signAllPublications()

    coordinates(mavenGroup, mavenArtifactId, mavenVersion)

    pom {
        name = "Diff-kotlin library"
        description = "Myers diff for Kotlin Multiplatform"
        inceptionYear = "2024"
        url = "https://github.com/baole/diff-kotlin"
        licenses {
            license {
                name = "The Apache License, Version 2.0"
                url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
                distribution = "repo"
            }
        }
        developers {
            developer {
                id = "baole"
                name = "Bao Le"
                email = "leducbao@gmail.com"
            }
        }
        scm {
            url = "https://github.com/baole/diff-kotlin"
            connection = "scm:git:git://github.com/baole/diff-kotlin.git"
            developerConnection = "scm:git:ssh://git@github.com/baole/diff-kotlin.git"
        }
    }
}

// Ktlint configuration
ktlint {
    version.set("1.0.1")
    android.set(true)
    outputColorName.set("RED")
    filter {
        exclude("**/build/**")
        exclude { it.file.path.contains("build.gradle.kts") }
    }
}
