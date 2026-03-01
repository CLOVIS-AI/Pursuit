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

package opensavvy.pursuit.integration.mongodb

import com.mongodb.MongoTimeoutException
import com.mongodb.kotlin.client.coroutine.MongoClient
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import opensavvy.ktmongo.coroutines.MongoCollection
import opensavvy.ktmongo.coroutines.asKtMongo
import opensavvy.prepared.suite.PreparedProvider
import opensavvy.prepared.suite.cleanUp
import opensavvy.prepared.suite.prepared
import opensavvy.prepared.suite.random.randomInt
import opensavvy.prepared.suite.shared

private const val testDatabaseName = "pursuit-tests"

/**
 * Connects to the database by testing both:
 * - `localhost:27017`: local development URL
 * - `mongo:27017`: CI URL
 */
val testDatabase by shared(CoroutineName("mongodb-establish-connection")) {
	val options = "connectTimeoutMS=3000&serverSelectionTimeoutMS=3000"
	val client = try {
		MongoClient.create("mongodb://localhost:27017/?$options")
			.also { it.getDatabase(testDatabaseName).getCollection<String>("test").countDocuments() }
	} catch (e: MongoTimeoutException) {
		System.err.println("Cannot connect to localhost:27017. Did you start the docker-compose services? [This is normal in CI]\n${e.stackTraceToString()}")
		MongoClient.create("mongodb://mongo:27017/?$options")
	}
	client.getDatabase(testDatabaseName)
}

val testId by randomInt(0, Int.MAX_VALUE)

/**
 * Creates a new collection in [testDatabase], with a randomly-generated name based on [name].
 *
 * Each test gets its own randomly-generated name, so tests cannot impact eacher other.
 *
 * At the end of the test:
 * - If the test is successful, the collection is dropped.
 * - If the test failed, the collection is dumped to the standard output.
 */
inline fun <reified Document : Any> testCollection(name: String): PreparedProvider<MongoCollection<Document>> = prepared {
	val name = "test-${testId()}-$name"

	val collection = testDatabase().getCollection<Document>(name)
		.asKtMongo()

	cleanUp("Log the collection after failed test", onSuccess = false) {
		println("Collection $name with ${collection.count()} documents:")

		try {
			collection.find().forEach { document ->
				println(" • $document")
			}
		} catch (e: Exception) {
			currentCoroutineContext().ensureActive()
			println("Could not print collection • ${e.stackTraceToString()}")
		}
	}

	cleanUp("Drop the collection $name", onFailure = false) {
		collection.drop()
	}

	collection
}
