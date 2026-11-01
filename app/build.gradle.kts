plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.services)
}

android {
    namespace = "com.example.astro_mobile"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.astro_mobile"
        minSdk = 30
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "API_BASE_URL", "\"https://api.astro-app.solutions/\"")
        buildConfigField("String", "CHAT_WEBSOCKET_URL", "\"wss://api.astro-app.solutions/ws\"")
        val resetBaseUrl = providers.gradleProperty("passwordResetApiBaseUrl")
            .orElse("https://astro-password-reset.app-4str0.workers.dev/").get()
        require(resetBaseUrl.startsWith("https://") && resetBaseUrl.endsWith("/") &&
                !resetBaseUrl.contains('"') && !resetBaseUrl.contains('\\')) {
            "passwordResetApiBaseUrl deve ser HTTPS e terminar em /"
        }
        buildConfigField("String", "PASSWORD_RESET_API_BASE_URL", "\"$resetBaseUrl\"")
        buildConfigField("String", "AI_API_BASE_URL", "\"https://ia.astro-app.solutions/\"")
    }

    buildFeatures {
        buildConfig = true
    }
    sourceSets.getByName("androidTest").assets.srcDir("src/test/resources")

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
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.messaging)
    implementation(libs.firebase.firestore)
    implementation(libs.work.runtime)
    implementation(libs.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.core.splashscreen)
    implementation(libs.material)
    implementation(libs.navigation.fragment)
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.lifecycle.livedata)
    implementation(libs.recyclerview)
    implementation(libs.glide)
    implementation(libs.swiperefreshlayout)
    implementation(libs.markwon.core)
    implementation(libs.markwon.tables)
    implementation(libs.markwon.strikethrough)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.gson)
    testImplementation(libs.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)
}
