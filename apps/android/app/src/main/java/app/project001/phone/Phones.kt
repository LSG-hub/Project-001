package app.project001.phone

import android.content.Context
import android.telephony.TelephonyManager
import io.michaelrocks.libphonenumber.android.NumberParseException
import io.michaelrocks.libphonenumber.android.PhoneNumberUtil
import io.michaelrocks.libphonenumber.android.Phonenumber.PhoneNumber
import java.text.Collator
import java.text.Normalizer
import java.util.Locale

data class Country(
    /** ISO 3166 region, e.g. "IN". */
    val region: String,
    /** Calling code without the plus, e.g. 91. */
    val dialCode: Int,
    /** Name in the phone's language. */
    val name: String,
    /** English name, so search works in English whatever the phone's language. */
    val englishName: String,
) {
    val flag: String get() = flagOf(region)
    val plusCode: String get() = "+$dialCode"
}

/** The flag emoji for a two-letter region, built from regional indicator letters. */
fun flagOf(region: String): String =
    region.uppercase().filter { it in 'A'..'Z' }.take(2)
        .map { String(Character.toChars(0x1F1E6 + (it - 'A'))) }
        .joinToString("")

/** Phone numbers: countries, the default country, checking and formatting. */
object Phones {
    @Volatile private var cached: PhoneNumberUtil? = null

    fun util(context: Context): PhoneNumberUtil =
        cached ?: synchronized(this) {
            cached ?: PhoneNumberUtil.createInstance(context.applicationContext).also { cached = it }
        }

    fun country(context: Context, region: String, locale: Locale = Locale.getDefault()): Country {
        val r = Locale.Builder().setRegion(region).build()
        return Country(region, util(context).getCountryCodeForRegion(region), r.getDisplayCountry(locale), r.getDisplayCountry(Locale.ENGLISH))
    }

    /** Every country that has phone numbers, sorted by name in the phone's language. */
    fun countries(context: Context, locale: Locale = Locale.getDefault()): List<Country> {
        val u = util(context)
        val collator = Collator.getInstance(locale)
        return u.supportedCallingCodes.flatMap { code ->
            u.getRegionCodesForCountryCode(code)
                .filter { it.length == 2 && it.all(Char::isLetter) }
                .map { region ->
                    val r = Locale.Builder().setRegion(region).build()
                    Country(region, code, r.getDisplayCountry(locale), r.getDisplayCountry(Locale.ENGLISH))
                }
        }.sortedWith { a, b -> collator.compare(a.name, b.name) }
    }

    /** The SIM's country, then the network's, then the phone's language region, then India. */
    fun defaultRegion(context: Context): String {
        val supported = util(context).supportedRegions
        val tm = context.getSystemService(TelephonyManager::class.java)
        val candidates = listOfNotNull(tm?.simCountryIso, tm?.networkCountryIso, Locale.getDefault().country)
        return candidates.map { it.uppercase() }.firstOrNull { it in supported } ?: "IN"
    }

    /** The SIM's country, if there is a SIM. */
    fun simRegion(context: Context): String? {
        val iso = context.getSystemService(TelephonyManager::class.java)?.simCountryIso?.uppercase()
        return iso?.takeIf { it in util(context).supportedRegions }
    }

    fun parse(context: Context, national: String, region: String): PhoneNumber? =
        try { util(context).parse(national, region) } catch (_: NumberParseException) { null }

    /** True once the digits could be a number in [region] (right length); used to enable Continue. */
    fun isPossible(context: Context, national: String, region: String): Boolean =
        parse(context, national, region)?.let { util(context).isPossibleNumber(it) } == true

    /**
     * True if it is a real mobile number pattern for [region]. Sign-in needs the SIM in
     * this phone, so landlines are out. Some countries (US, Canada…) can't tell mobile from
     * landline by the number; those come back FIXED_LINE_OR_MOBILE and are allowed.
     */
    fun isValid(context: Context, national: String, region: String): Boolean {
        val u = util(context)
        val number = parse(context, national, region) ?: return false
        if (!u.isValidNumberForRegion(number, region)) return false
        return u.getNumberType(number) in setOf(
            PhoneNumberUtil.PhoneNumberType.MOBILE,
            PhoneNumberUtil.PhoneNumberType.FIXED_LINE_OR_MOBILE,
        )
    }

    /** "+919876543210", the form the server stores and compares. */
    fun e164(context: Context, national: String, region: String): String =
        parse(context, national, region)?.let { util(context).format(it, PhoneNumberUtil.PhoneNumberFormat.E164) }
            ?: "+${util(context).getCountryCodeForRegion(region)}$national"

    /** "+91 98765 43210" for showing; the server should store E.164. */
    fun international(context: Context, national: String, region: String): String =
        parse(context, national, region)?.let { util(context).format(it, PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL) }
            ?: "+${util(context).getCountryCodeForRegion(region)} $national"

    /**
     * If someone pastes or types a full international number ("+44 7700 900123" or
     * "0044…"), works out its country. Returns (region, national digits) or null.
     */
    fun splitInternational(context: Context, raw: String): Pair<String, String>? {
        val trimmed = raw.trim()
        val normalized = when {
            trimmed.startsWith("+") -> trimmed
            trimmed.startsWith("00") -> "+" + trimmed.drop(2)
            else -> return null
        }
        val u = util(context)
        val number = try { u.parse(normalized, null) } catch (_: NumberParseException) { return null }
        // Wait until it is a whole number, so "+1 242…" isn't taken as the US after "+12".
        if (!u.isPossibleNumber(number)) return null
        val region = u.getRegionCodeForNumber(number)?.takeIf { it in u.supportedRegions }
            ?: u.getRegionCodeForCountryCode(number.countryCode).takeIf { it in u.supportedRegions }
            ?: return null
        return region to u.getNationalSignificantNumber(number)
    }
}

/** Lowercase, accents removed, so "cote" finds "Côte d’Ivoire". */
fun searchKey(s: String): String =
    Normalizer.normalize(s, Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "").lowercase(Locale.ROOT)

fun Country.matches(query: String): Boolean {
    val q = searchKey(query.trim())
    if (q.isEmpty()) return true
    val digits = q.removePrefix("+")
    if (digits.isNotEmpty() && digits.all(Char::isDigit)) return dialCode.toString().startsWith(digits)
    return searchKey(name).contains(q) || searchKey(englishName).contains(q) || region.lowercase() == q
}
