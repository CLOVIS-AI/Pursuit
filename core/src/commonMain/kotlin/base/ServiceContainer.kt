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

package opensavvy.pursuit.base

interface ServiceContainer {

	val services: Sequence<BaseService<*>>
}

// region Combined

private class CombinedServiceContainer(
	private val _services: Collection<ServiceContainer>,
) : ServiceContainer {

	override val services: Sequence<BaseService<*>>
		get() = _services.asSequence().flatMap { it.services }
}

/**
 * Combines multiple [containers] into a single one that contains all of them.
 */
fun ServiceContainer(
	vararg containers: ServiceContainer
): ServiceContainer =
	CombinedServiceContainer(containers.asList())

// endregion
// region Accessors

/**
 * Finds all registered services of type [Service].
 */
inline fun <reified Service : BaseService<*>> ServiceContainer.service(): Sequence<Service> =
	services.filterIsInstance<Service>()

// endregion
