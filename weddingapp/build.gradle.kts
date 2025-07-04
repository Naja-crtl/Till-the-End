plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.weddingapp"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.weddingapp"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        getByName("release") {
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
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.play.services.location)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // Firebase platform with Firebase Authentication, Firestore, Analytics, etc.
    implementation(platform("com.google.firebase:firebase-bom:33.13.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.firebaseui:firebase-ui-firestore:8.0.1")
    implementation("com.google.firebase:firebase-storage")

    // Glide for image loading
    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")

    // Play Services Authentication (optional)
    implementation("com.google.android.gms:play-services-auth:20.7.0")

    // MPAndroidChart for data visualization
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
    implementation ("com.google.android.material:material:1.9.0")

    // Material Calendar View with exclusion for legacy support libraries
    implementation("com.prolificinteractive:material-calendarview:1.4.3") {
        exclude(group = "com.android.support", module = "support-compat")
    }

    implementation("com.squareup.okhttp3:okhttp:4.9.3")

    // Google Maps for Android
    implementation("com.google.android.gms:play-services-maps:18.1.0")

    // AdMob for Android
    implementation("com.google.android.gms:play-services-ads:24.2.0")
}

apply(plugin = "com.google.gms.google-services")
