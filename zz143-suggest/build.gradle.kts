plugins {
    id("zz143.android-library")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.zz143.suggest"
    buildFeatures {
        compose = true
        viewBinding = true
    }
}

dependencies {
    api(project(":zz143-core"))
    implementation(project(":zz143-learn"))
    implementation(libs.kotlin.stdlib)
    implementation(libs.coroutines.core)
    implementation(libs.coroutines.android)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material3)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.foundation)

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.truth)
}
