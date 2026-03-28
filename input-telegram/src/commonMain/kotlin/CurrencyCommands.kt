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

import opensavvy.pursuit.finance.Currency
import opensavvy.pursuit.users.User
import opensavvy.telegram.entity.ReplyParameters
import opensavvy.telegram.sdk.BotRouter
import opensavvy.telegram.sdk.TelegramBot

fun BotRouter.Builder.currencyCommands(
	bot: TelegramBot,
	users: User.Service,
	currencies: Currency.Service,
) {

	users.authCommand("/list_currencies", "List the available currencies. Specify additional text to search for specific currencies.") { msg ->
		val text = (msg.text ?: "").substringAfter("/list_currencies").trim()
			.takeIf { it.isNotBlank() }

		val response = StringBuilder()

		response.appendLine("Available currencies:")
		currencies.search(text = text).collect { ref ->
			val currency = ref.read() ?: return@collect

			response.appendLine(" • ${currency.symbol} - ${currency.name}")
		}

		bot.sendMessage(
			chat = msg.chat.id,
			reply = ReplyParameters(msg.id, ReplyParameters.ChatIdentifier.Id(msg.chat.id)),
			text = response.toString(),
		)
	}

}
