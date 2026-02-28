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
import opensavvy.prepared.suite.SuiteDsl
import opensavvy.pursuit.users.User

fun SuiteDsl.verifyUserService(users: Prepared<User.Service>) = suite("Users") {

	suite("Telegram") {
		test("Can create a user by logging into Telegram") {
			val user = users().logInWithTelegram(
				telegramUserId = 42,
				username = "bob",
				fullName = "Bob",
			)

			check(user.read()?.username == "bob")
			check(user.read()?.fullName == "Bob")
		}

		test("Can log in into an existing Telegram user") {
			val initial = users().logInWithTelegram(
				telegramUserId = 42,
				username = "bob",
				fullName = "Bob",
			)

			// Log-in with the same user but different info
			val new = users().logInWithTelegram(
				telegramUserId = 42,
				username = "fred",
				fullName = "Fred",
			)

			check(initial == new) { "Logging in twice with the same Telegram user ID should not create a new account" }
			check(initial.read() == new.read())
		}

		test("Can create two different accounts") {
			val initial = users().logInWithTelegram(
				telegramUserId = 42,
				username = "bob",
				fullName = "Bob",
			)

			// Log-in with the same user but different ID, should create a new user
			val new = users().logInWithTelegram(
				telegramUserId = 43,
				username = "fred",
				fullName = "Fred",
			)

			check(initial != new) { "Logging in with a different Telegram user ID should create a new account" }
			check(initial.read()?.fullName == "Bob")
			check(new.read()?.fullName == "Fred")
		}
	}
}
