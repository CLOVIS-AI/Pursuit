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

@file:OptIn(ExperimentalTime::class, ExperimentalCoroutinesApi::class)

package opensavvy.pursuit.tests.finance

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.flow.toList
import opensavvy.prepared.suite.*
import opensavvy.prepared.suite.assertions.checkThrows
import opensavvy.prepared.suite.random.nextLong
import opensavvy.prepared.suite.random.random
import opensavvy.pursuit.finance.Category
import opensavvy.pursuit.finance.Currency
import opensavvy.pursuit.finance.Transaction
import opensavvy.pursuit.finance.Transaction.Amount
import opensavvy.pursuit.tests.users.executeAs
import opensavvy.pursuit.tests.users.testUser
import opensavvy.pursuit.users.User
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

fun SuiteDsl.verifyTransactionService(
	users: Prepared<User.Service>,
	currencies: Prepared<Currency.Service>,
	transactions: Prepared<Transaction.Service>,
	categories: Prepared<Category.Service>,
) = suite("Transactions") {

	val userA by users.testUser(name = "Alice")
	val userB by users.testUser(name = "Bob")

	val currencyA1 by currencies.testCurrency(owner = userA)
	val currencyA2 by currencies.testCurrency(owner = userA)
	val currencyB1 by currencies.testCurrency(owner = userB)

	suite("Unauthenticated access") {
		test("Cannot create a transaction without being authenticated") {
			checkThrows<IllegalStateException> {
				transactions().create(
					at = time.now,
					label = "Test",
					from = null,
					into = Amount(20, currencyA1()),
					category = null,
				)
			}
		}

		test("Cannot list transactions without being authenticated") {
			checkThrows<IllegalStateException> {
				transactions().search().toList()
			}
		}

		test("Cannot compute totals without being authenticated") {
			checkThrows<IllegalStateException> {
				transactions().totals()
			}

			checkThrows<IllegalStateException> {
				transactions().total(currencyA1())
			}
		}
	}

	suite("Create") {
		test("Create a transaction") {
			executeAs(userA) {
				val transaction = transactions().create(
					at = time.now,
					label = "Test",
					from = null,
					into = Amount(20, currencyA1()),
					category = null,
				)

				check(transaction.read()?.at == time.now)
				check(transaction.read()?.label == "Test")
				check(transaction.read()?.from == null)
				check(transaction.read()?.into == Amount(20, currencyA1()))
				check(transaction.read()?.currency == currencyA1())

				check(transaction in transactions().search().toList())
			}
		}

		test("A user cannot access a transaction they did not create") {
			val transaction = executeAs(userA) {
				transactions().create(
					at = time.now,
					label = "Test",
					from = null,
					into = Amount(20, currencyA1())
				)
			}

			executeAs(userB) {
				check(transaction.read() == null)

				check(transaction !in transactions().search().toList())

				check(transactions().totals().isEmpty())

				checkThrows<IllegalStateException> {
					transaction.edit(label = "Foo")
				}
			}
		}

		test("A user can't create a transaction with a currency they do not have access to") {
			executeAs(userA) {
				checkThrows<IllegalStateException> {
					transactions().create(
						at = time.now,
						label = "Test",
						from = null,
						into = Amount(20, currencyB1())
					)
				}

				checkThrows<IllegalStateException> {
					transactions().create(
						at = time.now,
						label = "Test",
						from = Amount(5, currencyB1()),
						into = Amount(20, currencyA1())
					)
				}
			}
		}

		test("A user can create a multi-amount transaction") {
			executeAs(userA) {
				val ref = transactions().create(
					at = time.now,
					label = "Test",
					from = Amount(10, currencyA1()),
					into = Amount(20, currencyA2()),
				)

				val transaction = checkNotNull(ref.read())
				check(transaction.from == Amount(10, currencyA1()))
				check(transaction.into == Amount(20, currencyA2()))
			}
		}

		test("A user can create a multi-amount transaction with a single currency") {
			executeAs(userA) {
				val ref = transactions().create(
					at = time.now,
					label = "Test",
					from = Amount(10, currencyA1()),
					into = Amount(20, currencyA1()),
				)

				val transaction = checkNotNull(ref.read())
				check(transaction.from == Amount(10, currencyA1()))
				check(transaction.into == Amount(20, currencyA1()))
			}
		}
	}

	suite("Edit") {
		test("A user can re-label their transactions") {
			executeAs(userA) {
				val transaction = transactions().create(
					at = time.now,
					label = "Test",
					from = Amount(10, currencyA1()),
					into = Amount(20, currencyA1()),
				)

				transaction.edit(label = "Foo")

				check(transaction.read()?.label == "Foo")
			}
		}

		test("A user can re-date their transactions") {
			executeAs(userA) {
				val transaction = transactions().create(
					at = time.now,
					label = "Test",
					from = Amount(10, currencyA1()),
					into = Amount(20, currencyA1()),
				)

				val at2 = time.now + 5.minutes

				transaction.edit(at = at2)

				check(transaction.read()?.at == at2)
			}
		}

		test("A user can change the origin of a transaction") {
			executeAs(userA) {
				val transaction = transactions().create(
					at = time.now,
					label = "Test",
					from = Amount(10, currencyA1()),
					into = Amount(20, currencyA1()),
				)

				transaction.edit(from = Amount(15, currencyA2()))

				check(transaction.read()?.from == Amount(15, currencyA2()))
			}
		}

		test("A user can change the destination of a transaction") {
			executeAs(userA) {
				val transaction = transactions().create(
					at = time.now,
					label = "Test",
					from = Amount(10, currencyA2()),
					into = Amount(20, currencyA1()),
				)

				transaction.edit(into = Amount(15, currencyA2()))

				check(transaction.read()?.into == Amount(15, currencyA2()))
			}
		}

		val categoryA1 by categories.testCategory(userA)
		val categoryA2 by categories.testCategory(userA)

		test("A user can change the category of a transaction as often as they want") {
			executeAs(userA) {
				val transaction = transactions().create(
					at = time.now,
					label = "Test",
					from = Amount(10, currencyA2()),
					into = Amount(20, currencyA1()),
					category = categoryA1(),
				)

				check(transaction.read()?.category == categoryA1())

				transaction.categorize(categoryA2())

				check(transaction.read()?.category == categoryA2())

				transaction.categorize(categoryA1())

				check(transaction.read()?.category == categoryA1())
			}
		}

		test("A user can remove the category of a transaction") {
			executeAs(userA) {
				val transaction = transactions().create(
					at = time.now,
					label = "Test",
					from = Amount(10, currencyA2()),
					into = Amount(20, currencyA1()),
					category = categoryA1(),
				)

				check(transaction.read()?.category == categoryA1())

				transaction.decategorize()

				check(transaction.read()?.category == null)
			}
		}

		test("A user can delete a transaction") {
			executeAs(userA) {
				val transaction = transactions().create(
					at = time.now,
					label = "Test",
					from = Amount(10, currencyA2()),
					into = Amount(20, currencyA1()),
				)

				transaction.delete()

				check(transaction.read() == null)
				check(transactions().search().toList().isEmpty())
			}
		}
	}

	suite("Search") {
		val t1 by prepared {
			executeAs(userA) {
				transactions().create(
					at = time.now,
					label = "Test Foo",
					from = Amount(10, currencyA2()),
					into = Amount(20, currencyA1()),
				)
			}
		}

		val t2 by prepared {
			executeAs(userA) {
				transactions().create(
					at = time.now,
					label = "Foo",
					from = Amount(10, currencyA2()),
					into = Amount(20, currencyA1()),
				)
			}
		}

		val t3 by prepared {
			executeAs(userA) {
				transactions().create(
					at = time.now,
					label = "Test",
					from = Amount(10, currencyA2()),
					into = Amount(20, currencyA1()),
				)
			}
		}

		test("By default, transactions are returned starting with the most recent") {
			t1()
			delay(1.seconds)
			t2()
			delay(1.seconds)
			t3()

			executeAs(userA) {
				check(transactions().search().toList() == listOf(t3(), t2(), t1()))
			}
		}

		test("Transactions can be returned starting with the oldest") {
			t1()
			delay(1.seconds)
			t2()
			delay(1.seconds)
			t3()

			executeAs(userA) {
				check(transactions().search(mostRecentFirst = false).toList() == listOf(t1(), t2(), t3()))
			}
		}

		test("Transactions can be searched by label") {
			t1()
			delay(1.seconds)
			t2()
			delay(1.seconds)
			t3()

			executeAs(userA) {
				check(transactions().search(label = "Foo").toList() == listOf(t2(), t1()))
				check(transactions().search(label = "Test").toList() == listOf(t3(), t1()))
			}
		}

		test("Transactions can be searched by date") {
			time.set("2025-12-07T17:22:00Z")
			t1()

			time.set("2025-12-09T02:10:00Z")
			t2()

			time.set("2026-02-01T12:05:00Z")
			t3()

			executeAs(userA) {
				check(transactions().search(start = Instant.parse("2025-12-09T02:10:00Z")).toList() == listOf(t3(), t2()))
				check(transactions().search(end = Instant.parse("2025-12-09T02:10:00Z")).toList() == listOf(t2(), t1()))
				check(transactions().search(start = Instant.parse("2025-12-09T02:08:00Z"), end = Instant.parse("2025-12-09T02:10:00Z")).toList() == listOf(t2()))
				check(transactions().search(start = Instant.parse("2026-01-01T00:00:00Z")).toList() == listOf(t3()))
			}
		}
	}

	suite("Total") {
		test("Single currency") {
			executeAs(userB) {
				// This transaction should not impact the total
				transactions().create(
					at = time.now,
					label = "Test",
					from = null,
					into = Amount(10, currencyB1()),
				)
			}

			executeAs(userA) {
				repeat(100) {
					val _ = transactions().create(
						at = time.now,
						label = "Test",
						from = null,
						into = Amount(10, currencyA1()),
					)
				}

				check(transactions().search().count() == 100)
				check(transactions().total(currencyA1()) == Amount(1000, currencyA1()))
				check(transactions().total(currencyA2()) == Amount(0, currencyA2()))
			}
		}

		test("Multiple currencies at once") {
			executeAs(userB) {
				// This transaction should not impact the total
				transactions().create(
					at = time.now,
					label = "Test",
					from = null,
					into = Amount(10, currencyB1()),
				)
			}

			executeAs(userA) {
				repeat(10) {
					val _ = transactions().create(
						at = Instant.fromEpochSeconds(random.nextLong()),
						label = "Test",
						from = null,
						into = Amount(10, currencyA1()),
					)
				}

				repeat(10) {
					val _ = transactions().create(
						at = Instant.fromEpochSeconds(random.nextLong()),
						label = "Test",
						from = null,
						into = Amount(10, currencyA2()),
					)
				}

				check(transactions().search().count() == 20)
				check(transactions().total(currencyA1()) == Amount(100, currencyA1()))
				check(transactions().total(currencyA2()) == Amount(100, currencyA2()))

				val totals = transactions().totals()
				check(totals.size == 2)
				check(Amount(100, currencyA1()) in totals)
				check(Amount(100, currencyA2()) in totals)
			}
		}

		test("Conversions between currencies") {
			executeAs(userA) {
				val _ = transactions().create(
					at = time.now,
					label = "Salary",
					from = null,
					into = Amount(1000, currencyA1()),
				)

				val _ = transactions().create(
					at = time.now,
					label = "Money exchange",
					from = Amount(50, currencyA1()),
					into = Amount(350, currencyA2()),
				)

				val _ = transactions().create(
					at = time.now,
					label = "Spent other currency",
					from = null,
					into = Amount(-10, currencyA2()),
				)

				check(transactions().search().count() == 3)
				check(transactions().total(currencyA1()) == Amount(950, currencyA1()))
				check(transactions().total(currencyA2()) == Amount(340, currencyA2()))

				val totals = transactions().totals()
				check(totals.size == 2)
				check(Amount(950, currencyA1()) in totals)
				check(Amount(340, currencyA2()) in totals)
			}
		}
	}
}
