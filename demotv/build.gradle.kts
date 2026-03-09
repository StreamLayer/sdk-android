plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "io.streamlayer.demotv"
    compileSdk = 36

    defaultConfig {
        applicationId = "io.streamlayer.demotv"
        minSdk = 23
        versionCode = 1
        versionName = "1"
        buildConfigField(
            "String",
            "SL_SDK_KEY",
            "\"679acc583ca0ad3ae5114a9a50a1646179cdc11f93c210277b30b8e3a807e92d\""
        )
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    buildTypes {
        getByName("debug") {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android.txt"),
                "proguard-debug-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_17.toString()
    }
    lint {
        abortOnError = false
    }
}
val Project.versions: Map<String, Any>
    get() = rootProject.extra["versions"] as Map<String, Any>

dependencies {
    implementation(project(":common"))
    val streamlayer =
        versions["streamlayer"] as String // latest 2.20.0-beta.9-kotlin1.9.23 for now
    val appcompat = versions["appcompat"] as String
    val coreKtx = versions["appcompat"] as String
    val constraintlayout = versions["constraintlayout"] as String
    val fragment = versions["fragment"] as String

    implementation("androidx.appcompat:appcompat:$appcompat")
    implementation("androidx.core:core-ktx:$coreKtx")
    implementation("androidx.constraintlayout:constraintlayout:$constraintlayout")
    implementation("androidx.fragment:fragment-ktx:$fragment")
    implementation("io.streamlayer:androidsdk:$streamlayer")
//    implementation("io.streamlayer:android-media3:$streamlayer")  // in case media3
    implementation("io.streamlayer:android-exoplayer:${streamlayer}")

    // if you need google pal, but be aware because it's adding additional dependencies
    // to com.google.android.gms:play-services-pal:23.0.0
    // reed https://developers.google.com/ad-manager/pal/android/get-started?hl=en
//    implementation("io.streamlayer:android-googlepal:streamlayer")

    implementation("androidx.tv:tv-foundation:1.0.0-alpha12")
    implementation("androidx.tv:tv-material:1.0.1")
}
