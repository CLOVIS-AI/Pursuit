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

	private val fields = ArrayList<Field>()
	private val values = HashMap<Field, Any?>()

	private data class Field(
		val name: String,
	)

	private lateinit var message: Message

	context(context: BotRouter.HandlerContext)
	suspend fun start(replyTo: Message) = with(context) {
		check(!::message.isInitialized) { "Cannot start a form multiple times: \"$name\"" }
		message = replyTo.reply("$name\n\nLoading…")
	}

	context(context: BotRouter.HandlerContext)
	suspend fun end(): Unit = with(context) {
		message.edit(generateText())
	}

	fun <T> acceptValue(
		name: String,
		value: T,
	) {
		val field = Field(name)
		fields += field
		values[field] = value
	}

	@JvmName("fieldString")
	context(context: BotRouter.HandlerContext)
	suspend fun field(
		name: String,
		question: String,
		validate: suspend (String) -> Boolean = { true },
	): String? =
		field(name, question, validate, convert = { it })

	context(context: BotRouter.HandlerContext)
	suspend fun <T> field(
		name: String,
		question: String,
		validate: suspend (String) -> Boolean = { true },
		convert: suspend (String) -> T,
	): T? = with(context) {
		val field = Field(name)
		fields += field

		fun keyboard() = InlineKeyboardMarkup(
			InlineKeyboardButton("Stop", callbackData = "stop")
		)

		message.edit(
			text = generateText() + "\n" + question + "\n(reply to this message)",
			replyMarkup = keyboard()
		)

		var result: T? = null
		selectUntilStopped {
			timeout(5.minutes) {
				stop()
			}

			message.callbackQuery("stop") {
				stop()
			}

			suspend fun reactTo(newMessage: Message) {
				val text = newMessage.text ?: return

				if (validate(text)) {
					val value = convert(text)
					values[field] = value
					result = value
					stop()
				} else {
					message.edit(
						text = generateText() + "\n" + question + "\n(invalid answer; reply to this message)",
						replyMarkup = keyboard(),
					)
				}
			}

			message.reply {
				reactTo(it)
			}

			update({ it.message != null && it.message?.from?.id == message.from?.id }) {
				reactTo(it.message!!)
			}
		}

		result
	}

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
}
