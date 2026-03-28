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
import opensavvy.pursuit.finance.Category
import opensavvy.pursuit.users.User
import opensavvy.pursuit.users.executeAs

private val templateCategories = listOf(
	"Food",
	"Transport",
	"Entertainment",
	"Shopping",
	"Health",
	"Education",
	"Utilities",
	"Finance",
)

/**
 * Generates a new category specifically for this test.
 */
context(_: TestDsl)
internal suspend fun Category.Service.testCategory(
	owner: User.Ref,
	label: String? = null,
	description: String? = null,
	parent: Category.Ref? = null,
): Category.Ref = executeAs(owner) {
	val defaultLabel = templateCategories.random(
		contextOf<TestDsl>().random.accessUnsafe()
	)

	this.create(
		label = label ?: defaultLabel,
		description = description,
		parent = parent,
	)
}

/**
 * Generates a new category specifically for this test.
 */
fun Prepared<Category.Service>.testCategory(
	owner: Prepared<User.Ref>,
	label: String? = null,
	description: String? = null,
	parent: Prepared<Category.Ref>? = null,
): PreparedProvider<Category.Ref> = prepared {
	this@testCategory().testCategory(owner(), label, description, parent?.invoke())
}
