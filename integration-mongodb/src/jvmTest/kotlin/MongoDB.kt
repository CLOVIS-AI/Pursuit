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

package opensavvy.pursuit.integration.mongodb

import kotlinx.serialization.Serializable
import opensavvy.prepared.runner.testballoon.preparedSuite
import opensavvy.prepared.suite.config.CoroutineTimeout
import kotlin.time.Duration.Companion.minutes

@Serializable
data class Dummy(val name: String)

val MongoDB by preparedSuite {

	// region Dummy collection to verify DB connection

	val dummyCollection by testCollection<Dummy>("dummy")

	test("Connect to the database", CoroutineTimeout(1.minutes)) {
		dummyCollection().insertOne(Dummy("bob"))

		check(dummyCollection().find().toList() == listOf(Dummy("bob")))
	}

	// endregion
}
