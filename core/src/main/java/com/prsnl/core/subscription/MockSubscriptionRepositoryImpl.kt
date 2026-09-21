package com.prsnl.core.subscription

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MockSubscriptionRepositoryImpl : SubscriptionRepository {

    private val _entitlement = MutableStateFlow(UserEntitlement(isProUser = true, activePlan = SubscriptionPlan.PRO_YEARLY))
    override val entitlement: StateFlow<UserEntitlement> = _entitlement.asStateFlow()

    override suspend fun purchasePlan(plan: SubscriptionPlan): Result<Boolean> {
        _entitlement.value = UserEntitlement(
            isProUser = true,
            activePlan = SubscriptionPlan.PRO_YEARLY
        )
        return Result.success(true)
    }

    override suspend fun restorePurchases(): Result<Boolean> {
        _entitlement.value = UserEntitlement(
            isProUser = true,
            activePlan = SubscriptionPlan.PRO_YEARLY
        )
        return Result.success(true)
    }

    override suspend fun cancelSubscription(): Result<Boolean> {
        _entitlement.value = UserEntitlement(
            isProUser = true,
            activePlan = SubscriptionPlan.PRO_YEARLY
        )
        return Result.success(true)
    }
}
