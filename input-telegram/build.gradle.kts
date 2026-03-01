/*
 * Copyright (c) 2025-2026, OpenSavvy and contributors.
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

	sourceSets.all {
		languageSettings.enableLanguageFeature("ContextParameters")
	}

	sourceSets.jvmMain.dependencies {
		api(projects.core)
		implementation(libs.telegram)
	}

	sourceSets.commonTest.dependencies {
		implementation(libsCommon.opensavvy.prepared.testBalloon)
		implementation(libsCommon.kotlin.test)
	}
}

library {
	name.set("Pursuit: Telegram bot UI")
	description.set("Telegram bot implementation to access Pursuit APIs")
	homeUrl.set("https://gitlab.com/opensavvy/pursuit")

	license.set {
		name.set("AGPL 3.0")
		url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
	}
}
