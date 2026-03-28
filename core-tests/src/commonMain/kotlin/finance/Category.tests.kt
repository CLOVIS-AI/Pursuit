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
import opensavvy.pursuit.finance.Category
import opensavvy.pursuit.finance.Category.CategoryTotal
import opensavvy.pursuit.finance.Currency
import opensavvy.pursuit.finance.Transaction
import opensavvy.pursuit.finance.Transaction.Amount
import opensavvy.pursuit.tests.users.executeAs
import opensavvy.pursuit.tests.users.testUser
import opensavvy.pursuit.users.User

fun SuiteDsl.verifyCategoryService(
	users: Prepared<User.Service>,
	currencies: Prepared<Currency.Service>,
	transactions: Prepared<Transaction.Service>,
	categories: Prepared<Category.Service>,
) = suite("Categories") {

	val userA by users.testUser(name = "Alice")
	val userB by users.testUser(name = "Bob")

	val currencyA1 by currencies.testCurrency(owner = userA)
	val currencyA2 by currencies.testCurrency(owner = userA)
	val currencyB1 by currencies.testCurrency(owner = userB)

	suite("Unauthenticated access") {
		test("Cannot create a category without being authenticated") {
			checkThrows<IllegalStateException> {
				categories().create(
					label = "Test",
					description = "Test category",
				)
			}
		}

		test("Cannot list categories without being authenticated") {
			checkThrows<IllegalStateException> {
				categories().search().toList()
			}
		}

		test("Cannot compute totals without being authenticated") {
			checkThrows<IllegalStateException> {
				categories().totals().toList()
			}
		}
	}

	suite("Create") {
		val parentA by categories.testCategory(userA)

		test("Create a category") {
			executeAs(userA) {
				val category = categories().create(
					label = "Test",
					description = null,
					parent = null,
				)

				check(category.read()?.label == "Test")
				check(category.read()?.description == null)
				check(category.read()?.parent == null)

				check(category in categories().search().toList())
				check(category !in categories().search(parent = parentA()).toList())
			}
		}

		test("Create a category with a parent") {
			executeAs(userA) {
				val category = categories().create(
					label = "Test",
					description = "foo",
					parent = parentA(),
				)

				check(category.read()?.label == "Test")
				check(category.read()?.description == "foo")
				check(category.read()?.parent == parentA())

				check(category in categories().search().toList())
				check(category in categories().search(parent = parentA()).toList())
			}
		}

		test("A user cannot access a category they did not create") {
			val category = executeAs(userA) {
				categories().create(
					label = "Test",
					description = "foo",
					parent = parentA(),
				)
			}

			executeAs(userB) {
				check(category.read() == null)

				check(category !in categories().search().toList())

				check(categories().totals().isEmpty())

				checkThrows<IllegalStateException> {
					category.total()
				}
			}
		}
	}

	suite("Edit") {
		test("A user can rename their categories") {
			executeAs(userA) {
				val category = categories().create(
					label = "Test",
					description = null,
					parent = null,
				)

				category.edit(label = "Foo")

				check(category.read()?.label == "Foo")
			}
		}

		test("A user can change the description of their categories") {
			executeAs(userA) {
				val category = categories().create(
					label = "Test",
					description = null,
					parent = null,
				)

				category.edit(description = "Foo")

				check(category.read()?.description == "Foo")
			}
		}

		val parentA1 by categories.testCategory(userA)
		val parentA2 by categories.testCategory(userA, parent = parentA1)
		val parentB1 by categories.testCategory(userB)

		test("A user can reorganize their category hierarchy") {
			executeAs(userA) {
				val category = categories().create(
					label = "Test",
					description = null,
					parent = null,
				)

				category.edit(parent = parentA1())

				check(category.read()?.parent == parentA1())
				check(parentA1().children() == setOf(category))
			}
		}

		test("A user can reorganize their category hierarchy with multiple levels") {
			executeAs(userA) {
				val category = categories().create(
					label = "Test",
					description = null,
					parent = null,
				)

				category.edit(parent = parentA2())

				check(category.read()?.parent == parentA2())
				check(parentA2().read()?.parent == parentA1())

				check(parentA1().children(includeIndirect = true) == setOf(parentA2(), category))
				check(parentA1().children(includeIndirect = false) == setOf(parentA2()))
				check(parentA2().children() == setOf(category))
			}
		}

		test("A user cannot reorganize their category hierarchy under a category from another user") {
			executeAs(userA) {
				val category = categories().create(
					label = "Test",
					description = null,
					parent = null,
				)

				checkThrows<IllegalStateException> {
					category.edit(parent = parentB1())
				}
			}
		}

		test("A user can delete their own categories") {
			executeAs(userA) {
				val category = categories().create(
					label = "Test",
					description = null,
					parent = null,
				)

				category.delete()

				check(category.read() == null)
			}
		}
	}

	suite("Totals") {
		val categoryA1 by categories.testCategory(userA)
		val categoryA2 by categories.testCategory(userA, parent = categoryA1)
		val categoryA3 by categories.testCategory(userA, parent = categoryA1)
		val categoryB1 by categories.testCategory(userB)

		val tA1 by transactions.testTransaction(userA, 1, currencyA1, category = categoryA1)
		val tA2 by transactions.testTransaction(userA, 2, currencyA1, category = categoryA2)
		val tA3a by transactions.testTransaction(userA, 4, currencyA1, category = categoryA3)
		val tA3b by transactions.testTransaction(userA, -8, currencyA1, category = categoryA3)
		val tA3c by transactions.testTransaction(userA, 16, currencyA2, category = categoryA3)

		test("Top-level category") {
			executeAs(userA) {
				tA1()

				check(categoryA1().total(currencyA1()) == Amount(1, currencyA1()))
				check(categoryA1().total() == CategoryTotal(categoryA1(), Amount(1, currencyA1())))
				check(categories().totals() == listOf(CategoryTotal(categoryA1(), Amount(1, currencyA1()))))

				check(categoryA2().total(currencyA1()) == Amount(0, currencyA1()))
				check(categoryA2().total() == CategoryTotal(categoryA2()))
			}

			executeAs(userB) {
				check(categoryB1().total() == CategoryTotal(categoryB1()))
			}
		}

		test("A category is the sum of its child") {
			executeAs(userA) {
				tA2()

				check(categoryA2().total(currencyA1()) == Amount(2, currencyA1()))
				check(categoryA2().total() == CategoryTotal(categoryA2(), Amount(2, currencyA1())))

				check(categoryA1().total(currencyA1()) == Amount(2, currencyA1()))
				check(categoryA1().total() == CategoryTotal(categoryA1(), Amount(2, currencyA1())))
				check(categories().totals() == listOf(CategoryTotal(categoryA1(), Amount(2, currencyA1())), CategoryTotal(categoryA2(), Amount(2, currencyA1()))))
			}

			executeAs(userB) {
				check(categoryB1().total() == CategoryTotal(categoryB1()))
			}
		}

		test("A category is the sum of its children") {
			executeAs(userA) {
				tA2()
				tA3a()

				check(categoryA2().total(currencyA1()) == Amount(2, currencyA1()))
				check(categoryA2().total() == CategoryTotal(categoryA2(), Amount(2, currencyA1())))

				check(categoryA3().total(currencyA1()) == Amount(4, currencyA1()))
				check(categoryA3().total() == CategoryTotal(categoryA3(), Amount(4, currencyA1())))

				check(categoryA1().total(currencyA1()) == Amount(6, currencyA1()))
				check(categoryA1().total() == CategoryTotal(categoryA1(), Amount(6, currencyA1())))
				check(categories().totals() == listOf(CategoryTotal(categoryA1(), Amount(6, currencyA1())), CategoryTotal(categoryA2(), Amount(2, currencyA1())), CategoryTotal(categoryA3(), Amount(4, currencyA1()))))
			}

			executeAs(userB) {
				check(categoryB1().total() == CategoryTotal(categoryB1()))
			}
		}

		test("A category is the sum of its children, including negative values") {
			executeAs(userA) {
				tA2()
				tA3a()
				tA3b()

				check(categoryA2().total(currencyA1()) == Amount(2, currencyA1()))
				check(categoryA2().total() == CategoryTotal(categoryA2(), Amount(2, currencyA1())))

				check(categoryA3().total(currencyA1()) == Amount(-4, currencyA1()))
				check(categoryA3().total() == CategoryTotal(categoryA3(), Amount(-4, currencyA1())))

				check(categoryA1().total(currencyA1()) == Amount(-2, currencyA1()))
				check(categoryA1().total() == CategoryTotal(categoryA1(), Amount(-2, currencyA1())))
				check(categories().totals() == listOf(CategoryTotal(categoryA1(), Amount(-2, currencyA1())), CategoryTotal(categoryA2(), Amount(2, currencyA1())), CategoryTotal(categoryA3(), Amount(-4, currencyA1()))))
			}

			executeAs(userB) {
				check(categoryB1().total() == CategoryTotal(categoryB1()))
			}
		}

		test("A category is the sum of its children, including negative values") {
			executeAs(userA) {
				tA2()
				tA3a()
				tA3b()
				tA3c()

				check(categoryA2().total(currencyA1()) == Amount(2, currencyA1()))
				check(categoryA2().total() == CategoryTotal(categoryA2(), Amount(2, currencyA1())))

				check(categoryA3().total(currencyA1()) == Amount(-4, currencyA1()))
				check(categoryA3().total(currencyA2()) == Amount(16, currencyA2()))
				check(categoryA3().total() == CategoryTotal(categoryA3(), Amount(-4, currencyA1()), Amount(16, currencyA2())))

				check(categoryA1().total(currencyA1()) == Amount(-2, currencyA1()))
				check(categoryA1().total(currencyA2()) == Amount(16, currencyA2()))
				check(categoryA1().total() == CategoryTotal(categoryA1(), Amount(-2, currencyA1()), Amount(16, currencyA2())))
				check(categories().totals() == listOf(CategoryTotal(categoryA1(), Amount(-2, currencyA1()), Amount(16, currencyA2())), CategoryTotal(categoryA2(), Amount(2, currencyA1())), CategoryTotal(categoryA3(), Amount(-4, currencyA1()), Amount(16, currencyA2()))))
			}

			executeAs(userB) {
				check(categoryB1().total() == CategoryTotal(categoryB1()))
			}
		}
	}
}
