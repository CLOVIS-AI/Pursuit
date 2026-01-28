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
 * Interface marker for entities.
 *
 * An entity is a class that stores pure business data.
 *
 * An entity doesn't provide methods to act on that data.
 * These methods are available on the [BaseRef].
 * As a result, the same entity can be reused between multiple integrations.
 *
 * An entity typically doesn't know its own ID, as different integrations may use different IDs.
 */
interface BaseEntity
