package com.prsnl.core.subscription

enum class SubscriptionPlan(
    val id: String,
    val title: String,
    val priceFormatted: String,
    val billingPeriod: String,
    val savingsBadge: String? = null
) {
    FREE(
        id = "free_tier",
        title = "Free Workspace",
        priceFormatted = "₹0",
        billingPeriod = "Forever"
    ),
    PRO_YEARLY(
        id = "prsnl_pro_yearly",
        title = "prsnl Pro Annual",
        priceFormatted = "₹299",
        billingPeriod = "/ year",
        savingsBadge = "Best Value • ₹25/mo"
    )
}

data class UserEntitlement(
    val isProUser: Boolean = true,
    val activePlan: SubscriptionPlan = SubscriptionPlan.PRO_YEARLY,
    val maxFoldersAllowed: Int = Int.MAX_VALUE,
    val maxNotebooksAllowed: Int = Int.MAX_VALUE,
    val canExportHdPdf: Boolean = true,
    val canUseCustomBrushes: Boolean = true,
    val canSyncCloud: Boolean = true
)
