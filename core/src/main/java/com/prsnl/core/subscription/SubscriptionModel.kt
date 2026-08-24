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
    val isProUser: Boolean = false,
    val activePlan: SubscriptionPlan = SubscriptionPlan.FREE,
    val maxFoldersAllowed: Int = if (isProUser) Int.MAX_VALUE else 1,
    val maxNotebooksAllowed: Int = if (isProUser) Int.MAX_VALUE else 3,
    val canExportHdPdf: Boolean = isProUser,
    val canUseCustomBrushes: Boolean = isProUser,
    val canSyncCloud: Boolean = isProUser
)
