package com.homeserve.app

import android.app.Application
import com.homeserve.app.billing.RevenueCatManager
import com.homeserve.app.data.repository.MockDataRepository
import com.homeserve.app.data.repository.MockDataRepositoryImpl

class HomeServeApplication : Application() {

    lateinit var repository: MockDataRepository
        private set

    lateinit var billingManager: RevenueCatManager
        private set

    override fun onCreate() {
        super.onCreate()
        repository = MockDataRepositoryImpl(this)
        billingManager = RevenueCatManager(this)
        billingManager.initialize(BuildConfig.REVENUECAT_API_KEY)
    }
}
