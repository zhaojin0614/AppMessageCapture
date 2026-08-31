plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.aifactory.appmessagecapture"
    compileSdk = 35

    defaultConfig {
        // -PlegacyPackage=true 时以旧包名构建「桥接包」：装到还留着旧版
        // （com.aifactory.appmessagecapture）的设备上会原地升级，数据保留，
        // 且补上导入/导出功能，供无电脑场景下把旧数据搬到新包名 app
        applicationId = if (project.hasProperty("legacyPackage"))
            "com.aifactory.appmessagecapture"
        else
            "com.zhaojin.billcatch"
        minSdk = 26
        targetSdk = 35
        versionCode = 3
        versionName = "2.2.6"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
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
    testOptions {
        // BirthdayLog falls back to android.util.Log in JVM unit tests
        unitTests.isReturnDefaultValues = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    // 自定义 APK 输出文件名：捕账-v版本号.apk（正式包）；旧包名桥接构建
    // （-PlegacyPackage=true）输出 捕账-legacy.apk，避免与正式包同名互覆
    applicationVariants.all {
        val variant = this
        outputs.forEach { output ->
            if (output is com.android.build.gradle.internal.api.BaseVariantOutputImpl) {
                output.outputFileName =
                    if (project.hasProperty("legacyPackage"))
                        "捕账-legacy.apk"
                    else
                        "捕账-v${variant.versionName}.apk"
            }
        }
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    // Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    // Compose BOM (ui 1.8.x for rememberGraphicsLayer / RenderEffect blur)
    implementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(platform(libs.androidx.compose.bom))

    // Compose UI
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    // material3 pinned (compatible with glance-material3 1.1.1)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)

    // Lifecycle & ViewModel Compose
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // WorkManager
    implementation(libs.androidx.work.runtime.ktx)

    // Glance (AppWidget)
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)

    // Gson (JSON serialization for import/export)
    implementation(libs.gson)

    // Timber (Logging)
    implementation(libs.timber)

    // Lunar Calendar Library (Maven Central)
    implementation(libs.lunar)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // Testing
    testImplementation(libs.junit)
    // 本地单测中提供 XmlPullParser 实现（生产环境用系统自带）
    testImplementation(libs.kxml2)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
