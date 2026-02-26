/*
 * Copyright (c) 2026, OpenSavvy and contributors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

@file:OptIn(ExperimentalWasmDsl::class)

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
	alias(opensavvyConventions.plugins.base)
	alias(opensavvyConventions.plugins.kotlin.library)
	alias(libsCommon.plugins.testBalloon)
}

kotlin {
	jvm()
	js {
		browser()
		nodejs()
	}
	linuxX64()
	linuxArm64()
	macosX64()
	macosArm64()
	iosArm64()
	iosX64()
	iosSimulatorArm64()
	watchosX64()
	watchosArm32()
	watchosArm64()
	watchosSimulatorArm64()
	tvosX64()
	tvosArm64()
	tvosSimulatorArm64()
	mingwX64()
	wasmJs {
		browser()
		nodejs()
	}
	wasmWasi {
		nodejs()
	}

	sourceSets.commonMain.dependencies {
		api(projects.core)
		implementation(libsCommon.opensavvy.prepared.testBalloon)
	}

	sourceSets.commonTest.dependencies {
		implementation(libsCommon.bundles.testBalloon)
	}

	compilerOptions {
		freeCompilerArgs.add("-Xcontext-parameters")
	}
}

library {
	name.set("Pursuit testing utilities")
	description.set("Testing utilities to verify the behavior of a Pursuit integration, including in-memory fake implementations")
	homeUrl.set("https://gitlab.com/opensavvy/pursuit")

	license.set {
		name.set("AGPL 3.0")
		url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
	}
}
