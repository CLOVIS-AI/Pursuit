/*
 * Copyright (c) 2025, OpenSavvy and contributors.
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

import com.mongodb.kotlin.client.coroutine.MongoDatabase
import opensavvy.ktmongo.coroutines.asKtMongo
import opensavvy.pursuit.base.BaseService
import opensavvy.pursuit.base.ServiceContainer
import opensavvy.pursuit.integration.mongodb.finance.*

class PursuitMongoDB(
	database: MongoDatabase,
) : ServiceContainer {

	private val currencies = MongoCurrencyService(
		collection = database.getCollection<MongoCurrency>("currencies").asKtMongo(),
	)

	private val wallets = MongoWalletService(
		collection = database.getCollection<MongoWallet>("wallets").asKtMongo(),
		currencies = currencies,
	)

	private val transactions = MongoTransactionService(
		collection = database.getCollection<MongoTransaction>("transactions").asKtMongo(),
		wallets = wallets,
	)

	override val services: Sequence<BaseService<*>> = sequenceOf(
		currencies,
		wallets,
		transactions,
	)
}
