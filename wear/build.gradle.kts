plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "io.github.gopalsuryak.galaxywatch.wear"
    compileSdk = 36

    defaultConfig {
        // Must match the phone app exactly: the Data Layer only delivers between apps
        // with the same application ID and the same signing key.
        applicationId = "io.github.gopalsuryak.galaxywatch"
        minSdk = 30 // Wear OS 3, the first version on Galaxy Watch4
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(project(":shared"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.wear.compose.material)
    implementation(libs.wear.compose.foundation)

    implementation(libs.health.services.client)
    implementation(libs.androidx.concurrent.futures.ktx)
    // Health Services returns ListenableFuture but only ships Guava at runtime.
    implementation(libs.guava)
    implementation(libs.play.services.wearable)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)
}
