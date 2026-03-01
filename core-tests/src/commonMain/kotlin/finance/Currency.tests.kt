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

import kotlinx.coroutines.flow.toList
import opensavvy.prepared.suite.Prepared
import opensavvy.prepared.suite.SuiteDsl
import opensavvy.prepared.suite.assertions.checkThrows
import opensavvy.pursuit.finance.Currency
import opensavvy.pursuit.tests.users.executeAs
import opensavvy.pursuit.tests.users.testUser
import opensavvy.pursuit.users.User

fun SuiteDsl.verifyCurrencyService(
	users: Prepared<User.Service>,
	currencies: Prepared<Currency.Service>,
) = suite("Currencies") {

	val userA by users.testUser(name = "Alice")
	val userB by users.testUser(name = "Bob")

	suite("Create") {
		test("Cannot create a currency without being authenticated") {
			checkThrows<IllegalStateException> {
				currencies().create(name = "EURO", symbol = "€", numberToBasic = 100)
			}
		}

		test("Cannot search for currencies without being authenticated") {
			checkThrows<IllegalStateException> {
				currencies().search().toList()
			}
		}

		test("A user can create a currency") {
			executeAs(userA) {
				val currency = checkNotNull(currencies().create(name = "EURO", symbol = "€", numberToBasic = 100).read())

				check(currency.name == "EURO")
				check(currency.symbol == "€")
				check(currency.description == null)
			}
		}

		test("A user doesn't have access to currencies created by another user") {
			var currency: Currency.Ref? = null
			executeAs(userA) {
				currency = currencies().create(name = "EURO", symbol = "€", numberToBasic = 100)
				check(currency.read() == Currency("EURO", "€", description = null, numberToBasic = 100))
			}
			checkNotNull(currency)

			executeAs(userB) {
				check(currencies().search().toList() == emptyList<Currency.Ref>())

				check(currency.read() == null)
				checkThrows<IllegalStateException> { currency.edit(name = "DOLLAR") }
			}
		}
	}

	suite("Edit") {
		test("A user can rename a currency") {
			executeAs(userA) {
				val currency = currencies().create(name = "EURO", symbol = "€", numberToBasic = 100)

				currency.edit(name = "DOLLAR")

				check(currency.read() == Currency("DOLLAR", "€", description = null, numberToBasic = 100))
			}
		}

		test("A user can change the symbol of a currency") {
			executeAs(userA) {
				val currency = currencies().create(name = "EURO", symbol = "€", numberToBasic = 100)

				currency.edit(symbol = "$")

				check(currency.read() == Currency("EURO", "$", description = null, numberToBasic = 100))
			}
		}

		test("A user can change the description of a currency") {
			executeAs(userA) {
				val currency = currencies().create(name = "EURO", symbol = "€", description = "Foo", numberToBasic = 100)

				currency.edit(description = "Bar")

				check(currency.read() == Currency("EURO", "€", description = "Bar", numberToBasic = 100))
			}
		}

		test("A user can change the number to basic of a currency") {
			executeAs(userA) {
				val currency = currencies().create(name = "EURO", symbol = "€", description = "Foo", numberToBasic = 100)

				currency.edit(numberToBasic = 5)

				check(currency.read() == Currency("EURO", "€", description = "Foo", numberToBasic = 5))
			}
		}
	}
}
