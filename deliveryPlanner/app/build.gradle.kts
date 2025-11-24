plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.fulechuan.deliveryplanner"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.fulechuan.deliveryplanner"
        minSdk = 25
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
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        // 启用 Core Library Desugaring
        isCoreLibraryDesugaringEnabled = true
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}
// 【新增】放在 android {} 块的下面，或者 dependencies {} 的上面
configurations {
    all {
        exclude(group = "com.intellij", module = "annotations")
    }
}
dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    //图标库
    implementation(libs.androidx.compose.material.icons.extended)
    // 协程 (用于算法和网络请求)
//    implementation(libs.org.jetbrains.kotlinx.kotlinx.coroutines.android)
    implementation(libs.androidx.material3)
    // Room Database
    implementation(libs.androidx.room.room.runtime)
//    implementation(libs.androidx.room.room.compiler)
    implementation(libs.androidx.room.common.jvm)
    implementation(libs.androidx.room.ktx)
    implementation(libs.play.services.maps)
    implementation(libs.androidx.compose.runtime)
    implementation(libs.androidx.compose.material)
    implementation(libs.play.services.tagmanager.v4.impl)
    ksp(libs.androidx.room.room.compiler)
    // 添加 Desugaring 库依赖
    coreLibraryDesugaring(libs.com.android.tools.desugar.jdk.libs) // 使用最新的版本}
    //3D地图so及jar,已经包含定位和搜索功能无需单独引用
    implementation("com.amap.api:3dmap-location-search:latest.integration")
//    // 高德地图 3D地图 SDK
//    implementation(libs.com.amap)
//    // 高德地图 搜索 SDK (用于地址转坐标，路径规划)
//    implementation(libs.com.amap.api.search)
//    // 高德地图 定位 SDK
//    implementation(libs.com.amap.api.location)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

}