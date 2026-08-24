package com.prsnl.core.subscription

import kotlinx.coroutines.flow.StateFlow

interface SubscriptionRepository {
    val entitlement: StateFlow<UserEntitlement>
    
    suspend fun purchasePlan(plan: SubscriptionPlan): Result<Boolean>
    suspend fun restorePurchases(): Result<Boolean>
    suspend fun cancelSubscription(): Result<Boolean>
}
