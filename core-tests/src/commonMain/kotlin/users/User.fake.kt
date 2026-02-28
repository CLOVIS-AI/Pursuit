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

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import opensavvy.pursuit.users.User

class FakeUserService : User.Service {

	private val usersById = mutableMapOf<Long, User>()
	private val userIdsByTelegramId = mutableMapOf<Long, Long>()
	private val lock = Mutex()

	override suspend fun logInWithTelegram(
		telegramUserId: Long,
		username: String,
		fullName: String,
	): User.Ref = lock.withLock("logInWithTelegram($telegramUserId)") {
		if (telegramUserId in userIdsByTelegramId) {
			// The user already exists
			val userId = userIdsByTelegramId[telegramUserId]!!
			return@withLock FakeUserRef(this, userId)
		} else {
			// Create the new user
			val userId = usersById.size.toLong()
			usersById[userId] = User(fullName = fullName, username = username)
			userIdsByTelegramId[telegramUserId] = userId
			return@withLock FakeUserRef(this, userId)
		}
	}

	private data class FakeUserRef(
		override val service: FakeUserService,
		val id: Long,
	) : User.Ref {

		override suspend fun read(): User? = service.lock.withLock("read($id)") {
			service.usersById[id]
		}

		override fun toString() = "FakeUserRef($id)"

		override fun equals(other: Any?): Boolean {
			if (this === other) return true
			if (other !is FakeUserRef) return false

			if (id != other.id) return false
			if (service != other.service) return false

			return true
		}

		override fun hashCode(): Int {
			var result = id.hashCode()
			result = 31 * result + service.hashCode()
			return result
		}

	}

	override fun toString() = "FakeUserService($usersById)"
}
