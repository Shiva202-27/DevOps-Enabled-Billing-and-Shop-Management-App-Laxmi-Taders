plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.shiv.shop"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.shiv.shop"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation("com.airbnb.android:lottie:6.3.0")
    implementation("com.airbnb.android:lottie:6.1.0")
    implementation("nl.dionsegijn:konfetti-xml:2.0.5")
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("androidx.work:work-runtime:2.8.1")
    implementation("com.google.android.material:material:1.6.0")
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
    implementation("org.apache.poi:poi:5.2.2")
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation("com.google.zxing:core:3.5.2")
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")
}