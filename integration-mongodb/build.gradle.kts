@file:OptIn(ExperimentalWasmDsl::class)

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
	alias(opensavvyConventions.plugins.base)
	alias(opensavvyConventions.plugins.kotlin.library)
	alias(libsCommon.plugins.kotlinx.serialization)
	alias(libsCommon.plugins.testBalloon)
}

kotlin {
	jvm()

	sourceSets.jvmMain.dependencies {
		api(projects.core)
		api(libs.ktmongo)
		implementation(libs.kotlinx.serialization)
	}

	sourceSets.commonTest.dependencies {
		implementation(libsCommon.opensavvy.prepared.testBalloon)
	}
}

library {
	name.set("Pursuit: MongoDB implementation")
	description.set("MongoDB-based implementation of the Pursuit entities")
	homeUrl.set("https://gitlab.com/opensavvy/pursuit")

	license.set {
		name.set("AGPL 3.0")
		url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
	}
}
