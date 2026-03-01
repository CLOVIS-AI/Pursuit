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
import opensavvy.pursuit.finance.Currency
import opensavvy.pursuit.users.User
import opensavvy.pursuit.users.currentUser

class FakeCurrencyService : Currency.Service {

	private val currenciesById = mutableMapOf<Long, Currency>()
	private val creatorsByCurrencyId = mutableMapOf<Long, User.Ref>()
	private val lock = Mutex()

	override suspend fun create(
		name: String,
		symbol: String,
		description: String?,
	): Currency.Ref = lock.withLock("create($name, $symbol, $description)") {
		val user = currentUser()

		val newId = currenciesById.size.toLong()
		val newCurrency = Currency(name, symbol, description)

		currenciesById[newId] = newCurrency
		creatorsByCurrencyId[newId] = user

		FakeCurrencyRef(this, newId)
	}

	override fun search(text: String?): Flow<Currency.Ref> = flow {
		val service = this@FakeCurrencyService
		val user = currentUser()

		// Query all elements BEFORE emitting in the flow to ensure there is no concurrency with other readers
		val results = lock.withLock("search($text)") {
			if (text == null)
				currenciesById.keys
					.filter { id -> creatorsByCurrencyId[id] == user }
					.map { id -> FakeCurrencyRef(service, id) }
			else {
				val pattern = Regex(".*$text.*", RegexOption.IGNORE_CASE)
				currenciesById
					.filter { (id, _) -> creatorsByCurrencyId[id] == user }
					.filter { (_, currency) -> pattern matches currency.name || pattern matches currency.symbol }
					.map { (id, _) -> FakeCurrencyRef(service, id) }
			}
		}

		emitAll(results.asFlow())
	}

	private data class FakeCurrencyRef(
		override val service: FakeCurrencyService,
		val id: Long,
	) : Currency.Ref {

		override suspend fun read(): Currency? = service.lock.withLock("read($id)") {
			val user = currentUser()

			if (service.creatorsByCurrencyId[id] != user)
				return@withLock null

			service.currenciesById[id]
		}

		override fun toString() = "FakeCurrencyRef($id)"

		override fun equals(other: Any?): Boolean {
			if (this === other) return true
			if (other !is FakeCurrencyRef) return false

			if (id != other.id) return false
			if (service != other.service) return false

			return true
		}

		override fun hashCode(): Int {
			var result = id.hashCode()
			result = 31 * result + service.hashCode()
			return result
		}

		override suspend fun edit(
			name: String?,
			symbol: String?,
			description: String?,
		) = service.lock.withLock("edit($id, $name, $symbol, $description)") {
			val user = currentUser()
			check(service.creatorsByCurrencyId[id] == user)

			val current = service.currenciesById[id] ?: error("Currency $id not found")

			val new = current.copy(
				name = name ?: current.name,
				symbol = symbol ?: current.symbol,
				description = description ?: current.description,
			)

			service.currenciesById[id] = new
		}
	}
}
