package com.example.householdapp.core.session

import com.example.householdapp.core.model.UserProfile

data class SessionState(
    val isAuthenticated: Boolean = false,
    val user: UserProfile? = null,
    val partner: UserProfile? = null,
    val email: String = "",
    val currency: String = "USD"
)
