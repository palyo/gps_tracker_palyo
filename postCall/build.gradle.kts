plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.post.call.info"
    compileSdk = 35
    buildFeatures {
        buildConfig = true
        viewBinding = true
    }

    defaultConfig {
        minSdk = 24
        targetSdk = 36

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
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)

    // Used by ui/fragment/* and adapter/*
    implementation("androidx.fragment:fragment-ktx:1.8.4")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.viewpager2:viewpager2:1.1.0")
    implementation(libs.lifecycle.runtime)

    // appStartup/* initializers
    implementation("androidx.startup:startup-runtime:1.2.0")

    // Native ad shimmer placeholders
    implementation("com.facebook.shimmer:shimmer:0.5.0")

    // AdMob + mediation (PostCallBannerAds / PostCallNativeAds)
    implementation("com.google.android.gms:play-services-ads:25.2.0")
    implementation("com.google.ads.mediation:applovin:13.6.2.0")
    implementation("com.google.ads.mediation:inmobi:11.2.0.0")
    implementation("com.google.ads.mediation:ironsource:9.4.0.0")
    implementation("com.unity3d.ads:unity-ads:4.17.0")
    implementation("com.google.ads.mediation:unity:4.17.0.0")

    // FirebaseAnalytics imports in initializers
    implementation(platform("com.google.firebase:firebase-bom:33.14.0"))
    implementation("com.google.firebase:firebase-analytics")
}