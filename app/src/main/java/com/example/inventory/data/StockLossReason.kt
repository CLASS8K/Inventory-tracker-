package com.example.inventory.data

/** Why a Stock Take's closing count came in lower than opening — only SOLD counts as revenue. */
enum class StockLossReason(val label: String) {
    SOLD("Sold"),
    SPILLAGE("Spillage / Breakage"),
    COMPLIMENTARY("Complimentary / Staff"),
    THEFT("Theft / Loss"),
    OTHER("Other"),
}
