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
import kotlinx.serialization.Serializable
import opensavvy.ktmongo.bson.types.ObjectId
import opensavvy.ktmongo.coroutines.MongoCollection
import opensavvy.pursuit.finance.Category
import opensavvy.pursuit.finance.Currency
import opensavvy.pursuit.finance.Transaction
import kotlin.time.Instant

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
) : Transaction.Service {
	override suspend fun create(at: Instant, label: String, from: Transaction.Amount?, into: Transaction.Amount, category: Category.Ref?): Transaction.Ref {
		TODO("Will be implemented in https://gitlab.com/opensavvy/pursuit/-/work_items/14")
	}

	override fun search(label: String?, start: Instant?, end: Instant?, mostRecentFirst: Boolean): Flow<Transaction.Ref> {
		TODO("Will be implemented in https://gitlab.com/opensavvy/pursuit/-/work_items/14")
	}

	override suspend fun totals(start: Instant?, end: Instant?): List<Transaction.Amount> {
		TODO("Will be implemented in https://gitlab.com/opensavvy/pursuit/-/work_items/14")
	}

	override suspend fun total(currency: Currency.Ref, start: Instant?, end: Instant?): Transaction.Amount {
		TODO("Will be implemented in https://gitlab.com/opensavvy/pursuit/-/work_items/14")
	}

	inner class MongoTransactionRef(
		val id: ObjectId,
	) : Transaction.Ref {
		override suspend fun edit(at: Instant?, label: String?, from: Transaction.Amount?, into: Transaction.Amount?) {
			TODO("Will be implemented in https://gitlab.com/opensavvy/pursuit/-/work_items/14")
		}

		override suspend fun delete() {
			TODO("Will be implemented in https://gitlab.com/opensavvy/pursuit/-/work_items/14")
		}

		override suspend fun categorize(category: Category.Ref) {
			TODO("Will be implemented in https://gitlab.com/opensavvy/pursuit/-/work_items/14")
		}

		override suspend fun decategorize() {
			TODO("Will be implemented in https://gitlab.com/opensavvy/pursuit/-/work_items/14")
		}

		override val service get() = this@MongoTransactionService

		override suspend fun read(): Transaction? {
			val transaction = collection.findOne {
				MongoTransaction::_id eq id
			} ?: return null

			TODO("Will be implemented in https://gitlab.com/opensavvy/pursuit/-/work_items/14")
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
