plugins {
    alias(libs.plugins.android)
    alias(libs.plugins.compose)
}

android {
    namespace = "io.github.dot166.flux"
    compileSdk = 36

    defaultConfig {
        applicationId = "io.github.dot166.flux"
        minSdk = 31
        targetSdk = 36
        versionCode = 2
        versionName = versionCode.toString()
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures { compose = true }
}

dependencies {
    implementation(libs.j.lib)
    implementation(libs.rssparser)
    implementation(libs.androidx.ui.android)
    implementation(libs.androidx.material3.android)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.common)
    implementation(libs.androidx.media3.session)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.coil.compose)
    implementation(libs.accompanist.drawablepainter)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.ui)
    implementation(libs.kotlinx.coroutines.guava)
    implementation(libs.glide)
    annotationProcessor(libs.compiler)
}