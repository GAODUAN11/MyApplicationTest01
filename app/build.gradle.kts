plugins {
    alias(libs.plugins.android.application)
}

android {
    signingConfigs {
        getByName("debug") {
            keyAlias = "key0"
            storeFile = file("D:\\A-development-project\\AndroidProject\\Qianming\\Test01Shudu.jks")
            storePassword = "200662.Gd"
            keyPassword = "200662.Gd"
        }
        create("release") {
            storeFile = file("D:\\A-development-project\\AndroidProject\\Qianming\\Test01Shudu.jks")
            storePassword = "200662.Gd"
            keyAlias = "key0"
            keyPassword = "200662.Gd"
        }
    }
    namespace = "com.example.myapplication_test01"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.myapplication_test01"
        minSdk = 24
        targetSdk = 36
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
            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}