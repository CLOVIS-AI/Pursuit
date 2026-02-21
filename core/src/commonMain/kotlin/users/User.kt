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

package opensavvy.pursuit.users

import opensavvy.pursuit.base.BaseEntity
import opensavvy.pursuit.base.BaseRef
import opensavvy.pursuit.base.BaseService

/**
 * A user of the Pursuit platform.
 *
 * A user represents the authentication of a human in the application.
 */
data class User(
	val fullName: String,
	val username: String,
) : BaseEntity {

	interface Service : BaseService<User> {

		/**
		 * Logs in with a Telegram [telegramUserId].
		 *
		 * If no account exists for this [telegramUserId], this method creates it and links it to the Telegram username.
		 *
		 * **The caller should verify that the user does in fact possess this user ID.**
		 *
		 * @param telegramUserId The identifier of the user.
		 * **The caller should verify that the user does in fact posses this ID.**
		 * @param username If creating a new account, its [User.username].
		 * If the account already exists, this parameter is ignored.
		 * @param fullName If creating a new account, its [User.fullName].
		 * If the account already exists, this parameter is ignored.
		 */
		suspend fun logInWithTelegram(
			telegramUserId: Long,
			username: String,
			fullName: String,
		): Ref

	}

	interface Ref : BaseRef<User> {
		override val service: Service
	}
}
