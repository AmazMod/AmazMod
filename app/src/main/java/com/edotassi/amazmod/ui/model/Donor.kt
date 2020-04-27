package com.edotassi.amazmod.ui.model

data class Donor(
    var MemberId: Int = 0,
    var createdAt: String? = null,
    var type: String? = null,
    var role: String? = null,
    var isActive: Boolean = false,
    var totalAmountDonated: Float = 0f,
    var lastTransactionAt: String? = null,
    var lastTransactionAmount: Float = 0f,
    var profile: String? = null,
    var name: String? = null,
    var company: String? = null,
    var description: String? = null,
    var image: String? = null,
    var email: String? = null,
    var twitter: String? = null,
    var github: String? = null,
    var website: String? = null,
    var currency: String? = null
)