plugins {
    id("zz143.android-library")
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.zz143.android"
    buildFeatures {
        compose = true
    }
}

dependencies {
    api(project(":zz143-core"))
    api(project(":zz143-capture"))
    api(project(":zz143-learn"))
    api(project(":zz143-suggest"))
    api(project(":zz143-replay"))

    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlin.reflect)
    implementation(libs.coroutines.core)
    implementation(libs.coroutines.android)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.startup)
    implementation(libs.androidx.navigation.fragment)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.foundation)

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.truth)
}
