plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.mediscan"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.mediscan"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)
    implementation("androidx.camera:camera-core:1.4.2")
    implementation("androidx.camera:camera-camera2:1.4.2")
    implementation("androidx.camera:camera-lifecycle:1.4.2")
    implementation("androidx.camera:camera-view:1.4.2")
    implementation("com.google.mlkit:text-recognition:16.0.1")
    implementation("com.google.mlkit:barcode-scanning:17.3.0")
    implementation("com.google.android.material:material:1.14.0")
    implementation("androidx.fragment:fragment:1.9.0")
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("androidx.room:room-runtime:2.8.5")
    implementation("androidx.recyclerview:recyclerview:1.4.0")
    implementation("androidx.work:work-runtime:2.11.2")
    implementation("androidx.core:core:1.19.0")
    annotationProcessor("androidx.room:room-compiler:2.8.5")

}