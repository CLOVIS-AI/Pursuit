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

package opensavvy.pursuit.backend

import com.mongodb.kotlin.client.coroutine.MongoClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import opensavvy.pursuit.base.ServiceContainer
import opensavvy.pursuit.base.service
import opensavvy.pursuit.finance.Currency
import opensavvy.pursuit.finance.ensureStandardCurrencies
import opensavvy.pursuit.input.telegram.startTelegramBot
import opensavvy.pursuit.integration.mongodb.PursuitMongoDB

fun main(): Unit = runBlocking(Dispatchers.Default) {
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

	println("\nInitializing currencies…")
	for (currencyService in services.service<Currency.Service>()) {
		currencyService.ensureStandardCurrencies()
	}

	val telegramBotToken: String? = System.getenv("telegram_bot_token")
	if (telegramBotToken != null) {
		println("\nStarting the Telegram bot…")
		startTelegramBot(telegramBotToken, services)
	} else {
		println("\nNo Telegram bot token provided, ignoring the bot. To register the bot, create the environment variable 'telegram_bot_token'.'")
	}
}
