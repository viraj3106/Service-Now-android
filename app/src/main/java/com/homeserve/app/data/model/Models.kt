package com.homeserve.app.data.model

data class ServiceCategory(
    val id: String,
    val name: String,
    val icon: String
)

data class Review(
    val user: String,
    val rating: Double,
    val comment: String
)

data class ServiceProvider(
    val id: String,
    val name: String,
    val category: String,
    val city: String,
    val rating: Double,
    val reviewCount: Int,
    val priceRange: String,
    val availability: String,
    val photoUrl: String,
    val reviews: List<Review>
)

data class Booking(
    val id: String,
    val providerId: String,
    val service: String,
    val date: String,
    val timeSlot: String,
    val status: String,
    val notes: String
)

data class User(
    val id: String,
    val name: String,
    val phone: String,
    var subscriptionTier: String, // "free", "premium", "elite"
    var bookingsThisMonth: Int
)
