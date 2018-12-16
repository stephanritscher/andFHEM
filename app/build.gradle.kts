import java.net.URI

/*
 * AndFHEM - Open Source Android application to control a FHEM home automation
 * server.
 *
 * Copyright (c) 2011, Matthias Klass or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU GENERAL PUBLIC LICENSE, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU GENERAL PUBLIC LICENSE
 * for more details.
 *
 * You should have received a copy of the GNU GENERAL PUBLIC LICENSE
 * along with this distribution; if not, write to:
 *   Free Software Foundation, Inc.
 *   51 Franklin Street, Fifth Floor
 *   Boston, MA  02110-1301  USA
 */


buildscript {
    val kotlinVersion: String by project
    repositories {
        google()
        mavenCentral()
        maven { setUrl("https://oss.sonatype.org/content/repositories/snapshots/") }
        maven { setUrl("https://maven.fabric.io/public") }
        jcenter()
    }

    dependencies {
        classpath(group = "com.github.jcandksolutions.gradle", name = "android-unit-test", version = "2.1.1")
        classpath(group = "com.jakewharton.sdkmanager", name = "gradle-plugin", version = "0.12.0")
        classpath(group = "de.mobilej.unmock", name = "UnMockPlugin", version = "0.6.4")
        classpath(group = "co.riiid", name = "gradle-github-plugin", version = "0.4.2")
        classpath(group = "org.jetbrains.kotlin", name = "kotlin-gradle-plugin", version = kotlinVersion)
        classpath(group = "com.google.gms", name = "google-services", version = "4.2.0")
        classpath(group = "com.github.triplet.gradle", name = "play-publisher", version = "1.2.0")
        classpath(group = "org.jetbrains.kotlin", name = "kotlin-serialization", version = kotlinVersion)
        classpath(group = "io.fabric.tools", name = "gradle", version = "1.25.4")
        classpath(group = "com.google.firebase", name = "firebase-plugins", version = "1.1.5")
    }
}

val kotlinVersion: String by project
val kotlinSerializationVersion = "0.9.1"
val supportLibVersion = "28.0.0"
val ankoVersion = "0.10.8"
val architectureComponentsVersion = "1.1.1"
val glideVersion = "4.8.0"
val daggerVersion = "2.19"
val coroutinesVersion = "1.0.1"

plugins {
    id("net.researchgate.release") version "2.6.0"
    id("com.android.application") version "3.2.1"
    kotlin("android").version("1.2.31")
    kotlin("android.extensions").version("1.2.31")
    kotlin("kapt").version("1.2.31")
}

repositories {
    mavenCentral()
    jcenter()
    maven { setUrl("https://plugins.gradle.org/m2/") }
    maven { setUrl("https://dl.bintray.com/mamohr/maven") }
    maven { setUrl("https://oss.sonatype.org/content/repositories/snapshots/") }
    maven { setUrl("http://partnerdemo.artifactoryonline.com/partnerdemo/snapshots") }
    maven { setUrl("https://jitpack.io") }
    maven { setUrl("https://maven.google.com") }
    maven { setUrl("https://kotlin.bintray.com/kotlinx") }
}


apply(plugin = "de.mobilej.unmock")
apply(plugin = "co.riiid.gradle")
apply(plugin = "com.github.triplet.play")
apply(plugin = "kotlinx-serialization")
apply(plugin = "io.fabric")

configurations.all {
    exclude(group = "com.google.guava", module = "listenablefuture")
    // exclude, due to https://issuetracker.google.com/issues/116154359
}

val unmock = configurations.findByName("unmock")!!
dependencies {
    unmock(group = "org.robolectric", name = "android-all", version = "7.0.0_r1-robolectric-0")

    implementation(fileTree(mapOf("dir" to "./libs", "include" to "*.jar")))
    implementation(project(":external-dep"))

    implementation("com.google.firebase", name = "firebase-appindexing", version = "17.1.0")
    implementation(group = "com.google.firebase", name = "firebase-messaging", version = "17.3.4")
    implementation(group = "com.google.firebase", name = "firebase-ads", version = "17.1.2")
    implementation(group = "com.google.firebase", name = "firebase-perf", version = "16.2.2")
    implementation(group = "com.google.android.material", name = "material", version = "1.1.0-alpha02")
    implementation(group = "com.google.code.gson", name = "gson", version = "2.8.5")
    implementation(group = "com.google.guava", name = "guava", version = "23.4-android") {
        exclude(group = "com.google.code.findbugs")
    }
    implementation(group = "com.google.http-client", name = "google-http-client-android", version = "1.22.0") {
        exclude(group = "com.google.code.findbugs")
        exclude(group = "org.apache.httpcomponents")
    }
    implementation(group = "com.google.dagger", name = "dagger", version = daggerVersion)
    kapt(group = "com.google.dagger", name = "dagger-compiler", version = daggerVersion)
    implementation(group = "com.google.errorprone", name = "error_prone_annotations", version = "2.2.0")
    implementation(group = "com.google.j2objc", name = "j2objc-annotations", version = "1.3")

    implementation(group = "androidx.multidex", name = "multidex", version = "2.0.0")
    implementation(group = "androidx.cardview", name = "cardview", version = "1.0.0")
    implementation(group = "androidx.recyclerview", name = "recyclerview", version = "1.1.0-alpha01")
    implementation(group = "androidx.percentlayout", name = "percentlayout", version = "1.0.0")
    implementation(group = "androidx.annotation", name = "annotation", version = coroutinesVersion)
    implementation(group = "androidx.room", name = "room-runtime", version = "2.1.0-alpha03")
    kapt(group = "androidx.room", name = "room-compiler", version = "2.1.0-alpha03")

    implementation(group = "commons-net", name = "commons-net", version = "3.3")
    implementation(group = "commons-codec", name = "commons-codec", version = "1.11")
    implementation(group = "org.apache.commons", name = "commons-lang3", version = "3.7")


    compileOnly(group = "javax.annotation", name = "jsr250-api", version = "1.0")
    implementation(group = "com.github.angads25", name = "filepicker", version = "1.1.0")
    implementation(group = "joda-time", name = "joda-time", version = "2.9.9")
    implementation(group = "org.slf4j", name = "slf4j-android", version = "1.7.12")
    implementation(group = "org.apmem.tools", name = "layouts", version = "1.10")
    implementation(group = "net.lingala.zip4j", name = "zip4j", version = "1.3.2")
    implementation(group = "com.github.PhilJay", name = "MPAndroidChart", version = "v3.0.3")
    implementation(group = "com.squareup.picasso", name = "picasso", version = "2.5.1")
    implementation(group = "com.github.alexfu", name = "Phoenix", version = "1.0.0")
    implementation(group = "com.github.bumptech.glide", name = "glide", version = glideVersion)
    kapt(group = "com.github.bumptech.glide", name = "compiler", version = glideVersion)
    implementation(group = "com.github.bumptech.glide", name = "okhttp3-integration", version = glideVersion) {
        exclude(group = "glide-parent")
    }
    implementation(group = "com.crashlytics.sdk.android", name = "crashlytics", version = "2.9.7")

    implementation(group = "org.jetbrains.kotlin", name = "kotlin-stdlib-jdk7", version = kotlinVersion)
    implementation(group = "org.jetbrains.kotlinx", name = "kotlinx-serialization-runtime", version = kotlinSerializationVersion) {
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-reflect")
    }
    implementation(group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-android", version = coroutinesVersion)
    implementation(group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-core", version = coroutinesVersion)

    // Anko Layouts
    implementation(group = "org.jetbrains.anko", name = "anko-sdk25", version = ankoVersion)
    // sdk15, sdk19, sdk21, sdk23 are also available
    implementation(group = "org.jetbrains.anko", name = "anko-appcompat-v7", version = ankoVersion)
    // Coroutine listeners for Anko Layouts
    implementation(group = "org.jetbrains.anko", name = "anko-sdk25-coroutines", version = ankoVersion)
    implementation(group = "org.jetbrains.anko", name = "anko-appcompat-v7-coroutines", version = ankoVersion)
    implementation(group = "org.jetbrains.anko", name = "anko-coroutines", version = ankoVersion)


    testImplementation(group = "junit", name = "junit", version = "4.12")
    testImplementation(group = "org.mockito", name = "mockito-core", version = "2.23.4")
    testImplementation(group = "com.nhaarman", name = "mockito-kotlin", version = "1.5.0")
    testImplementation(group = "com.tngtech.java", name = "junit-dataprovider", version = "1.12.0")
    testImplementation(group = "org.assertj", name = "assertj-core", version = "3.11.1")
    testImplementation(group = "org.assertj", name = "assertj-guava", version = "1.3.1") {
        exclude(group = "com.google.guava")
    }

    androidTestImplementation(group = "androidx.test", name = "runner", version = "1.1.1")
    androidTestImplementation(group = "androidx.test", name = "rules", version = "1.1.1")
    androidTestImplementation(group = "androidx.test.espresso", name = "espresso-core", version = "3.1.1")
    androidTestImplementation(group = "androidx.test.espresso", name = "espresso-contrib", version = "3.1.1")
}

apply(from = "build-includes/whatsnew.gradle.kts")
apply(from = "build-includes/resourceIdMapper.gradle.kts")
apply(from = "build-includes/deviceConfiguration.gradle.kts")
apply(from = "build-includes/github.gradle")
apply(from = "build-includes/release.gradle")
apply(from = "build-includes/android.gradle")
apply(from = "build-includes/unmock.gradle")
apply(from = "build-includes/test-resources.gradle")
apply(plugin = "com.google.gms.google-services")
apply(plugin = "com.google.firebase.firebase-perf")