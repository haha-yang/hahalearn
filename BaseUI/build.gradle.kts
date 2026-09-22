plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.haha.baseui"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        minSdk = 24

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
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
    buildFeatures {
        dataBinding = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    api("androidx.core:core-ktx:1.10.0")
    api("androidx.appcompat:appcompat:1.1.0")
    api("androidx.activity:activity-ktx:1.5.0")
    api("androidx.fragment:fragment-ktx:1.4.1")
    api("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.0")
    api("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    api("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    api("io.reactivex.rxjava2:rxjava:2.2.20")
    api(project(":DOFLog"))

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}