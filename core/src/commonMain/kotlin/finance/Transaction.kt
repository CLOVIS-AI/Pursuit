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

package opensavvy.pursuit.finance

import kotlinx.coroutines.flow.Flow
import opensavvy.pursuit.base.BaseEntity
import opensavvy.pursuit.base.BaseRef
import opensavvy.pursuit.base.BaseService
import kotlin.time.Instant

/**
 * A transaction is a conversion of an [Amount] into another [Amount].
 *
 * ## Single-amount transactions
 *
 * The simplest kind of transaction only has an [into] amount.
 * In this case, it represents a change in amount for the current user.
 *
 * For example, a transaction with [label] "Carefour" and the [into] amount "-52.67€" represents
 * that the user has spent 52.67€.
 *
 * A transaction with [label] "Salary" and the [into] amount "1000€" represents
 * that the user has received 1000€.
 *
 * ## Multi-amount transactions
 *
 * A multi-amount transaction has two amounts: [from] and [into].
 * It represents a transfer of funds.
 *
 * Typically, this is useful when both amounts have a different [Amount.currency],
 * to represent conversions from one currency to another.
 *
 * Note that in that case, [from] is interpreted a negative amount.
 * For example, a [from] of "52¥" and an [into] of "0.5€" means that the user
 * has **spent** 52 yens and **received** 50 euro-cents.
 */
data class Transaction(
	/**
	 * The instant at which the transaction happened.
	 */
	val at: Instant,
	/**
	 * Human-readable label for this transaction.
	 */
	val label: String,
	/**
	 * An amount that was **lost** during this transaction, by the current user.
	 */
	val from: Amount?,
	/**
	 * An amount that was **received** during this transaction, by the current user.
	 */
	val into: Amount,
	/**
	 * The category that this transaction was [categorized][Ref.categorize] as.
	 *
	 * If `null`, the user has not yet categorized this transaction.
	 *
	 * Note that categories are personal. Each user sees different categories.
	 */
	val category: Category.Ref?,
) : BaseEntity {

	/**
	 * The currency used for both [from] and [into] amounts.
	 *
	 * If [from] and [into] use different currencies, this is `null`.
	 *
	 * See [Transaction] to learn more about multi-currency transactions.
	 */
	val currency: Currency.Ref?
		get() = when {
			from == null -> into.currency
			from.currency == into.currency -> into.currency
			else -> null
		}

	data class Amount(
		/**
		 * How much money is involved in this transaction.
		 *
		 * Note that this field respects [Currency.numberToBasic].
		 * For example, 52.67€ is represented as `5267`, because the EURO unit has 2 decimals.
		 */
		val amount: Long,
		/**
		 * The currency this [amount] is a value of.
		 */
		val currency: Currency.Ref,
	)

	interface Service : BaseService<Transaction> {

		/**
		 * Creates a new [Transaction].
		 *
		 * The parameters correspond to the fields of the [Transaction] class.
		 *
		 * ### Authorization
		 *
		 * The user who creates the transaction is the only one who has access to it.
		 */
		suspend fun create(
			at: Instant,
			label: String,
			from: Amount?,
			into: Amount,
			category: Category.Ref? = null,
		): Ref

		/**
		 * Searches for transactions.
		 *
		 * Parameters that are `null` have no effect.
		 *
		 * ### Authorization
		 *
		 * Users can search for transactions they have created.
		 *
		 * @param mostRecentFirst If `true`, transactions are sorted by creation date in descending order.
		 * If `false`, transactions are sorted by creation date in ascending order.
		 */
		fun search(
			label: String? = null,
			start: Instant? = null,
			end: Instant? = null,
			mostRecentFirst: Boolean = true,
		): Flow<Ref>

		/**
		 * Computes the total amount a user has in each currency.
		 *
		 * Parameters that are `null` have no effect.
		 *
		 * For example, if the following transactions exist:
		 * - Salary, 1000€
		 * - Travel, -50¥
		 * - Conversion, 50€ → 500¥
		 * then this method will return:
		 * - 950€
		 * - 450¥
		 *
		 * If you are interested in the total for a specific amount, see [total].
		 *
		 * ### Authorization
		 *
		 * Users can total transactions thay have created.
		 */
		suspend fun totals(
			start: Instant? = null,
			end: Instant? = null,
		): List<Amount>

		/**
		 * Computes the total amount a user has in the specified [currency].
		 *
		 * Parameters that are `null` have no effect.
		 *
		 * For example, if the following transactions exist:
		 * - Salary, 1000€
		 * - Travel, -50¥
		 * - Conversion, 50€ → 500¥
		 * then this method, when totaling euros, will return 950€.
		 *
		 * If you are interested in the totals for all amounts, see [totals].
		 *
		 * ### Authorization
		 *
		 * Users can total transactions thay have created.
		 */
		suspend fun total(
			currency: Currency.Ref,
			start: Instant? = null,
			end: Instant? = null,
		): Amount
	}

	interface Ref : BaseRef<Transaction> {

		/**
		 * Modifies information about an existing transaction.
		 *
		 * The parameters correspond to the fields of the [Transaction] class.
		 * `null` means "this field should not be modified".
		 *
		 * ### Authorization
		 *
		 * The user who created the transaction is the only one who can modify it.
		 */
		suspend fun edit(
			at: Instant? = null,
			label: String? = null,
			from: Amount? = null,
			into: Amount? = null,
		)

		/**
		 * Sets the category of this transaction for the current user.
		 *
		 * ### Authorization
		 *
		 * The user who created the transaction is the only one who can modify it.
		 * They must also have created the [category].
		 *
		 * @see decategorize
		 */
		suspend fun categorize(category: Category.Ref)

		/**
		 * Unsets the category of this transaction for the current user.
		 *
		 * ### Authorization
		 *
		 * The user who created the transaction is the only one who can modify it.
		 */
		suspend fun decategorize()

		suspend fun delete()

		override val service: Service
	}
}
