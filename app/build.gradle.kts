plugins {
    // alias(libs.plugins.android.application) // Assuming this alias is correctly defined in libs.versions.toml
    id("com.android.application") // Using standard ID just in case alias isn't set up yet
}

android {
    namespace = "com.example.eventcountdownwidget"
    compileSdk = 35 // Keep this high (31+)

    defaultConfig {
        applicationId = "com.example.eventcountdownwidget"
        // *** CHANGE: Lower minSdk for broader compatibility ***
        minSdk = 34 // Changed from 34 to 24 (Android 7.0 Nougat)
        targetSdk = 35 // Keep targetSdk high
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
        // Changed to JavaVersion.VERSION_1_8 for wider compatibility often needed with lower minSdk
        // Keep 11 if you are sure all libraries support it and you need Java 11 features
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    // If using Kotlin, you might need kotlinOptions block here
    // kotlinOptions {
    //    jvmTarget = '1.8'
    // }
}

dependencies {

    implementation(libs.appcompat) // Assuming alias exists
    implementation(libs.material)  // Assuming alias exists
    // implementation(libs.activity) // Often included transitively, keep if needed directly
    // implementation(libs.constraintlayout) // Keep if ConstraintLayout is used elsewhere

    // *** ADD: androidx.core dependency for ColorUtils ***
    // Assuming alias androidx.core.ktx exists in libs.versions.toml
    implementation("androidx.work:work-runtime:2.8.1")
    implementation("androidx.core:core-ktx:1.12.0")
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    // Or use direct string if alias doesn't exist (replace version if needed):
    // implementation("androidx.core:core-ktx:1.12.0") // Example version

    implementation ("com.google.android.material:material:1.5.0")


    testImplementation(libs.junit) // Assuming alias exists
    androidTestImplementation(libs.ext.junit) // Assuming alias exists
    androidTestImplementation(libs.espresso.core) // Assuming alias exists
}