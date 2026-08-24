package com.prsnl.core.subscription

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MockSubscriptionRepositoryImpl : SubscriptionRepository {

    private val _entitlement = MutableStateFlow(UserEntitlement())
    override val entitlement: StateFlow<UserEntitlement> = _entitlement.asStateFlow()

    override suspend fun purchasePlan(plan: SubscriptionPlan): Result<Boolean> {
        return if (plan != SubscriptionPlan.FREE) {
            _entitlement.value = UserEntitlement(
                isProUser = true,
                activePlan = plan
            )
            Result.success(true)
        } else {
            _entitlement.value = UserEntitlement(
                isProUser = false,
                activePlan = SubscriptionPlan.FREE
            )
            Result.success(true)
        }
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
            isProUser = false,
            activePlan = SubscriptionPlan.FREE
        )
        return Result.success(true)
    }
}
