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

package opensavvy.pursuit.users

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.withContext
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

private class CurrentUser(
	val user: User.Ref,
) : AbstractCoroutineContextElement(CurrentUser) {

	companion object : CoroutineContext.Key<CurrentUser>
}

/**
 * Obtains a [CoroutineContext] instance which contains the receiver user.
 *
 * In most cases, see [executeAs].
 */
fun User.Ref.asCoroutineContext(): CoroutineContext =
	CurrentUser(this)

/**
 * Executes [block] as the given [user].
 *
 * ### Example
 *
 * ```kotlin
 * val userA = …
 * val userB = …
 *
 * executeAs(userA) {
 *     // This code is authenticated as userA
 *     println(currentUser()) // userA
 * }
 *
 * executeAs(userB) {
 *     // This code is authenticated as userB
 *     println(currentUser()) // userB
 * }
 * ```
 *
 * @see currentUser
 */
suspend fun executeAs(user: User.Ref, block: suspend () -> Unit) =
	withContext(user.asCoroutineContext()) { block() }

/**
 * Returns the current user.
 *
 * @see executeAs Execute a block of code as a specific user.
 * @throws IllegalStateException If no user has been declared.
 */
suspend fun currentUser(): User.Ref {
	val currentUser = currentCoroutineContext()[CurrentUser]
		?: error("No user is currently logged in. To declare a user, use executeAs().")

	return currentUser.user
}

/**
 * Returns the current user.
 *
 * @see executeAs Execute a block of code as a specific user.
 * @return The current user, or `null` if no user has been declared.
 */
suspend fun currentUserOrNull(): User.Ref? =
	currentCoroutineContext()[CurrentUser]?.user
