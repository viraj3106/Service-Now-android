package com.homeserve.app.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.homeserve.app.data.model.Booking
import com.homeserve.app.data.model.ServiceCategory
import com.homeserve.app.data.model.ServiceProvider
import com.homeserve.app.data.model.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

interface MockDataRepository {
    fun getCategories(): Flow<List<ServiceCategory>>
    fun getProviders(categoryId: String, city: String): Flow<List<ServiceProvider>>
    fun getProviderById(providerId: String): Flow<ServiceProvider?>
    fun getBookings(): Flow<List<Booking>>
    fun getCurrentUser(): Flow<User?>
    suspend fun login(name: String, phone: String): User
    suspend fun logout()
    suspend fun createBooking(
        providerId: String,
        service: String,
        date: String,
        timeSlot: String,
        notes: String
    ): Result<Booking>
    suspend fun updateSubscription(tier: String)
    suspend fun forceIncrementBookings() // For verification
}

class MockDataRepositoryImpl(private val context: Context) : MockDataRepository {
    private val sharedPrefs: SharedPreferences =
        context.getSharedPreferences("homeserve_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    // In-memory states backed by SharedPreferences
    private val _categories = MutableStateFlow<List<ServiceCategory>>(emptyList())
    private val _providers = MutableStateFlow<List<ServiceProvider>>(emptyList())
    private val _bookings = MutableStateFlow<List<Booking>>(emptyList())
    private val _currentUser = MutableStateFlow<User?>(null)

    init {
        loadDataFromAssets()
        loadLocalSession()
        checkAndResetMonthlyBookings()
    }

    private fun loadDataFromAssets() {
        try {
            val assetManager = context.assets
            val inputStream = assetManager.open("mock_data.json")
            val reader = InputStreamReader(inputStream)
            val rawMapType = object : TypeToken<Map<String, Any>>() {}.type
            val rawMap: Map<String, Any> = gson.fromJson(reader, rawMapType)

            // Parse categories
            val categoriesJson = gson.toJson(rawMap["serviceCategories"])
            val categoryListType = object : TypeToken<List<ServiceCategory>>() {}.type
            val categoriesList: List<ServiceCategory> = gson.fromJson(categoriesJson, categoryListType)
            _categories.value = categoriesList

            // Parse providers
            val providersJson = gson.toJson(rawMap["serviceProviders"])
            val providerListType = object : TypeToken<List<ServiceProvider>>() {}.type
            val providersList: List<ServiceProvider> = gson.fromJson(providersJson, providerListType)
            _providers.value = providersList

            reader.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadLocalSession() {
        // Load user session
        val userJson = sharedPrefs.getString("current_user", null)
        if (userJson != null) {
            val user: User = gson.fromJson(userJson, User::class.java)
            _currentUser.value = user
        }

        // Load bookings list
        val bookingsJson = sharedPrefs.getString("bookings_list", null)
        if (bookingsJson != null) {
            val bookingListType = object : TypeToken<List<Booking>>() {}.type
            val bookingsList: List<Booking> = gson.fromJson(bookingsJson, bookingListType)
            _bookings.value = bookingsList
        }
    }

    private fun checkAndResetMonthlyBookings() {
        val user = _currentUser.value ?: return
        val currentMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
        val lastReset = sharedPrefs.getString("last_reset_month", "")

        if (currentMonth != lastReset) {
            // Month has changed, reset bookings
            user.bookingsThisMonth = 0
            saveUserSession(user)
            sharedPrefs.edit().putString("last_reset_month", currentMonth).apply()
        }
    }

    private fun saveUserSession(user: User) {
        _currentUser.value = user
        sharedPrefs.edit().putString("current_user", gson.toJson(user)).apply()
    }

    private fun saveBookings(bookingsList: List<Booking>) {
        _bookings.value = bookingsList
        sharedPrefs.edit().putString("bookings_list", gson.toJson(bookingsList)).apply()
    }

    override fun getCategories(): Flow<List<ServiceCategory>> = _categories.asStateFlow()

    override fun getProviders(categoryId: String, city: String): Flow<List<ServiceProvider>> {
        val filtered = MutableStateFlow<List<ServiceProvider>>(emptyList())
        val all = _providers.value
        filtered.value = all.filter {
            it.category.lowercase() == categoryId.lowercase() && it.city.lowercase() == city.lowercase()
        }
        return filtered.asStateFlow()
    }

    override fun getProviderById(providerId: String): Flow<ServiceProvider?> {
        val flow = MutableStateFlow<ServiceProvider?>(null)
        flow.value = _providers.value.firstOrNull { it.id == providerId }
        return flow.asStateFlow()
    }

    override fun getBookings(): Flow<List<Booking>> = _bookings.asStateFlow()

    override fun getCurrentUser(): Flow<User?> = _currentUser.asStateFlow()

    override suspend fun login(name: String, phone: String): User {
        val existingUserJson = sharedPrefs.getString("current_user", null)
        val user = if (existingUserJson != null) {
            val u = gson.fromJson(existingUserJson, User::class.java)
            u.copy(name = name, phone = phone)
        } else {
            User(
                id = UUID.randomUUID().toString(),
                name = name,
                phone = phone,
                subscriptionTier = "free",
                bookingsThisMonth = 0
            )
        }
        saveUserSession(user)
        checkAndResetMonthlyBookings()
        return user
    }

    override suspend fun logout() {
        _currentUser.value = null
        _bookings.value = emptyList()
        sharedPrefs.edit()
            .remove("current_user")
            .remove("bookings_list")
            .remove("last_reset_month")
            .apply()
    }

    override suspend fun createBooking(
        providerId: String,
        service: String,
        date: String,
        timeSlot: String,
        notes: String
    ): Result<Booking> {
        val user = _currentUser.value ?: return Result.failure(Exception("User not logged in"))

        // Enforce RevenueCat subscription gating for Free tier
        if (user.subscriptionTier == "free" && user.bookingsThisMonth >= 1) {
            return Result.failure(Exception("GATED_LIMIT_EXCEEDED"))
        }

        val newBooking = Booking(
            id = UUID.randomUUID().toString(),
            providerId = providerId,
            service = service,
            date = date,
            timeSlot = timeSlot,
            status = "Upcoming",
            notes = notes
        )

        val updatedBookings = _bookings.value.toMutableList().apply {
            add(0, newBooking) // Add at top of list
        }
        saveBookings(updatedBookings)

        // Increment usage count for active month
        user.bookingsThisMonth += 1
        saveUserSession(user)

        return Result.success(newBooking)
    }

    override suspend fun updateSubscription(tier: String) {
        val user = _currentUser.value ?: return
        user.subscriptionTier = tier
        saveUserSession(user)
    }

    override suspend fun forceIncrementBookings() {
        val user = _currentUser.value ?: return
        user.bookingsThisMonth += 1
        saveUserSession(user)
    }
}
