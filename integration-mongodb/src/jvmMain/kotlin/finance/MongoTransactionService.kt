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

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.serialization.Serializable
import opensavvy.ktmongo.bson.types.ObjectId
import opensavvy.ktmongo.coroutines.MongoCollection
import opensavvy.ktmongo.coroutines.firstOrNull
import opensavvy.pursuit.finance.Category
import opensavvy.pursuit.finance.Currency
import opensavvy.pursuit.finance.Transaction
import opensavvy.pursuit.integration.mongodb.finance.MongoCurrencyService.MongoCurrencyRef
import opensavvy.pursuit.integration.mongodb.users.currentMongoUser
import kotlin.time.Instant

@Serializable
internal data class MongoTransaction(
	val _id: ObjectId,
	val fromCurrency: ObjectId?,
	val fromAmount: Long?,
	val intoCurrency: ObjectId,
	val intoAmount: Long,
	val owner: ObjectId,
	val at: Instant,
	val label: String,
	val categories: List<ObjectId>,
)

@Serializable
internal data class MongoTransactionTotalResult(
	val total: Long,
)

internal class MongoTransactionService(
	private val collection: MongoCollection<MongoTransaction>,
	private val currencyService: MongoCurrencyService,
) : Transaction.Service {
	override suspend fun create(
		at: Instant,
		label: String,
		from: Transaction.Amount?,
		into: Transaction.Amount,
		category: Category.Ref?,
	): Transaction.Ref {
		val user = currentMongoUser()

		val newId = collection.newId()

		if (from != null)
			checkNotNull(from.currency.read()) { "Cannot find currency ${from.currency}" }
		checkNotNull(into.currency.read()) { "Cannot find currency ${into.currency}" }

		if (category != null)
			checkNotNull(category.read()) { "Cannot find category $category" }

		collection.insertOne(
			MongoTransaction(
				_id = newId,
				fromCurrency = (from?.currency as MongoCurrencyRef?)?.id,
				fromAmount = from?.amount,
				intoCurrency = (into.currency as MongoCurrencyRef).id,
				intoAmount = into.amount,
				owner = user.id,
				at = at,
				label = label,
				categories = emptyList(), // TODO in #17
			)
		)

		return MongoTransactionRef(newId)
	}

	override fun search(
		label: String?,
		start: Instant?,
		end: Instant?,
		mostRecentFirst: Boolean,
	): Flow<Transaction.Ref> = flow {
		val user = currentMongoUser()

		val search = collection.find({
			sort {
				if (mostRecentFirst)
					descending(MongoTransaction::at)
				else
					ascending(MongoTransaction::at)
			}
		}) {
			MongoTransaction::at gteNotNull start
			MongoTransaction::at lteNotNull end

			MongoTransaction::owner eq user.id

			if (label != null)
				MongoTransaction::label.regex(label)
		}

		emitAll(search.asFlow().map { MongoTransactionRef(it._id) })
	}

	@OptIn(ExperimentalCoroutinesApi::class)
	override suspend fun totals(
		start: Instant?,
		end: Instant?,
	): List<Transaction.Amount> {
		val user = currentMongoUser()

		// This method finds all currencies, then loops and computes the total for each currency.
		// This is quite slow. In the future, this should either use aggregations (requires complex grouping)
		// or precomputing.

		val currencies = collection.find {
			MongoTransaction::owner eq user.id
			MongoTransaction::at gteNotNull start
			MongoTransaction::at lteNotNull end
		}.asFlow()
			.flatMapConcat { listOfNotNull(it.fromCurrency, it.intoCurrency).asFlow() }
			.toSet()

		return coroutineScope {
			currencies.map {
				async { total(currencyService.refById(it), start, end) }
			}.awaitAll()
		}
	}

	override suspend fun total(
		currency: Currency.Ref,
		start: Instant?,
		end: Instant?,
	): Transaction.Amount {
		val user = currentMongoUser()

		val currencyId = (currency as MongoCurrencyRef).id

		val result = collection.aggregate()
			.match {
				MongoTransaction::owner eq user.id

				or {
					MongoTransaction::fromCurrency eq currencyId
					MongoTransaction::intoCurrency eq currencyId
				}

				MongoTransaction::at gteNotNull start
				MongoTransaction::at lteNotNull end
			}
			.group {
				val fromAmount = cond(
					condition = of(MongoTransaction::fromCurrency) eq of(currencyId),
					ifTrue = of(MongoTransaction::fromAmount),
					ifFalse = of(0),
				)

				val intoAmount = cond(
					condition = of(MongoTransaction::intoCurrency) eq of(currencyId),
					ifTrue = of(MongoTransaction::intoAmount),
					ifFalse = of(0),
				)

				MongoTransactionTotalResult::total sum (intoAmount - fromAmount)
			}
			.firstOrNull()
			?: MongoTransactionTotalResult(0)

		return Transaction.Amount(
			amount = result.total,
			currency = currency,
		)
	}

	inner class MongoTransactionRef(
		val id: ObjectId,
	) : Transaction.Ref {
		override suspend fun edit(
			at: Instant?,
			label: String?,
			from: Transaction.Amount?,
			into: Transaction.Amount?,
		) {
			val user = currentMongoUser()

			val result = collection.updateOne(
				filter = {
					MongoTransaction::_id eq id
					MongoTransaction::owner eq user.id
				},
				update = {
					if (at != null)
						MongoTransaction::at set at

					if (label != null)
						MongoTransaction::label set label

					if (from != null) {
						MongoTransaction::fromAmount set from.amount
						MongoTransaction::fromCurrency set (from.currency as MongoCurrencyRef).id
					}

					if (into != null) {
						MongoTransaction::intoAmount set into.amount
						MongoTransaction::intoCurrency set (into.currency as MongoCurrencyRef).id
					}
				}
			)

			check(result.modifiedCount == 1L) { "Transaction with ID $id not found or not owned by user" }
		}

		override suspend fun delete() {
			val user = currentMongoUser()

			collection.deleteOne {
				MongoTransaction::_id eq id
				MongoTransaction::owner eq user.id
			}
		}

		override suspend fun categorize(category: Category.Ref) {
			TODO("Will be implemented in https://gitlab.com/opensavvy/pursuit/-/work_items/14")
		}

		override suspend fun decategorize() {
			TODO("Will be implemented in https://gitlab.com/opensavvy/pursuit/-/work_items/14")
		}

		override val service get() = this@MongoTransactionService

		override suspend fun read(): Transaction? {
			val user = currentMongoUser()

			val transaction = collection.findOne {
				MongoTransaction::_id eq id
				MongoTransaction::owner eq user.id
			} ?: return null

			return Transaction(
				at = transaction.at,
				label = transaction.label,
				from = if (transaction.fromAmount != null && transaction.fromCurrency != null)
					Transaction.Amount(transaction.fromAmount, currencyService.refById(transaction.fromCurrency))
				else null,
				into = Transaction.Amount(transaction.intoAmount, currencyService.refById(transaction.intoCurrency)),
				category = null, // TODO in #17
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
