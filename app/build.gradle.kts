plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")
}

private fun buyerManifestPlaceholders(
    profileId: String,
    launcherLabel: String
): Map<String, String> = mapOf(
    "clientProfileId" to profileId,
    "launcherLabel" to launcherLabel
)

private val lunchPointDefaultManifestPlaceholders = buyerManifestPlaceholders(
    profileId = "lunch-point",
    launcherLabel = "@string/branding_lunch_point_app_name"
)

private val onboardingCheckManifestPlaceholders = buyerManifestPlaceholders(
    profileId = "super-viandas",
    launcherLabel = "@string/branding_super_viandas_app_name"
)

android {
    namespace = "com.example.reservasapp"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.reservasapp"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        // Keep Lunch Point as the installed default.
        // For a quick pre-commit onboarding smoke check, temporarily swap to
        // `onboardingCheckManifestPlaceholders` and then switch back before committing.
        manifestPlaceholders += lunchPointDefaultManifestPlaceholders

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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        viewBinding = true
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.viewpager2:viewpager2:1.1.0")
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("com.github.bumptech.glide:glide:4.16.0")
    implementation(platform("com.google.firebase:firebase-bom:34.10.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-storage")
    implementation("com.google.android.gms:play-services-auth:20.7.0")
    implementation("androidx.annotation:annotation:1.6.0")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.6.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.1")

    testImplementation("junit:junit:4.13.2")
    testImplementation(kotlin("test"))
}
