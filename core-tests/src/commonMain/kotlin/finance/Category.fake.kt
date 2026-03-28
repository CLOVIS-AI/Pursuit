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

import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import opensavvy.prepared.suite.assertions.matches
import opensavvy.pursuit.finance.Category
import opensavvy.pursuit.users.User
import opensavvy.pursuit.users.currentUser
import kotlin.time.Instant

class FakeCategoryService(
	private val transactions: FakeTransactionService,
) : Category.Service {

	private val categoriesById = mutableMapOf<Long, Category>()
	private val ownersByCategoryId = mutableMapOf<Long, User.Ref>()
	private val lock = Mutex()

	override suspend fun create(
		label: String,
		description: String?,
		parent: Category.Ref?,
	): Category.Ref {
		if (parent != null)
			checkNotNull(parent.read()) { "You do not have access to the specified parent category: $parent" }

		return lock.withLock("create($label, $description, $parent)") {
			val user = currentUser()

			val newId = (categoriesById.keys.maxOrNull() ?: 0) + 1
			val newCategory = Category(
				label = label,
				description = description,
				parent = parent,
			)

			categoriesById[newId] = newCategory
			ownersByCategoryId[newId] = user

			FakeCategoryRef(this, newId)
		}
	}

	override fun search(
		text: String?,
		parent: Category.Ref?,
	): Flow<Category.Ref> = flow {
		val user = currentUser()
		val service = this@FakeCategoryService

		// Query all elements BEFORE emitting in the flow to ensure there is no concurrency with other readers
		val results = lock.withLock("search($text, $parent)") {
			categoriesById
				.asSequence()
				.filter { (id, _) -> service.ownersByCategoryId[id] == user }
				.filter { (id, _) ->
					val ref = FakeCategoryRef(service, id)
					parent == null || ref.hasParentUnsafe(parent, includeIndirect = true, user)
				}
				.filter { (_, it) -> text == null || it.label matches ".*$text.*" || it.description matches ".*$text.*" }
				.map { (id, _) -> FakeCategoryRef(service, id) }
				.toList()
		}

		emitAll(results.asFlow())
	}

	override suspend fun totals(
		start: Instant?,
		end: Instant?,
		currency: Category.Ref?,
	): List<Category.CategoryTotal> {
		val user = currentUser()

		val categories = lock.withLock("totals.listCategories()") {
			categoriesById
				.asSequence()
				.filter { (id, _) -> ownersByCategoryId[id] == user }
				.map { (id, _) -> FakeCategoryRef(this, id) }
				.toList()
		}

		val totals = categories.map {
			it.total(start = start, end = end, includeIndirect = true)
		}.map { total ->
			total.copy(
				totals = total.totals.filter { currency == null || it.currency == currency }
			)
		}

		return totals
	}

	private data class FakeCategoryRef(
		override val service: FakeCategoryService,
		val id: Long,
	) : Category.Ref {

		/**
		 * UNSAFE. Must only be called while holding the [lock].
		 */
		fun hasParentUnsafe(
			targetParent: Category.Ref,
			includeIndirect: Boolean,
			user: User.Ref,
		): Boolean {
			if (service.ownersByCategoryId[this.id] != user) {
				return false
			}

			val category = service.categoriesById[this.id]
				?: return false

			return when (val myParent = category.parent) {
				targetParent -> true
				is FakeCategoryRef if includeIndirect -> myParent.hasParentUnsafe(targetParent, includeIndirect, user)
				else -> false
			}
		}

		override suspend fun edit(
			label: String?,
			description: String?,
			parent: Category.Ref?,
		) {
			val user = currentUser()

			if (parent != null)
				checkNotNull(parent.read()) { "You do not have access to the specified parent category: $parent" }

			service.lock.withLock("edit($id, $label, $description, $parent)") {
				if (service.ownersByCategoryId[id] != user)
					throw IllegalArgumentException("You do not have access to edit this category")

				val current = service.categoriesById[id]
				checkNotNull(current) { "Category $id not found" }

				service.categoriesById[id] = current.copy(
					label = label ?: current.label,
					description = description ?: current.description,
					parent = parent ?: current.parent,
				)
			}
		}

		override suspend fun delete() {
			val user = currentUser()

			service.lock.withLock {
				if (service.ownersByCategoryId[id] != user)
					throw IllegalArgumentException("You do not have access to delete this category")

				service.ownersByCategoryId.remove(id)
				service.categoriesById.remove(id)
			}
		}

		override suspend fun children(
			includeIndirect: Boolean,
		): Set<Category.Ref> = service.lock.withLock("children($id)") {
			val user = currentUser()

			service.categoriesById
				.asSequence()
				.filter { (id, _) -> service.ownersByCategoryId[id] == user }
				.filter { (id, _) ->
					FakeCategoryRef(service, id).hasParentUnsafe(this, includeIndirect, user)
				}
				.map { (id, _) -> FakeCategoryRef(service, id) }
				.toSet()
		}

		override suspend fun total(
			start: Instant?,
			end: Instant?,
			includeIndirect: Boolean,
		): Category.CategoryTotal {
			val user = currentUser()

			check(service.ownersByCategoryId[id] == user) { "User $user does not own category $id" }

			val categories = children(includeIndirect) + this

			val transactions = service.transactions
				.search(start = start, end = end)
				.mapNotNull { it.read() }
				.filter { it.category in categories }
				.toList()

			val currencies = transactions
				.flatMapTo(LinkedHashSet()) { listOfNotNull(it.into.currency, it.from?.currency) }

			val results = currencies.map {
				service.transactions.totalOf(it, transactions.asSequence())
			}

			return Category.CategoryTotal(
				category = this,
				totals = results,
			)
		}

		override suspend fun read(): Category? = service.lock.withLock("read($id)") {
			service.categoriesById[id]
				.takeIf { service.ownersByCategoryId[id] == currentUser() }
		}

		override fun toString() = "FakeCategoryRef($id)"
	}
}
