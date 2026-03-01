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
import opensavvy.prepared.suite.*
import opensavvy.prepared.suite.random.nextInt
import opensavvy.prepared.suite.random.random
import opensavvy.pursuit.finance.Currency
import opensavvy.pursuit.finance.Transaction
import opensavvy.pursuit.finance.Transaction.Amount
import opensavvy.pursuit.users.User
import opensavvy.pursuit.users.executeAs
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Generates a new transaction specifically for this test.
 */
context(test: TestDsl)
internal suspend fun Transaction.Service.testTransaction(
	owner: User.Ref,
	at: Instant? = null,
	label: String? = null,
	from: Amount? = null,
	into: Amount,
): Transaction.Ref = executeAs(owner) {
	this.create(
		at = at ?: test.time.now,
		label = label ?: "Test Transaction ${test.random.nextInt()}",
		from = from,
		into = into,
	)
}

/**
 * Generates a new transaction specifically for this test.
 */
fun Prepared<Transaction.Service>.testTransaction(
	owner: Prepared<User.Ref>,
	at: Instant? = null,
	label: String? = null,
	from: Amount? = null,
	into: Amount,
): PreparedProvider<Transaction.Ref> = prepared {
	this@testTransaction().testTransaction(owner(), at, label, from, into)
}

/**
 * Generates a new transaction specifically for this test.
 */
fun Prepared<Transaction.Service>.testTransaction(
	owner: Prepared<User.Ref>,
	amount: Long,
	currency: Prepared<Currency.Ref>,
	at: Instant? = null,
	label: String? = null,
	from: Amount? = null,
): PreparedProvider<Transaction.Ref> = prepared {
	this@testTransaction().testTransaction(owner(), at, label, from, Amount(amount, currency()))
}
