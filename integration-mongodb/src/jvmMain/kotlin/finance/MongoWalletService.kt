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

import kotlinx.serialization.Serializable
import opensavvy.ktmongo.bson.types.ObjectId
import opensavvy.ktmongo.coroutines.MongoCollection
import opensavvy.pursuit.finance.Wallet

@Serializable
internal data class MongoWallet(
	val _id: ObjectId,
	val name: String,
	val currency: ObjectId,
)

internal class MongoWalletService(
	private val collection: MongoCollection<MongoWallet>,
	private val currencies: MongoCurrencyService,
) : Wallet.Service {

	inner class MongoWalletRef(
		val id: ObjectId,
	) : Wallet.Ref {
		override val service get() = this@MongoWalletService

		override suspend fun read(): Wallet? {
			val wallet = collection.findOne {
				MongoWallet::_id eq id
			} ?: return null

			return Wallet(
				name = wallet.name,
				currency = currencies.MongoCurrencyRef(wallet.currency),
			)
		}

		// region Identity

		override fun equals(other: Any?): Boolean = other is MongoWalletRef
			&& service === other.service
			&& id == other.id

		override fun hashCode(): Int {
			var result = id.hashCode()
			result = 31 * result + service.hashCode()
			return result
		}

		override fun toString() = "mongo.Wallet($id)"

		// endregion
	}
}
