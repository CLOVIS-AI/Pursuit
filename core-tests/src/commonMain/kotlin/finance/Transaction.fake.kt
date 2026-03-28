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

package opensavvy.pursuit.tests.finance

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import opensavvy.prepared.suite.assertions.matches
import opensavvy.pursuit.finance.Category
import opensavvy.pursuit.finance.Currency
import opensavvy.pursuit.finance.Transaction
import opensavvy.pursuit.users.User
import opensavvy.pursuit.users.currentUser
import kotlin.time.Instant

class FakeTransactionService : Transaction.Service {

	private val transactionsById = mutableMapOf<Long, Transaction>()
	private val creatorsByTransactionId = mutableMapOf<Long, User.Ref>()
	private val lock = Mutex()

	override suspend fun create(
		at: Instant,
		label: String,
		from: Transaction.Amount?,
		into: Transaction.Amount,
		category: Category.Ref?,
	): Transaction.Ref = lock.withLock("create($at, $label, $from, $into)") {
		val user = currentUser()

		// Verify access rights
		if (from != null)
			checkNotNull(from.currency.read()) { "Could not access the 'from' currency: ${from.currency}" }
		checkNotNull(into.currency.read()) { "Could not access the 'into' currency: ${into.currency}" }

		val newId = (transactionsById.keys.maxOrNull() ?: 0) + 1
		val newTransaction = Transaction(at, label, from, into, category)

		transactionsById[newId] = newTransaction
		creatorsByTransactionId[newId] = user

		FakeTransactionRef(this, newId)
	}

	override fun search(
		label: String?,
		start: Instant?,
		end: Instant?,
		mostRecentFirst: Boolean,
	): Flow<Transaction.Ref> = flow {
		val user = currentUser()
		val service = this@FakeTransactionService

		// Query all elements BEFORE emitting in the flow to ensure there is no concurrency with other readers
		val results = lock.withLock("search($label, $start, $end, $mostRecentFirst)") {
			val unordered = transactionsById
				.asSequence()
				.filter { (id, _) -> creatorsByTransactionId[id] == user }
				.filter { (_, it) -> label == null || it.label matches ".*$label.*" }
				.filter { (_, it) -> start == null || it.at >= start }
				.filter { (_, it) -> end == null || it.at <= end }
				.map { (id, _) -> FakeTransactionRef(service, id) }
				.toList()

			if (mostRecentFirst)
				unordered.sortedByDescending { transactionsById[it.id]!!.at }
			else
				unordered.sortedBy { transactionsById[it.id]!!.at }
		}

		emitAll(results.asFlow())
	}

	override suspend fun totals(
		start: Instant?,
		end: Instant?,
	): List<Transaction.Amount> {
		val user = currentUser()

		// This implementation is naive and slow, but this is a test double, not real code, so it doesn't matter
		val currencies = lock.withLock("totals.computeCurrencies()") {
			transactionsById
				.filter { (id, _) -> creatorsByTransactionId[id] == user }
				.flatMapTo(HashSet()) { (_, it) ->
					listOfNotNull(it.from?.currency, it.into.currency)
				}
		}

		return currencies.map { currency ->
			total(currency, start, end)
		}
	}

	override suspend fun total(
		currency: Currency.Ref,
		start: Instant?,
		end: Instant?,
	): Transaction.Amount = lock.withLock("total($currency, $start, $end)") {
		val user = currentUser()

		val transactions = transactionsById
			.asSequence()
			.filter { (id, _) -> creatorsByTransactionId[id] == user }
			.map { (_, it) -> it }
			.filter { start == null || it.at >= start }
			.filter { end == null || it.at <= end }

		totalOf(currency, transactions)
	}

	private fun Transaction.delta(currency: Currency.Ref): Long {
		val fromAmount = if (from != null && from!!.currency == currency)
			-from!!.amount
		else 0

		val intoAmount = if (into.currency == currency)
			into.amount
		else 0

		return fromAmount + intoAmount
	}

	fun totalOf(
		currency: Currency.Ref,
		transactions: Sequence<Transaction>,
	): Transaction.Amount {
		println("Sum of:")
		return transactions
			.sumOf { it.delta(currency) }
			.let { Transaction.Amount(it, currency) }
			.also { println(" = $it") }
	}

	private data class FakeTransactionRef(
		override val service: FakeTransactionService,
		val id: Long,
	) : Transaction.Ref {

		override suspend fun read(): Transaction? = service.lock.withLock("read($id)") {
			val user = currentUser()

			if (service.creatorsByTransactionId[id] != user)
				return@withLock null

			service.transactionsById[id]
		}

		override suspend fun edit(
			at: Instant?,
			label: String?,
			from: Transaction.Amount?,
			into: Transaction.Amount?,
		) = service.lock.withLock("edit($id, $at, $label, $from, $into)") {
			val user = currentUser()

			check(service.creatorsByTransactionId[id] == user) { "You are not allowed to modify this transaction" }
			val existing = service.transactionsById[id]!!

			service.transactionsById[id] = existing.copy(
				at = at ?: existing.at,
				label = label ?: existing.label,
				from = from ?: existing.from,
				into = into ?: existing.into,
			)
		}

		override suspend fun categorize(category: Category.Ref) = service.lock.withLock("categorize($category)") {
			val user = currentUser()

			check(service.creatorsByTransactionId[id] == user) { "You are not allowed to modify this transaction" }
			val existing = service.transactionsById[id]!!

			service.transactionsById[id] = existing.copy(
				category = category,
			)
		}

		override suspend fun decategorize() = service.lock.withLock("decategorize()") {
			val user = currentUser()

			check(service.creatorsByTransactionId[id] == user) { "You are not allowed to modify this transaction" }
			val existing = service.transactionsById[id]!!

			service.transactionsById[id] = existing.copy(
				category = null,
			)
		}

		override suspend fun delete(): Unit = service.lock.withLock("delete($id)") {
			val user = currentUser()

			check(service.creatorsByTransactionId[id] == user) { "You are not allowed to modify this transaction" }
			val existing = service.transactionsById[id]!!

			service.transactionsById.remove(id)
		}

		override fun toString(): String = "FakeTransactionRef(id=$id)"
	}
}
