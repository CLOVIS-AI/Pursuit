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

import opensavvy.prepared.suite.Prepared
import opensavvy.prepared.suite.PreparedProvider
import opensavvy.prepared.suite.TestDsl
import opensavvy.prepared.suite.prepared
import opensavvy.prepared.suite.random.random
import opensavvy.pursuit.finance.Currency
import opensavvy.pursuit.users.User
import opensavvy.pursuit.users.executeAs

private val templateCurrencies = listOf(
	"EURO" to "€",
	"US DOLLAR" to "$",
	"YEN" to "¥",
	"POUND" to "£",
	"RUBLE" to "₽",
	"YUAN" to "¥",
	"FRANC" to "₣",
	"RUPEE" to "₹",
	"WON" to "₩",
	"REAL" to "R$",
	"RAND" to "R",
	"KRONA" to "kr",
	"PESO" to "₱",
	"LIRA" to "₺",
	"DONG" to "₫",
	"ZLOTY" to "zł",
	"SHEKEL" to "₪",
)

/**
 * Generates a new currency specifically for this test.
 */
context(_: TestDsl)
internal suspend fun Currency.Service.testCurrency(
	owner: User.Ref,
	name: String? = null,
	symbol: String? = null,
	description: String? = null,
	numberToBasic: Int? = null,
): Currency.Ref = executeAs(owner) {
	val (templateName, templateSymbol) = templateCurrencies.random(
		contextOf<TestDsl>().random.accessUnsafe()
	)

	this.create(
		name = name ?: templateName,
		symbol = symbol ?: templateSymbol,
		description = description,
		numberToBasic = numberToBasic ?: 100,
	)
}

/**
 * Generates a new currency specifically for this test.
 */
fun Prepared<Currency.Service>.testCurrency(
	owner: Prepared<User.Ref>,
	name: String? = null,
	symbol: String? = null,
	description: String? = null,
): PreparedProvider<Currency.Ref> = prepared {
	this@testCurrency().testCurrency(owner(), name, symbol, description)
}
