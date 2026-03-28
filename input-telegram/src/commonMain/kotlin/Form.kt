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

import opensavvy.telegram.entity.InlineKeyboardButton
import opensavvy.telegram.entity.InlineKeyboardMarkup
import opensavvy.telegram.entity.Message
import opensavvy.telegram.sdk.BotRouter
import kotlin.time.Duration.Companion.minutes

class Form(
	private val name: String,
) {

	private val fields = ArrayList<FieldImpl>()
	private val values = HashMap<FieldImpl, String>()

	interface Field {
		fun read(): String?
	}

	private inner class FieldImpl(
		val name: String,
		val question: String,
		val predicate: (String) -> Boolean,
	) : Field {

		override fun read(): String? =
			values[this]
	}

	fun field(
		name: String,
		question: String,
		predicate: (String) -> Boolean = { true },
	): Field = FieldImpl(name, question, predicate)
		.also { fields += it }

	private fun generateText() = buildString {
		appendLine(name)
		appendLine()

		for (field in fields) {
			val value = values[field]

			if (value != null) {
				append(field.name)
				append(": ")
				appendLine(value)
			}
		}
	}

	private fun chooseNextField(): FieldImpl? = fields
		.firstOrNull { it !in values }

	context(context: BotRouter.HandlerContext)
	suspend fun executeAsReplyTo(message: Message): Unit = with(context) {
		var nextField = chooseNextField() ?: error("This form contains no fields")

		fun keyboard() = InlineKeyboardMarkup(
			InlineKeyboardButton("Stop", callbackData = "stop")
		)

		val announce = message.reply(
			text = generateText() + "\n" + nextField.question + "\n(reply to this message)",
			replyMarkup = keyboard(),
		)

		selectUntilStopped {
			timeout(5.minutes) {
				stop()
			}

			announce.callbackQuery("stop") {
				stop()
			}

			suspend fun reactTo(newMessage: Message) {
				val text = newMessage.text ?: return

				if (nextField.predicate(text)) {
					values[nextField] = text
					nextField = chooseNextField() ?: stop()
					announce.edit(
						text = generateText() + "\n" + nextField.question + "\n(reply to this message)",
						replyMarkup = keyboard(),
					)
				} else {
					announce.edit(
						text = generateText() + "\n" + nextField.question + "\n(invalid answer; reply to this message)",
						replyMarkup = keyboard(),
					)
				}
			}

			announce.reply {
				reactTo(it)
			}

			update({ it.message != null && it.message?.from?.id == message.from?.id }) {
				reactTo(it.message!!)
			}
		}

		announce.edit(
			text = generateText()
		)
	}
}
