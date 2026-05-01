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

package opensavvy.pursuit.integration.mongodb.finance

import kotlinx.coroutines.flow.Flow
import opensavvy.ktmongo.bson.types.ObjectId
import opensavvy.pursuit.finance.Category
import kotlin.time.Instant

internal class MongoCategoryService : Category.Service {
	override suspend fun create(label: String, description: String?, parent: Category.Ref?): Category.Ref {
		TODO("Will be implemented in #17")
	}

	override fun search(text: String?, parent: Category.Ref?): Flow<Category.Ref> {
		TODO("Will be implemented in #17")
	}

	override suspend fun totals(start: Instant?, end: Instant?, currency: Category.Ref?): List<Category.CategoryTotal> {
		TODO("Will be implemented in #17")
	}

	inner class MongoCategoryRef(
		val id: ObjectId,
	) : Category.Ref {
		override suspend fun edit(label: String?, description: String?, parent: Category.Ref?) {
			TODO("Will be implemented in #17")
		}

		override suspend fun delete() {
			TODO("Will be implemented in #17")
		}

		override suspend fun children(includeIndirect: Boolean): Set<Category.Ref> {
			TODO("Will be implemented in #17")
		}

		override suspend fun total(start: Instant?, end: Instant?, includeIndirect: Boolean): Category.CategoryTotal {
			TODO("Will be implemented in #17")
		}

		override val service: MongoCategoryService
			get() = this@MongoCategoryService

		override suspend fun read(): Category? {
			TODO("Will be implemented in #17")
		}

		// region Identity

		override fun equals(other: Any?): Boolean = other is MongoCategoryRef
			&& service === other.service
			&& id == other.id

		override fun hashCode(): Int {
			var result = id.hashCode()
			result = 31 * result + service.hashCode()
			return result
		}

		override fun toString(): String = "mongo.Category($id)"

		// endregion
	}
}
