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
 * Marker interface for entity services.
 *
 * An entity services is the entrypoint to handling an entity.
 * Typically, an entity service holds the methods to list or create entities, while methods
 * to act on a specific entity are held by its [BaseRef].
 *
 * Methods in this interface should prefer parameters and return types of [BaseRef] instead of [BaseEntity].
 */
interface BaseService<E : BaseEntity>
