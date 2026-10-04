import java.util.Properties

val releaseSigningPropertiesFile = rootProject.file("keystore.properties")
val releaseSigningProperties = Properties().apply {
  if (releaseSigningPropertiesFile.isFile) {
    releaseSigningPropertiesFile.inputStream().use { load(it) }
  }
}

fun requiredReleaseSigningProperty(name: String): String =
  releaseSigningProperties.getProperty(name)?.takeIf { it.isNotBlank() }
    ?: throw GradleException("Missing '$name' in ${releaseSigningPropertiesFile.name}")

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.compose.compiler)
  alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "id.elclark.lunas"
    compileSdk = 37
    buildToolsVersion = "37.0.0"
    defaultConfig {
        applicationId = "id.elclark.lunas"
        minSdk = 26
        targetSdk = 37
        versionCode = 3
        versionName = "1.2"
    }

    signingConfigs {
        create("release") {
          if (releaseSigningPropertiesFile.isFile) {
            val keystoreFile = rootProject.file(requiredReleaseSigningProperty("storeFile"))
            if (!keystoreFile.isFile) {
              throw GradleException("Release signing keystore does not exist: ${keystoreFile.path}")
            }
            storeFile = keystoreFile
            storePassword = requiredReleaseSigningProperty("storePassword")
            keyAlias = requiredReleaseSigningProperty("keyAlias")
            keyPassword = requiredReleaseSigningProperty("keyPassword")
          }
        }
    }

    buildTypes {
          release {
              isMinifyEnabled = false
              if (releaseSigningPropertiesFile.isFile) {
                signingConfig = signingConfigs.getByName("release")
              }
              proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
          }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
      compose = true
      aidl = false
      buildConfig = true
      shaders = false
    }

    packaging {
      resources {
        excludes += "/META-INF/{AL2.0,LGPL2.1}"
      }
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
  val composeBom = platform(libs.androidx.compose.bom)
  implementation(composeBom)
  androidTestImplementation(composeBom)

  // Core Android dependencies
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.biometric)

  // Arch Components
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.kotlinx.serialization.json)

  // Compose
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.material.icons.extended)
  // Tooling
  debugImplementation(libs.androidx.compose.ui.tooling)
  // Instrumented tests
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  debugImplementation(libs.androidx.compose.ui.test.manifest)

  // Local tests: jUnit, coroutines, Android runner
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)

  // Instrumented tests: jUnit rules and runners
  androidTestImplementation(libs.androidx.test.core)
  androidTestImplementation(libs.androidx.test.ext.junit)
  androidTestImplementation(libs.androidx.test.runner)
  androidTestImplementation(libs.androidx.test.espresso.core)

  // Navigation
  implementation(libs.androidx.navigation3.ui)
  implementation(libs.androidx.navigation3.runtime)
  implementation(libs.androidx.lifecycle.viewmodel.navigation3)
}
