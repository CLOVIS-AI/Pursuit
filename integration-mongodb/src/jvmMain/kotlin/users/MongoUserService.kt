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

package opensavvy.pursuit.integration.mongodb.users

import kotlinx.serialization.Serializable
import opensavvy.ktmongo.bson.types.ObjectId
import opensavvy.ktmongo.coroutines.MongoCollection
import opensavvy.pursuit.users.User
import opensavvy.pursuit.users.currentUser

@Serializable
internal data class MongoUser(
	val _id: ObjectId,
	val username: String,
	val fullName: String,
	val tgId: Long? = null,
)

internal class MongoUserService(
	private val collection: MongoCollection<MongoUser>,
) : User.Service {

	override suspend fun logInWithTelegram(
		telegramUserId: Long,
		username: String,
		fullName: String,
	): User.Ref {
		// Currently, this doesn't return the value
		// See https://gitlab.com/opensavvy/ktmongo/-/issues/110
		collection.upsertOne(
			filter = {
				MongoUser::tgId eq telegramUserId
			},
			update = {
				MongoUser::username setOnInsert username
				MongoUser::fullName setOnInsert fullName
			}
		)

		val user = collection.findOne {
			MongoUser::tgId eq telegramUserId
		} ?: error("Cannot find the user for the Telegram account $telegramUserId, but we should have just created it")

		return MongoUserRef(user._id)
	}

	inner class MongoUserRef(
		val id: ObjectId,
	) : User.Ref {
		override val service get() = this@MongoUserService

		override suspend fun read(): User? {
			val user = collection.findOne {
				MongoUser::_id eq id
			} ?: return null

			return User(
				fullName = user.fullName,
				username = user.username,
			)
		}

		// region Identity

		override fun equals(other: Any?): Boolean = other is MongoUserRef
			&& service === other.service
			&& id == other.id

		override fun hashCode(): Int {
			var result = id.hashCode()
			result = 31 * result + service.hashCode()
			return result
		}

		override fun toString() = "mongo.User($id)"

		// endregion
	}
}

internal suspend fun currentMongoUser(): MongoUserService.MongoUserRef {
	val user = currentUser()
	check(user is MongoUserService.MongoUserRef) { "Cannot create a MongoDB currency with a non-MongoDB user: $user" }
	return user
}
