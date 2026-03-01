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

import opensavvy.pursuit.users.User
import opensavvy.pursuit.users.executeAs
import opensavvy.telegram.entity.Message
import opensavvy.telegram.sdk.BotRouter

/**
 * Declares a command that requires to be logged in.
 */
context(builder: BotRouter.Builder)
fun User.Service.authCommand(
	text: String,
	description: String,
	handler: suspend (Message) -> Unit,
) = builder.command(text, description) { message ->
	val from = message.from ?: return@command
	val username = from.username ?: return@command

	val user = this.logInWithTelegram(
		telegramUserId = from.id.value,
		username = username,
		fullName = listOfNotNull(from.firstName, from.lastName).joinToString(" "),
	)

	executeAs(user) {
		handler(message)
	}
}
