buildscript {
    buildscript {
        repositories {
            google()
            mavenCentral()
        }
        buildscript {
            dependencies {
                classpath(libs.google.services)
            }
        }
    }
}
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    id("com.google.gms.google-services") version "4.4.2" apply false
}