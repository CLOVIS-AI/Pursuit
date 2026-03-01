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

package opensavvy.pursuit.input.telegram

import opensavvy.pursuit.base.ServiceContainer
import opensavvy.pursuit.base.service
import opensavvy.pursuit.users.User
import opensavvy.telegram.sdk.TelegramBot

suspend fun startTelegramBot(
	token: String,
	services: ServiceContainer,
) {
	val bot = TelegramBot.create(token)

	val me = bot.getMe()
	println("Telegram bot started with username: ${me.username} • $me")

	val users = services.service<User.Service>().first()

	bot.poll {
		command("/start", description = "Start the bot and log in") {
			val from = it.from ?: return@command
			val username = from.username

			if (username == null) {
				bot.sendMessage(it.chat.id, "Welcome to the Pursuit bot! \n\nTo continue using this bot, please configure a username in your Telegram settings.")
				return@command
			}

			val user = try {
				users.logInWithTelegram(
					telegramUserId = from.id.value,
					username = username,
					fullName = listOfNotNull(from.firstName, from.lastName).joinToString(" "),
				)
			} catch (e: Exception) {
				System.err.println("Error logging in user $username: $e")
				bot.sendMessage(it.chat.id, "Could not log you in. Please try again later.")
				return@command
			}.read() ?: return@command

			bot.sendMessage(it.chat.id, "Welcome, ${user.fullName}!\n\nYou are logged in to Pursuit. Pursuit is a new personal and financial tracker, to help you pursue your life goals. \n\nStay tuned for updates!")
		}

		command("/services") {
			bot.sendMessage(it.chat.id, "Registered services:\n\n • ${services.services.joinToString("\n • ")}")
		}
	}
}
