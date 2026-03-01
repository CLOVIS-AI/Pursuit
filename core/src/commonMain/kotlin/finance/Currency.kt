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

package opensavvy.pursuit.finance

import kotlinx.coroutines.flow.Flow
import opensavvy.pursuit.base.BaseEntity
import opensavvy.pursuit.base.BaseRef
import opensavvy.pursuit.base.BaseService

/**
 * A type of fungible good.
 *
 * The typical use-case is to represent financial currencies, like euros (€), US dollars ($), yen (¥), etc.
 * However, users can create currencies for other types of valuable, like time spent on sleeping, points awarded
 * for collecting tasks, GitLab-style issue weights, etc.
 */
data class Currency(
	/**
	 * The human-readable name of the currency.
	 */
	val name: String,

	/**
	 * A very short string (if possible a single character) representing this currency.
	 *
	 * Typically, this is a character like € or ¥.
	 */
	val symbol: String,

	/**
	 * How many units of the smallest decomposition of this currency makes one of this currency.
	 *
	 * For example, the Euro can be decomposed into 100 cents, so the number to basic is 100.
	 *
	 * The Malagasy ariary can be decomposed into 5 Iraimbilanja, so the number to basic is 5.
	 */
	val numberToBasic: Int,

	/**
	 * A human-readable multi-line description of this currency.
	 */
	val description: String?,
) : BaseEntity {

	interface Service : BaseService<Currency> {

		/**
		 * Creates a new [Currency].
		 *
		 * The parameters correspond to the fields of the [Currency] class.
		 *
		 * ### Authorization
		 *
		 * The user who creates the currency is the only one who has access to it.
		 */
		suspend fun create(
			name: String,
			symbol: String,
			numberToBasic: Int,
			description: String? = null,
		): Ref

		/**
		 * Ensures that the public currency [name] exists.
		 *
		 * Public currencies are visible by all users but are modifiable by no one.
		 *
		 * This function creates the public currency if it doesn't exist yet and updates it if it already does
		 * but has different fields.
		 *
		 * The detection of the update is based on the [name].
		 *
		 * ### Authorization
		 *
		 * **This method should never be called by users.** It is a system function only.
		 * It should be called on start-up with a set of default currencies.
		 */
		suspend fun ensurePublic(
			name: String,
			symbol: String,
			numberToBasic: Int,
			description: String? = null,
		): Ref

		/**
		 * Searches for currencies.
		 *
		 * ### Authorization
		 *
		 * Users can only see the currencies they created.
		 *
		 * @param text An optional filter.
		 *
		 * Implementations are encouraged to use fuzzy search on the fields [Currency.name], [Currency.symbol]
		 * and [Currency.description].
		 *
		 * Implementations must not ignore this parameter: if the underlying engine doesn't support search,
		 * implementations must perform the search themselves (e.g., with [filter][kotlinx.coroutines.flow.filter]).
		 */
		fun search(
			text: String? = null,
		): Flow<Ref>

	}

	interface Ref : BaseRef<Currency> {
		override val service: Service

		/**
		 * Whether the current user can edit this currency.
		 *
		 * `true` means the current user is allowed to edit the currency.
		 */
		val canEdit: Boolean

		/**
		 * Modifies information about an existing currency.
		 *
		 * The fields correspond to the fields of the [Currency] class.
		 * `null` means "this field should not be modified".
		 *
		 * ### Authorization
		 *
		 * The user who created the currency is the only one who can modify it.
		 */
		suspend fun edit(
			name: String? = null,
			symbol: String? = null,
			numberToBasic: Int? = null,
			description: String? = null,
		)
	}
}

/**
 * Uses [ensurePublic] to create a set of standard currencies.
 *
 * The system should call this function on start-up.
 *
 * ### Authorization
 *
 * **This method should never be called by users.** It is a system function only.
 * It should be called on start-up with a set of default currencies.
 */
@Suppress("RETURN_VALUE_NOT_USED")
suspend fun Currency.Service.ensureStandardCurrencies() {
	// Taken from https://en.wikipedia.org/wiki/List_of_circulating_currencies

	ensurePublic("Afghan afghani", "؋", numberToBasic = 100)
	ensurePublic("Abkhazian apsar", "аԥ", numberToBasic = 1)
	ensurePublic("Malagasy ariary", "Ar", numberToBasic = 5)
	ensurePublic("Thai baht", "฿", numberToBasic = 100)
	ensurePublic("Panamanian balboa", "B/", numberToBasic = 100)
	ensurePublic("Ethiopian birr", "Br", numberToBasic = 100)
	ensurePublic("Venezuelan digital bolívar", "Bs.D", numberToBasic = 100)
	ensurePublic("Venezuelan sovereign bolívar", "Bs.S", numberToBasic = 100)
	ensurePublic("Bolivian boliviano", "Bs", numberToBasic = 100)
	ensurePublic("Ghanaian cedi", "₵", numberToBasic = 100)
	ensurePublic("Costa Rican colón", "₡", numberToBasic = 100)
	ensurePublic("Nicaraguan córdoba", "C$", numberToBasic = 100)
	ensurePublic("Gambian dalasi", "D", numberToBasic = 100)
	ensurePublic("Macedonian denar", "DEN", numberToBasic = 100)
	ensurePublic("Algerian dinar", "DA", numberToBasic = 100)
	ensurePublic("Bahraini dinar", "BD", numberToBasic = 1000)
	ensurePublic("Iraqi dinar", "ID", numberToBasic = 1000)
	ensurePublic("Jordanian dinar", "JD", numberToBasic = 100)
	ensurePublic("Kuwaiti dinar", "KD", numberToBasic = 1000)
	ensurePublic("Libyan dinar", "LD", numberToBasic = 1000)
	ensurePublic("Serbian dinar", "DIN", numberToBasic = 100)
	ensurePublic("Tunisian dinar", "DT", numberToBasic = 1000)
	ensurePublic("Moroccan dirham", "DH", numberToBasic = 100)
	ensurePublic("United Arab Emirates dirham", "AED", numberToBasic = 100)
	ensurePublic("São Tomé and Príncipe dobra", "Db", numberToBasic = 100)
	ensurePublic("Australian dollar", "$", numberToBasic = 100)
	ensurePublic("Bahamian dollar", "$", numberToBasic = 100)
	ensurePublic("Belize dollar", "$", numberToBasic = 100)
	ensurePublic("Bermudian dollar", "$", numberToBasic = 100)
	ensurePublic("Brunei dollar", "$", numberToBasic = 100)
	ensurePublic("Canadian dollar", "$", numberToBasic = 100)
	ensurePublic("Cayman Islands dollar", "$", numberToBasic = 100)
	ensurePublic("Cook Islands dollar", "$", numberToBasic = 100)
	ensurePublic("Eastern Caribbean dollar", "EC$", numberToBasic = 100)
	ensurePublic("Fijian dollar", "$", numberToBasic = 100)
	ensurePublic("Guyanese dollar", "$", numberToBasic = 100)
	ensurePublic("Hong Kong dollar", "$", numberToBasic = 100)
	ensurePublic("Jamaican dollar", "$", numberToBasic = 100)
	ensurePublic("Kiribati dollar", "$", numberToBasic = 100)
	ensurePublic("Liberian dollar", "$", numberToBasic = 100)
	ensurePublic("Namibian dollar", "$", numberToBasic = 100)
	ensurePublic("New Taiwan dollar", "$", numberToBasic = 100)
	ensurePublic("New Zealand dollar", "$", numberToBasic = 100)
	ensurePublic("Niue dollar", "$", numberToBasic = 100)
	ensurePublic("Pitcairn Islands dollar", "$", numberToBasic = 100)
	ensurePublic("Singapore dollar", "$", numberToBasic = 100)
	ensurePublic("Solomon Islands dollar", "$", numberToBasic = 100)
	ensurePublic("Surinamese dollar", "$", numberToBasic = 100)
	ensurePublic("Trinidad and Tobago dollar", "$", numberToBasic = 100)
	ensurePublic("Tuvaluan dollar", "$", numberToBasic = 100)
	ensurePublic("United States dollar", "$", numberToBasic = 100)
	ensurePublic("Vietnamese đồng", "₫", numberToBasic = 10)
	ensurePublic("Armenian dram", "֏", numberToBasic = 100)
	ensurePublic("Cape Verdean escudo", "CVE", numberToBasic = 100)
	ensurePublic("Euro", "€", numberToBasic = 100)
	ensurePublic("Aruban florin", "ƒ", numberToBasic = 100)
	ensurePublic("Hungarian forint", "Ft", numberToBasic = 100)
	ensurePublic("Burundian franc", "FBu", numberToBasic = 100)
	ensurePublic("Central African CFA franc", "FCFA", numberToBasic = 100)
	ensurePublic("CFP franc", "₣", numberToBasic = 100)
	ensurePublic("Comorian franc", "FC", numberToBasic = 100)
	ensurePublic("Congolese franc", "FC", numberToBasic = 100)
	ensurePublic("Djiboutian franc", "Fdj", numberToBasic = 100)
	ensurePublic("Guinean franc", "Fr", numberToBasic = 100)
	ensurePublic("Rwandan franc", "FRw", numberToBasic = 100)
	ensurePublic("Swiss franc", "Fr", numberToBasic = 100)
	ensurePublic("West African CFA franc", "F.CFA", numberToBasic = 100)
	ensurePublic("Haitian gourde", "G", numberToBasic = 100)
	ensurePublic("Paraguayan guaraní", "₲", numberToBasic = 100)
	ensurePublic("Caribbean guilder", "Cg", numberToBasic = 100)
	ensurePublic("Ukrainian hryvnia", "₴", numberToBasic = 100)
	ensurePublic("Papua New Guinean kina", "K", numberToBasic = 100)
	ensurePublic("Lao kip", "₭", numberToBasic = 100)
	ensurePublic("Czech koruna", "Kč", numberToBasic = 100)
	ensurePublic("Faroese króna", "kr", numberToBasic = 100)
	ensurePublic("Icelandic króna", "kr", numberToBasic = 100)
	ensurePublic("Swedish krona", "kr", numberToBasic = 100)
	ensurePublic("Danish krone", "kr", numberToBasic = 100)
	ensurePublic("Norwegian krone", "kr", numberToBasic = 100)
	ensurePublic("Malawian kwacha", "K", numberToBasic = 100)
	ensurePublic("Zambian kwacha", "K", numberToBasic = 100)
	ensurePublic("Angolan kwanza", "Kz", numberToBasic = 100)
	ensurePublic("Burmese kyat", "Ks", numberToBasic = 100)
	ensurePublic("Georgian lari", "₾", numberToBasic = 100)
	ensurePublic("Albanian lek", "L", numberToBasic = 100)
	ensurePublic("Honduran lempira", "L", numberToBasic = 100)
	ensurePublic("Sierra Leonean leone", "Le", numberToBasic = 100)
	ensurePublic("Moldovan leu", "Lei", numberToBasic = 100)
	ensurePublic("Romanian leu", "lei", numberToBasic = 100)
	ensurePublic("Swazi lilangeni", "E", numberToBasic = 100)
	ensurePublic("Turkish lira", "₺", numberToBasic = 100)
	ensurePublic("Lesotho loti", "M", numberToBasic = 100)
	ensurePublic("Azerbaijani manat", "₼", numberToBasic = 100)
	ensurePublic("Turkmenistani manat", "m", numberToBasic = 100)
	ensurePublic("Bosnia and Herzegovina convertible mark", "KM", numberToBasic = 100)
	ensurePublic("Mozambican metical", "Mt", numberToBasic = 100)
	ensurePublic("Nigerian naira", "₦", numberToBasic = 100)
	ensurePublic("Eritrean nakfa", "Nkf", numberToBasic = 100)
	ensurePublic("Bhutanese ngultrum", "Nu", numberToBasic = 100)
	ensurePublic("Mauritanian ouguiya", "UM", numberToBasic = 5)
	ensurePublic("Tongan paʻanga", "T$", numberToBasic = 100)
	ensurePublic("Macanese pataca", "ptc", numberToBasic = 100)
	ensurePublic("Sahrawi peseta", "Pts", numberToBasic = 100)
	ensurePublic("Argentine peso", "$", numberToBasic = 100)
	ensurePublic("Chilean peso", "$", numberToBasic = 100)
	ensurePublic("Colombian peso", "$", numberToBasic = 100)
	ensurePublic("Cuban peso", "$", numberToBasic = 100)
	ensurePublic("Dominican peso", "$", numberToBasic = 100)
	ensurePublic("Mexican peso", "$", numberToBasic = 100)
	ensurePublic("Philippine peso", "₱", numberToBasic = 100)
	ensurePublic("Uruguayan peso", "$", numberToBasic = 100)
	ensurePublic("Egyptian pound", "LE", numberToBasic = 100)
	ensurePublic("Falkland Islands pound", "£", numberToBasic = 100)
	ensurePublic("Gibraltar pound", "£", numberToBasic = 100)
	ensurePublic("Guernsey pound", "£", numberToBasic = 100)
	ensurePublic("Jersey pound", "£", numberToBasic = 100)
	ensurePublic("Lebanese pound", "£", numberToBasic = 100)
	ensurePublic("Manx pound", "£", numberToBasic = 100)
	ensurePublic("Saint Helena pound", "£", numberToBasic = 100)
	ensurePublic("South Sudanese pound", "SS£", numberToBasic = 100)
	ensurePublic("Sterling", "£", numberToBasic = 100)
	ensurePublic("Sudanese pound", "LS", numberToBasic = 100)
	ensurePublic("Syrian pound", "LS", numberToBasic = 100)
	ensurePublic("Botswana pula", "P", numberToBasic = 100)
	ensurePublic("Guatemalan quetzal", "Q", numberToBasic = 100)
	ensurePublic("South African rand", "R", numberToBasic = 100)
	ensurePublic("Brazilian real", "R$", numberToBasic = 100)
	ensurePublic("Iranian rial", "RIs", numberToBasic = 100)
	ensurePublic("Omani rial", "RO", numberToBasic = 1000)
	ensurePublic("Yemeni rial", "RIs", numberToBasic = 100)
	ensurePublic("Cambodian riel", "៛", numberToBasic = 100)
	ensurePublic("Malaysian ringgit", "RM", numberToBasic = 100)
	ensurePublic("Qatari riyal", "QR", numberToBasic = 100)
	ensurePublic("Saudi riyal", "SAR", numberToBasic = 100)
	ensurePublic("Belarusian ruble", "Br", numberToBasic = 100)
	ensurePublic("Russian ruble", "₽", numberToBasic = 100)
	ensurePublic("Transnistrian ruble", "₽", numberToBasic = 100)
	ensurePublic("Maldivian rufiyaa", "Rf", numberToBasic = 100)
	ensurePublic("Indian rupee", "₹", numberToBasic = 100)
	ensurePublic("Mauritian rupee", "Rs", numberToBasic = 100)
	ensurePublic("Nepalese rupee", "रु", numberToBasic = 100)
	ensurePublic("Pakistani rupee", "Rs", numberToBasic = 100)
	ensurePublic("Seychellois rupee", "Rs", numberToBasic = 100)
	ensurePublic("Sri Lankan rupee", "Rs", numberToBasic = 100)
	ensurePublic("Indonesian rupiah", "Rp", numberToBasic = 100)
	ensurePublic("Israeli new shekel", "₪", numberToBasic = 100)
	ensurePublic("Kenyan shilling", "Shs", numberToBasic = 100)
	ensurePublic("Somali shilling", "Shs", numberToBasic = 100)
	ensurePublic("Somaliland shilling", "Shs", numberToBasic = 100)
	ensurePublic("Tanzanian shilling", "Shs", numberToBasic = 100)
	ensurePublic("Ugandan shilling", "Shs", numberToBasic = 1)
	ensurePublic("Peruvian sol", "S/", numberToBasic = 100)
	ensurePublic("Kyrgyz som", "⃀", numberToBasic = 100)
	ensurePublic("Tajikistani somoni", "SM", numberToBasic = 100)
	ensurePublic("Uzbekistani sum", "Sʻ", numberToBasic = 100)
	ensurePublic("Bangladeshi taka", "৳", numberToBasic = 100)
	ensurePublic("Samoan tālā", "$", numberToBasic = 100)
	ensurePublic("Kazakhstani tenge", "₸", numberToBasic = 100)
	ensurePublic("Mongolian tögrög", "₮", numberToBasic = 100)
	ensurePublic("Vanuatu vatu", "VT", numberToBasic = 100)
	ensurePublic("North Korean won", "₩", numberToBasic = 100)
	ensurePublic("South Korean won", "₩", numberToBasic = 100)
	ensurePublic("Japanese yen", "¥", numberToBasic = 1)
	ensurePublic("Renminbi", "¥", numberToBasic = 10)
	ensurePublic("Zimbabwe gold", "ZiG", numberToBasic = 1)
	ensurePublic("Polish złoty", "zł", numberToBasic = 100)
}
