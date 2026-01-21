package com.jehan.aidkitmobile.models

import java.util.UUID

data class Medication(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val purpose: String,
    val sideEffects: List<String>? = null,
    val expiryDate: String? = null
)
