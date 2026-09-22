package com.example.inventory.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

private val mwkFormat = DecimalFormat("#,##0.00", DecimalFormatSymbols(Locale.US))

/** Formats an amount as Malawi Kwacha, e.g. "MWK 1,250,000.00". */
fun formatMwk(amount: Double): String = "MWK ${mwkFormat.format(amount)}"

/** Compact form without the currency prefix, for tight layouts, e.g. "1,250,000.00". */
fun formatMwkAmount(amount: Double): String = mwkFormat.format(amount)
