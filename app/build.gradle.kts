import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
}

val releaseSigningProperties = Properties().apply {
    listOf("keystore.properties", "secrets.properties")
        .map(rootProject::file)
        .firstOrNull { it.exists() }
        ?.inputStream()
        ?.use(::load)
}

fun releaseSigningValue(environmentName: String, legacyPropertyName: String): String? =
    System.getenv(environmentName)
        ?: releaseSigningProperties.getProperty(environmentName)
        ?: releaseSigningProperties.getProperty(legacyPropertyName)

android {
    namespace = "com.example.kubmi"
    compileSdk = 35

    val releaseStoreFile =
        releaseSigningValue("KUBMI_RELEASE_STORE_FILE", "storeFile")
    val releaseStorePassword =
        releaseSigningValue("KUBMI_RELEASE_STORE_PASSWORD", "storePassword")
    val releaseKeyAlias =
        releaseSigningValue("KUBMI_RELEASE_KEY_ALIAS", "keyAlias")
    val releaseKeyPassword =
        releaseSigningValue("KUBMI_RELEASE_KEY_PASSWORD", "keyPassword")

    val releaseSigningConfig = if (
        listOf(
            releaseStoreFile,
            releaseStorePassword,
            releaseKeyAlias,
            releaseKeyPassword
        ).all { !it.isNullOrBlank() }
    ) {
        signingConfigs.create("release") {
            storeFile = rootProject.file(requireNotNull(releaseStoreFile))
            storePassword = releaseStorePassword
            keyAlias = releaseKeyAlias
            keyPassword = releaseKeyPassword
        }
    } else {
        null
    }

    defaultConfig {
        applicationId = "com.example.kubmi"
        minSdk = 23
        targetSdk = 35
        versionCode = 2
        versionName = "1.0.1"

    }

    buildTypes {
        release {
            isMinifyEnabled = false
            releaseSigningConfig?.let { signingConfig = it }
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
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.tv.foundation)
    implementation(libs.androidx.tv.material)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.accompanist.navigation.animation)
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp.logging)
    implementation(libs.coil.compose)
    implementation("io.coil-kt:coil-svg:2.5.0")
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.engage.tv)
    implementation("androidx.compose.animation:animation")
    ksp(libs.room.compiler)
    implementation(libs.work.runtime)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)
    implementation("androidx.hilt:hilt-work:1.2.0")
    // Required for @HiltWorker codegen (WorkManager + Hilt integration)
    ksp("androidx.hilt:hilt-compiler:1.2.0")
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.gson)
    implementation(libs.jsoup)
    implementation("com.jakewharton.timber:timber:5.0.1")
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    implementation("com.jcraft:jsch:0.1.55")
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
