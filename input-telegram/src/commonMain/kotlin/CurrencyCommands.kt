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

import kotlinx.coroutines.flow.toList
import opensavvy.pursuit.finance.Currency
import opensavvy.pursuit.users.User
import opensavvy.telegram.entity.InlineKeyboardButton
import opensavvy.telegram.entity.InlineKeyboardMarkup
import opensavvy.telegram.sdk.BotRouter
import kotlin.time.Duration.Companion.minutes

fun BotRouter.Builder.currencyCommands(
	users: User.Service,
	currencies: Currency.Service,
) {

	users.authCommand("/list_currencies", "List the available currencies. Specify additional text to search for specific currencies.") { msg ->
		val text = (msg.text ?: "").substringAfter("/list_currencies").trim()
			.takeIf { it.isNotBlank() }

		val pageSize = 30
		var page = 0

		suspend fun generate(): Pair<String, InlineKeyboardMarkup> {
			val response = StringBuilder()

			val results = currencies.search(text = text).toList()

			if (results.size > pageSize)
				response.appendLine("Available currencies (page ${page + 1}):")
			else if (results.isEmpty())
				response.appendLine("No currencies found.")
			else
				response.appendLine("Available currencies:")

			results.asSequence()
				.drop(page * pageSize)
				.take(pageSize)
				.forEach { ref ->
					val currency = ref.read() ?: return@forEach

					response.appendLine(" • ${currency.symbol} - ${currency.name}")
				}

			val markup = InlineKeyboardMarkup(
				listOf(
					buildList {
						if (page > 0) {
							add(InlineKeyboardButton("←", callbackData = "page-"))
						}

						if (results.size > (page + 1) * pageSize) {
							add(InlineKeyboardButton("→", callbackData = "page+"))
						}
					}
				)
			)

			return response.toString() to markup
		}

		val (response, markup) = generate()

		val reply = msg.reply(
			text = response,
			replyMarkup = markup,
		)

		selectUntilStopped {
			timeout(5.minutes) {
				stop()
			}

			reply.callbackQuery("page-") {
				if (page > 0)
					page--

				val (response, markup) = generate()
				reply.edit(
					text = response,
					replyMarkup = markup,
				)
			}

			reply.callbackQuery("page+") {
				page++

				val (response, markup) = generate()
				reply.edit(
					text = response,
					replyMarkup = markup,
				)
			}
		}

		// Delete the keyboard
		reply.edit(
			text = generate().first,
		)
	}

	users.authCommand("/create_currency", "Create a new custom currency") { msg ->
		val form = Form(
			name = "Creating a new currency…",
		)

		val name = form.field(
			name = "Name",
			question = "What should be the name of the new currency?",
		)

		val symbol = form.field(
			name = "Symbol",
			question = "What should be the symbol of the new currency?",
			predicate = { it.length <= 3 },
		)

		val numberToBasic = form.field(
			name = "How many of the smallest division in one unit",
			question = "How many of the smallest division in one unit?",
			predicate = { it.toIntOrNull() != null },
		)

		val description = form.field(
			name = "Description",
			question = "What should be the description of the new currency?",
		)

		form.executeAsReplyTo(msg)

		val currency = currencies.create(
			name = name.read()!!,
			symbol = symbol.read()!!,
			numberToBasic = numberToBasic.read()!!.toInt(),
			description = description.read(),
		).read() ?: error("Currency creation failed")

		msg.reply("Currency successfully created: ${currency.symbol} (${currency.name})")
	}

}
