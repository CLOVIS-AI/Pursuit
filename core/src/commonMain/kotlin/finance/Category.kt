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

package opensavvy.pursuit.finance

import kotlinx.coroutines.flow.Flow
import opensavvy.pursuit.base.BaseEntity
import opensavvy.pursuit.base.BaseRef
import opensavvy.pursuit.base.BaseService
import kotlin.time.Instant

/**
 * A category represents the 'kind' of a [Transaction].
 *
 * For example, categories could be 'Groceries', 'Entertainment', or 'Transportation'.
 * Visually, categories are represented by the transaction's icon.
 *
 * Categories are hierarchical: users can create subcategories to further separate
 * their spending. See [parent].
 *
 * Categories are personal: each user has their own. It is not possible to share categories.
 */
data class Category(
	/**
	 * Human-readable label for this category.
	 *
	 * The label should be kept short as it is often displayed in the UI.
	 */
	val label: String,
	/**
	 * Description of this category.
	 *
	 * The description may be multi-line.
	 */
	val description: String?,
	/**
	 * Parent category, if any.
	 *
	 * To get the children categories, see [Ref.children].
	 */
	val parent: Ref?,
) : BaseEntity {

	/**
	 * A [Category] associated with the total of all transactions assigned to that category.
	 */
	data class CategoryTotal(
		/**
		 * The category this total is for.
		 */
		val category: Ref,

		/**
		 * The total spending for this category, for each [Transaction.Amount.currency].
		 *
		 * A user could assign spending in different currencies for a single category,
		 * so there may be multiple results.
		 */
		val totals: List<Transaction.Amount>,
	)

	interface Service : BaseService<Category> {

		/**
		 * Create a new [Category].
		 *
		 * The parameters correspond to the fields of the [Category] class.
		 *
		 * ### Authorization
		 *
		 * The user who creates the category is the only one who has access to it.
		 */
		suspend fun create(
			label: String,
			description: String? = null,
			parent: Category? = null,
		): Ref

		/**
		 * Searches for categories.
		 *
		 * ### Authorization
		 *
		 * Users can search for categories they have created.
		 *
		 * @param text Searches within [Category.label] and [Category.description].
		 * If absent, all categories are returned.
		 * @param parent Searches only within the children (including indirect) of the specified
		 * category. If absent, all categories are returned.
		 */
		fun search(
			text: String? = null,
			parent: Ref? = null,
		): Flow<Ref>

		/**
		 * Computes the total of all transactions across all categories.
		 *
		 * Note that a single category may contain transactions in different currencies.
		 * See [CategoryTotal].
		 *
		 * Parameters that are `null` have no effect.
		 *
		 * ### Authorization
		 *
		 * Users can total transactions they have created.
		 */
		suspend fun totals(
			start: Instant? = null,
			end: Instant? = null,
		): List<CategoryTotal>

	}

	interface Ref : BaseRef<Category> {

		/**
		 * Modifies information about an existing category.
		 *
		 * The parameters correspond to the fields in the [Category] class.
		 * `null` means "this field should not be modified".
		 *
		 * ### Authorization
		 *
		 * The user who created the category is the only one who can modify it.
		 */
		suspend fun edit(
			label: String? = null,
			description: String? = null,
			parent: Category? = null,
		)

		/**
		 * Deletes this category.
		 *
		 * All transactions that have been [categorized][Transaction.Ref.categorize] become uncategorized.
		 *
		 * ### Authorization
		 *
		 * The user who created the category is the only one who can delete it.
		 */
		suspend fun delete()

		/**
		 * Lists subcategories of this category.
		 *
		 * ### Authorization
		 *
		 * The user who created the category is the only one who can see the subcategories.
		 *
		 * @param includeIndirect If `true`, indirect subcategories are included in the result.
		 * If `false`, only direct subcategories are included.
		 */
		suspend fun children(
			includeIndirect: Boolean = true,
		): List<Ref>

		/**
		 * Computes the total of all transactions in this category.
		 *
		 * ### Authorization
		 *
		 * The user who created the category is the only one who can see the total.
		 */
		suspend fun total(
			start: Instant? = null,
			end: Instant? = null,
		): CategoryTotal

		/**
		 * Computes the total of all transactions in this category that were made in the specified [currency].
		 *
		 * ### Authorization
		 *
		 * The user who created the category is the only one who can see the total.
		 */
		suspend fun total(
			currency: Currency.Ref,
			start: Instant? = null,
			end: Instant? = null,
		): Transaction.Amount {
			val totals = total(start, end).totals
			val total = totals.find { it.currency == currency }
				?: return Transaction.Amount(0L, currency)
			return total
		}

		override val service: Service
	}

}
