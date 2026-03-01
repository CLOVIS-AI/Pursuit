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

package opensavvy.pursuit.integration.mongodb.finance

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import opensavvy.ktmongo.bson.types.ObjectId
import opensavvy.ktmongo.coroutines.MongoCollection
import opensavvy.pursuit.finance.Currency
import opensavvy.pursuit.integration.mongodb.users.currentMongoUser

@Serializable
internal data class MongoCurrency(
	val _id: ObjectId,
	val name: String,
	val symbol: String,
	val description: String?,
	/** The user who created this currency. `null` if it is public/was created by the system. */
	val owner: ObjectId?,
	/** numberToBasic */
	val nToB: Int,
)

internal class MongoCurrencyService(
	private val collection: MongoCollection<MongoCurrency>,
) : Currency.Service {

	override suspend fun create(
		name: String,
		symbol: String,
		numberToBasic: Int,
		description: String?,
	): Currency.Ref {
		val user = currentMongoUser()

		val newId = collection.context.newId()

		collection.insertOne(
			MongoCurrency(
				_id = newId,
				name = name,
				symbol = symbol,
				description = description,
				owner = user.id,
				nToB = numberToBasic,
			)
		)

		return MongoCurrencyRef(newId, canEdit = true)
	}

	override suspend fun ensurePublic(
		name: String,
		symbol: String,
		numberToBasic: Int,
		description: String?,
	): Currency.Ref {
		collection.upsertOne(
			filter = {
				MongoCurrency::name eq name
				MongoCurrency::owner eq null
			},
			update = {
				MongoCurrency::symbol set symbol
				MongoCurrency::nToB set numberToBasic
				MongoCurrency::description set description
			}
		)

		// TODO after https://gitlab.com/opensavvy/ktmongo/-/merge_requests/197:
		//    merge the 'find' and the 'updateOne' into a single atomic operation
		val created = collection.findOne {
			MongoCurrency::name eq name
			MongoCurrency::owner eq null
		} ?: error("Could not find the public currency we just created: '$name' ($symbol)")

		return MongoCurrencyRef(created._id, canEdit = false)
	}

	override fun search(text: String?): Flow<Currency.Ref> = flow {
		val user = currentMongoUser()

		val search = collection.find {
			// TODO in the future: add a projection on just the ID, or add a cache

			or {
				MongoCurrency::owner eq user.id
				MongoCurrency::owner eq null
			}

			if (text != null) {
				val regex = ".*$text.*"
				MongoCurrency::name.regex(regex)
				MongoCurrency::symbol.regex(regex)
				MongoCurrency::description.regex(regex)
			}
		}

		emitAll(search.asFlow().map { MongoCurrencyRef(it._id, canEdit = it.owner == null || it.owner == user.id) })
	}

	inner class MongoCurrencyRef(
		val id: ObjectId,
		override val canEdit: Boolean,
	) : Currency.Ref {
		override val service get() = this@MongoCurrencyService

		override suspend fun edit(
			name: String?,
			symbol: String?,
			numberToBasic: Int?,
			description: String?,
		) {
			val user = currentMongoUser()

			// TODO after https://gitlab.com/opensavvy/ktmongo/-/merge_requests/197:
			//    merge the 'find' and the 'updateOne' into a single atomic operation
			val exists = collection.findOne {
				MongoCurrency::_id eq id
				MongoCurrency::owner eq user.id
			}
			checkNotNull(exists) { "Cannot modify a currency $id that does not exist, or that does not belong to the logged-in user" }

			collection.updateOne(
				filter = {
					MongoCurrency::_id eq id
					MongoCurrency::owner eq user.id
				},
				update = {
					if (name != null)
						MongoCurrency::name set name

					if (symbol != null)
						MongoCurrency::symbol set symbol

					if (description != null)
						MongoCurrency::description set description

					if (numberToBasic != null)
						MongoCurrency::nToB set numberToBasic
				}
			)
		}

		override suspend fun read(): Currency? {
			val user = currentMongoUser()

			val currency = collection.findOne {
				MongoCurrency::_id eq id
				or {
					MongoCurrency::owner eq user.id
					MongoCurrency::owner eq null
				}
			} ?: return null

			return Currency(
				name = currency.name,
				symbol = currency.symbol,
				description = currency.description,
				numberToBasic = currency.nToB,
			)
		}

		// region Identity

		override fun equals(other: Any?): Boolean = other is MongoCurrencyRef
			&& service === other.service
			&& id == other.id

		override fun hashCode(): Int {
			var result = id.hashCode()
			result = 31 * result + service.hashCode()
			return result
		}

		override fun toString() = "mongo.Currency($id)"

		// endregion
	}
}
