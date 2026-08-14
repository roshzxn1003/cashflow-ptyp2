package com.example.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class TransactionType {
    EXPENSE, INCOME
}

enum class FinanceScope {
    PERSONAL,
    FAMILY
}

enum class FamilyRole {
    ADMIN,
    MEMBER,
    VIEWER
}

@Entity(tableName = "families")
data class FamilyEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val createdByUserId: String,
    val createdAt: Long,
    val serverId: String? = null,
    val syncStatus: String = "PENDING_CREATE",
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false
)

@Entity(tableName = "family_members")
data class FamilyMemberEntity(
    @PrimaryKey
    val id: String,
    val familyId: String,
    val userId: String,
    val name: String,
    val role: FamilyRole,
    val joinedAt: Long,
    val serverId: String? = null,
    val syncStatus: String = "PENDING_CREATE",
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false
)

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val amount: Double,
    val type: TransactionType,
    val category: String,
    val categoryIconName: String = "Category",
    val note: String = "",
    val paymentMethod: String = "Cash", // Cash, Credit Card, UPI, Debit Card, Bank Transfer
    val dateMillis: Long = System.currentTimeMillis(),
    val receiptImageUri: String? = null,
    val financeScope: FinanceScope = FinanceScope.PERSONAL,
    val familyId: String? = null,
    val createdByUserId: String? = null,
    val serverId: String? = null,
    val syncStatus: String = "PENDING_CREATE",
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false
)

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val iconName: String,
    val colorHex: String,
    val type: TransactionType = TransactionType.EXPENSE,
    val isDefault: Boolean = false,
    val serverId: String? = null,
    val syncStatus: String = "PENDING_CREATE",
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false
)

@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val categoryName: String,
    val monthlyLimit: Double,
    val monthYear: String = "2026-07", // e.g. "2026-07"
    val periodType: String = "MONTHLY", // "MONTHLY", "WEEKLY", "YEARLY", "CUSTOM"
    val customPeriodName: String = "",
    val financeScope: FinanceScope = FinanceScope.PERSONAL,
    val familyId: String? = null,
    val serverId: String? = null,
    val syncStatus: String = "PENDING_CREATE",
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false
)

@Entity(tableName = "savings_goals")
data class SavingsGoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val targetAmount: Double,
    val currentAmount: Double = 0.0,
    val targetDateMillis: Long = System.currentTimeMillis() + (30L * 24 * 3600 * 1000),
    val iconName: String = "Savings",
    val colorHex: String = "#059669",
    val financeScope: FinanceScope = FinanceScope.PERSONAL,
    val familyId: String? = null,
    val serverId: String? = null,
    val syncStatus: String = "PENDING_CREATE",
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false
)

@Entity(tableName = "scanned_items")
data class ScannedItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val barcodeValue: String,
    val productName: String = "Unknown Product",
    val addedDateMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "user_profiles")
data class UserProfileEntity(
    @PrimaryKey
    val id: String,
    val fullName: String,
    val email: String,
    val avatarUrl: String? = null,
    val serverId: String? = null,
    val syncStatus: String = "SYNCED",
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false
)
