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

package opensavvy.pursuit.tests.users

import opensavvy.prepared.suite.Prepared
import opensavvy.prepared.suite.PreparedProvider
import opensavvy.prepared.suite.TestDsl
import opensavvy.prepared.suite.prepared
import opensavvy.prepared.suite.random.nextLong
import opensavvy.prepared.suite.random.random
import opensavvy.pursuit.users.User
import opensavvy.pursuit.users.executeAs

/**
 * Generates a new user specifically for this test.
 */
// 'internal' because context parameters are experimental
context(_: TestDsl)
internal suspend fun User.Service.testUser(
	name: String? = null,
	fullName: String? = null,
): User.Ref {
	val telegramId = contextOf<TestDsl>().random.nextLong()

	return this.logInWithTelegram(
		telegramUserId = telegramId,
		username = name ?: "User $telegramId",
		fullName = fullName ?: "User $telegramId",
	)
}

/**
 * Generates a new user specifically for this test.
 */
fun Prepared<User.Service>.testUser(
	name: String? = null,
	fullName: String? = null,
): PreparedProvider<User.Ref> = prepared {
	this@testUser().testUser(name, fullName)
}

suspend fun TestDsl.executeAs(user: Prepared<User.Ref>, block: suspend () -> Unit) =
	executeAs(user(), block)
