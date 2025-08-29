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

package opensavvy.pursuit.backend

import com.mongodb.kotlin.client.coroutine.MongoClient
import opensavvy.pursuit.base.ServiceContainer
import opensavvy.pursuit.integration.mongodb.PursuitMongoDB

fun main() {
	val mongoClient = MongoClient.create()
	val database = mongoClient.getDatabase("pursuit-data")

	val services = ServiceContainer(
		PursuitMongoDB(database),
	)

	println("Welcome to Pursuit!")

	println("\nLoaded services:")
	for (service in services.services) {
		println("- $service")
	}
}
