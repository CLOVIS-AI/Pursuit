/*
 * Copyright (c) 2025, OpenSavvy and contributors.
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

/**
 * The interface of all [BaseEntity] references.
 *
 * Each entity typically declares its own interface inheriting this one, adding methods to describe actions on that
 * specific entity.
 */
interface BaseRef<E : BaseEntity> {

	/**
	 * The [BaseService] responsible for handling the referenced entity.
	 */
	val service: BaseService<E>

	/**
	 * Attempts to read the referenced entity.
	 *
	 * Returns `null` if the entity doesn't exist.
	 */
	suspend fun read(): E?
}
