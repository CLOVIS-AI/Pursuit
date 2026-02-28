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

/**
 * A type of fungible good.
 *
 * The typical use-case is to represent financial currencies, like euros (€), US dollars ($), yen (¥), etc.
 * However, users can create currencies for other types of valuable, like time spent on sleeping, points awarded
 * for collecting tasks, GitLab-style issue weights, etc.
 */
data class Currency(
	/**
	 * The human-readable name of the currency.
	 */
	val name: String,

	/**
	 * A very short string (if possible a single character) representing this currency.
	 *
	 * Typically, this is a character like € or ¥.
	 */
	val symbol: String,

	/**
	 * A human-readable multi-line description of this currency.
	 */
	val description: String?,
) : BaseEntity {

	interface Service : BaseService<Currency> {

		/**
		 * Creates a new [Currency].
		 *
		 * The parameters correspond to the fields of the [Currency] class.
		 *
		 * ### Authorization
		 *
		 * The user who creates the currency is the only one who has access to it.
		 */
		suspend fun create(
			name: String,
			symbol: String,
			description: String? = null,
		): Ref

		/**
		 * Searches for currencies.
		 *
		 * ### Authorization
		 *
		 * Users can only see the currencies they created.
		 *
		 * @param text An optional filter.
		 *
		 * Implementations are encouraged to use fuzzy search on the fields [Currency.name], [Currency.symbol]
		 * and [Currency.description].
		 *
		 * Implementations must not ignore this parameter: if the underlying engine doesn't support search,
		 * implementations must perform the search themselves (e.g., with [filter][kotlinx.coroutines.flow.filter]).
		 */
		fun search(
			text: String? = null,
		): Flow<Ref>

	}

	interface Ref : BaseRef<Currency> {
		override val service: Service

		/**
		 * Modifies information about an existing currency.
		 *
		 * The fields correspond to the fields of the [Currency] class.
		 * `null` means "this field should not be modified".
		 *
		 * ### Authorization
		 *
		 * The user who created the currency is the only one who can modify it.
		 */
		suspend fun edit(
			name: String? = null,
			symbol: String? = null,
			description: String? = null,
		)
	}
}
