import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

// --- Release signing -------------------------------------------------------
// Signing material is NEVER committed: keystore.properties and keystore/ are
// gitignored (see .gitignore, docs/release/KEYSTORE.md). When the properties
// file or the keystore is absent (CI, fresh clone) the release build simply
// stays unsigned, so assembleRelease / bundleRelease still succeed.
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        keystorePropertiesFile.inputStream().use { load(it) }
    }
}
val hasReleaseKeystore = keystorePropertiesFile.exists() &&
    keystoreProperties.getProperty("storeFile")?.let { rootProject.file(it).exists() } == true &&
    keystoreProperties.getProperty("storePassword") != null &&
    keystoreProperties.getProperty("keyAlias") != null

// Privacy policy URL is supplied by the owner at build time, e.g.
//   .\gradlew.bat :app:bundleRelease -Psabeel.privacyPolicyUrl=https://example.org/privacy
// Exposed as BuildConfig.PRIVACY_POLICY_URL; empty means "not configured".
val privacyPolicyUrl = providers.gradleProperty("sabeel.privacyPolicyUrl").orElse("").get()

android {
    namespace = "com.sabeel.steps"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.sabeel.steps"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        buildConfigField(
            "String",
            "PRIVACY_POLICY_URL",
            "\"${privacyPolicyUrl}\"",
        )
    }

    signingConfigs {
        if (hasReleaseKeystore) {
            create("release") {
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            // D12: minified + resource-shrunk release. Debug stays unminified.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            if (hasReleaseKeystore) {
                signingConfig = signingConfigs.getByName("release")
            }
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
        compose = true
        buildConfig = true
    }

    // D7: keep Arabic resources in every AAB install. The in-app language switch
    // changes the configuration at runtime and cannot download Play language
    // splits, so with splitting enabled an English-locale install would show
    // English strings after switching to Arabic.
    bundle {
        language {
            enableSplit = false
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

// Room schema export for reviewable migrations. The Lead commits app/schemas
// after the first build (docs/review/2026-11-17/fix-release.md has the command).
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    // Core / lifecycle / activity
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.activity:activity-compose:1.9.3")

    // Compose (BOM-managed versions)
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.8.5")

    // Compose foundation + core icons (BOM-managed)
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material:material-icons-core")

    // Lifecycle + ViewModel Compose integration
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")

    // Room (KSP compiler)
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // DataStore preferences
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Health Connect client: optional read-only source for phones with no hardware step sensor.
    // Read steps only; nothing is ever written to Health Connect.
    implementation("androidx.health.connect:connect-client:1.1.0")

    // WorkManager
    implementation("androidx.work:work-runtime-ktx:2.10.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    // Tests
    testImplementation("junit:junit:4.13.2")

    // Debug tooling
    debugImplementation("androidx.compose.ui:ui-tooling")
}
