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

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.toList
import opensavvy.pursuit.finance.Currency
import opensavvy.pursuit.finance.Transaction
import opensavvy.pursuit.users.User
import opensavvy.telegram.entity.InlineKeyboardButton
import opensavvy.telegram.entity.InlineKeyboardMarkup
import opensavvy.telegram.sdk.BotRouter
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days

fun BotRouter.Builder.transactionCommands(
	users: User.Service,
	currencies: Currency.Service,
	transactions: Transaction.Service,
) {

	users.authCommand("/spend", "Create a new transaction with an amount. For example: /spend 5 €") { msg ->
		val amountStr = msg.text?.split(" ")

		if (amountStr == null || amountStr.size != 3) { // command + amount + currency
			msg.reply("Invalid amount format. Please use the format /spend <amount> <currency>.\n\nFor example: /spend 5 €")
			return@authCommand
		}

		val amountValue = amountStr[1].toDoubleOrNull() ?: amountStr[1].toLongOrNull()?.toDouble()

		if (amountValue == null) {
			msg.reply("Invalid amount format. The amount should be a valid number.\n\nFor example: /spend 5 €")
			return@authCommand
		}

		val candidateCurrencyText = amountStr[2]
		var selectedCurrencyRef: Currency.Ref? = null

		while (selectedCurrencyRef == null) {
			val candidates = currencies.search(text = candidateCurrencyText).toList()

			if (candidates.size == 1) {
				selectedCurrencyRef = candidates.first()
			} else {
				val candidatesValues = coroutineScope {
					candidates.map {
						async {
							val currency = it.read()
							currency to it
						}
					}.awaitAll()
				}

				fun generateCallbackData(currency: Currency): String =
					"${currency.name}_${currency.symbol}_${currency.description}"

				val callbackDataToRef = candidatesValues.associate { (currency, ref) ->
					generateCallbackData(currency!!) to ref
				}

				val chooseCurrency = msg.reply(
					text = "Multiple currencies found for text: $candidateCurrencyText. Please choose one.",
					replyMarkup = InlineKeyboardMarkup(
						listOf(
							candidatesValues.map { (currency, ref) ->
								InlineKeyboardButton(
									text = "${currency!!.name} • ${currency.symbol}",
									callbackData = generateCallbackData(currency),
								)
							}
						)
					)
				)

				selectFirst {
					chooseCurrency.callbackQuery {
						selectedCurrencyRef = callbackDataToRef[it.data]
					}
				}
			}
		}

		val currencyRef = selectedCurrencyRef!!
		val currency = currencyRef.read()!!
		val amount = Transaction.Amount(amountValue, currency, currencyRef)

		val form = Form("Creating a new transaction")
		form.start(replyTo = msg)

		form.acceptValue("Amount", amount.toShortString(currency))

		val label = form.field(
			name = "Label",
			question = "How would you name this transaction?",
		)

		form.end()

		val _ = transactions.create(
			at = Clock.System.now(),
			label = label!!,
			from = null,
			into = amount.copy(amount = -amount.amount),
			category = null,
		)

		msg.reply("New transaction created! Use /monthly to see your recent transactions.")
	}

	users.authCommand("/monthly", "List transactions from the last 31 days") { msg ->
		val now = Clock.System.now()

		val result = buildString {
			appendLine("Recent transactions:")
			appendLine()

			transactions.search(
				start = now - 31.days,
				end = now,
				mostRecentFirst = false,
			).collect {
				val transaction = it.read()!!

				appendLine("${transaction.at} • ${transaction.into.toShortString()} • ${transaction.label}")
			}

			appendLine()
			appendLine("Total:")
			transactions.totals(
				start = now - 31.days,
				end = now,
			).forEach {
				appendLine(" • ${it.toShortString()}")
			}
		}

		msg.reply(result)
	}
}
