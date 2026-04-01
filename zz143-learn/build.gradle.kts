plugins {
    id("zz143.android-library")
}

android {
    namespace = "com.zz143.learn"
}

dependencies {
    api(project(":zz143-core"))
    implementation(libs.kotlin.stdlib)
    implementation(libs.coroutines.core)
    implementation(libs.coroutines.android)

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.truth)
}
