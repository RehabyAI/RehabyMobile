import java.net.URI

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.rehaby.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.rehaby.app"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
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
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        viewBinding = true
    }
    // MediaPipe / Task graphs read models from the APK; compressed .task assets often crash native code.
    androidResources {
        noCompress += "task"
    }
}

// Ensure MediaPipe model is present — without it, assets.open() fails at runtime.
val downloadPoseModel by tasks.registering {
    val outFile = file("src/main/assets/models/pose_landmarker_lite.task")
    outputs.file(outFile)
    doLast {
        if (outFile.exists() && outFile.length() > 500_000L) {
            return@doLast
        }
        outFile.parentFile.mkdirs()
        val url = URI.create(
            "https://storage.googleapis.com/mediapipe-models/pose_landmarker/pose_landmarker_lite/float16/latest/pose_landmarker_lite.task"
        ).toURL()
        url.openStream().use { input ->
            outFile.outputStream().use { input.copyTo(it) }
        }
    }
}

tasks.named("preBuild") {
    dependsOn(downloadPoseModel)
}

dependencies {
    val cameraxVersion = "1.3.1"
    implementation("androidx.camera:camera-core:$cameraxVersion")
    implementation("androidx.camera:camera-camera2:$cameraxVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraxVersion")
    implementation("androidx.camera:camera-view:$cameraxVersion")

    implementation("com.google.mediapipe:tasks-vision:0.10.14")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.fragment:fragment-ktx:1.6.2")

    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")

    implementation("io.coil-kt:coil:2.5.0")
}
