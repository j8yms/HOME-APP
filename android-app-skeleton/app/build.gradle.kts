import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.example.householdapp"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.householdapp"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        
        val appsScriptBaseUrl: String = (project.findProperty("APPS_SCRIPT_BASE_URL") as? String)
            ?: System.getenv("APPS_SCRIPT_BASE_URL")
            ?: "https://script.google.com/"
        val appsScriptDeploymentPath: String = (project.findProperty("APPS_SCRIPT_DEPLOYMENT_PATH") as? String)
            ?: System.getenv("APPS_SCRIPT_DEPLOYMENT_PATH")
            ?: "macros/s/AKfycbySdX4LCqpZVaH6N0a-CuTE0kh9qC5BCmGBE2JnUsl3j9O7XtM7rS2Jg46idWkJ_gD3TQ/exec"

        buildConfigField("String", "APPS_SCRIPT_BASE_URL", "\"$appsScriptBaseUrl\"")
        buildConfigField("String", "APPS_SCRIPT_DEPLOYMENT_PATH", "\"$appsScriptDeploymentPath\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        // signingConfigs is defined above buildTypes so it can be referenced here
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Use the `release` signing config if present. The actual keystore
            // should be provided by the developer (see keystore.properties.example).
            signingConfig = signingConfigs.findByName("release")
        }
    }

    // Signing configuration: reads `keystore.properties` if present, otherwise
    // falls back to environment variables. Place a `keystore.properties` file
    // at the project root or set the environment variables listed below.
    signingConfigs {
        create("release") {
            val keystorePropertiesFile = rootProject.file("keystore.properties")
            val keystoreProperties = Properties()
            if (keystorePropertiesFile.exists()) {
                keystorePropertiesFile.inputStream().use { keystoreProperties.load(it) }
            }

            storeFile = file(
                keystoreProperties.getProperty("storeFile")
                    ?: System.getenv("KEYSTORE_FILE")
                    ?: "keystore.jks"
            )
            storePassword = keystoreProperties.getProperty("storePassword")
                ?: System.getenv("KEYSTORE_PASSWORD")
            keyAlias = keystoreProperties.getProperty("keyAlias")
                ?: System.getenv("KEY_ALIAS")
            keyPassword = keystoreProperties.getProperty("keyPassword")
                ?: System.getenv("KEY_PASSWORD")
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



    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    val bom = platform("androidx.compose:compose-bom:2024.06.00")

    implementation(bom)
    androidTestImplementation(bom)

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.3")
    implementation("androidx.activity:activity-compose:1.9.1")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("com.google.android.material:material:1.10.0")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.3")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-moshi:2.11.0")
    implementation("com.squareup.moshi:moshi-kotlin:1.15.1")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
