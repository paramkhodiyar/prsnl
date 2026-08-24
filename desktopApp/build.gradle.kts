plugins {
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
    kotlin("jvm")
}

sourceSets {
    main {
        kotlin.srcDirs("src/jvmMain/kotlin")
    }
}

dependencies {
    implementation(libs.kotlinx.serialization.json)

    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)
}

compose.desktop {
    application {
        mainClass = "com.prsnl.desktop.MainKt"
        nativeDistributions {
            targetFormats(
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Dmg,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Pkg
            )
            packageName = "prsnl-mac"
            packageVersion = "1.0.0"
            description = "prsnl Digital Notebook Companion Viewer for macOS"
        }
    }
}
