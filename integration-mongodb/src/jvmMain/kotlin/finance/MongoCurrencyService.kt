/*
 * Copyright (c) 2025, OpenSavvy and contributors.
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

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import opensavvy.ktmongo.coroutines.MongoCollection
import opensavvy.pursuit.finance.Currency
import org.bson.types.ObjectId

@Serializable
internal data class MongoCurrency(
	val _id: @Contextual ObjectId,
	val name: String,
	val symbol: String,
)

internal class MongoCurrencyService(
	private val collection: MongoCollection<MongoCurrency>,
) : Currency.Service {

	inner class MongoCurrencyRef(
		val id: ObjectId,
	) : Currency.Ref {
		override val service get() = this@MongoCurrencyService

		override suspend fun read(): Currency? {
			val currency = collection.findOne {
				MongoCurrency::_id eq id
			} ?: return null

			return Currency(
				name = currency.name,
				symbol = currency.symbol,
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
