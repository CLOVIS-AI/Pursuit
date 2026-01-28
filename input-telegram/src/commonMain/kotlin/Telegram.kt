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
import opensavvy.telegram.sdk.TelegramBot

suspend fun startTelegramBot(
	token: String,
	services: ServiceContainer,
) {
	val bot = TelegramBot.create(token)

	val me = bot.getMe()
	println("Telegram bot started with username: ${me.username} • $me")

	bot.poll {
		command("/start") {
			bot.sendMessage(it.chat.id, "Welcome to the Pursuit bot! Stay tuned for more features.")
		}

		command("/services") {
			bot.sendMessage(it.chat.id, "Registered services:\n\n • ${services.services.joinToString("\n • ")}")
		}
	}
}
