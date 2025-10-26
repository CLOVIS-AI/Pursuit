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
import opensavvy.pursuit.finance.Transaction

@Serializable
internal data class MongoTransaction(
	val _id: ObjectId,
	val fromWallet: ObjectId,
	val fromAmount: Long,
	val intoWallet: ObjectId,
	val intoAmount: Long,
)

internal class MongoTransactionService(
	private val collection: MongoCollection<MongoTransaction>,
	private val wallets: MongoWalletService,
) : Transaction.Service {

	inner class MongoTransactionRef(
		val id: ObjectId,
	) : Transaction.Ref {
		override val service get() = this@MongoTransactionService

		override suspend fun read(): Transaction? {
			val transaction = collection.findOne {
				MongoTransaction::_id eq id
			} ?: return null

			return Transaction(
				from = Transaction.TransactionEnd(
					amount = transaction.fromAmount,
					wallet = wallets.MongoWalletRef(transaction.fromWallet),
				),
				into = Transaction.TransactionEnd(
					amount = transaction.intoAmount,
					wallet = wallets.MongoWalletRef(transaction.intoWallet),
				)
			)
		}

		// region Identity

		override fun equals(other: Any?): Boolean = other is MongoTransactionRef
			&& service === other.service
			&& id == other.id

		override fun hashCode(): Int {
			var result = id.hashCode()
			result = 31 * result + service.hashCode()
			return result
		}

		override fun toString(): String = "mongo.Transaction($id)"

		// endregion
	}
}
