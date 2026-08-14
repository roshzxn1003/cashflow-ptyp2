package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.*
import com.example.data.models.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        val tables = listOf("transactions", "categories", "budgets", "savings_goals", "families", "family_members")
        for (table in tables) {
            db.execSQL("ALTER TABLE `$table` ADD COLUMN `serverId` TEXT")
            db.execSQL("ALTER TABLE `$table` ADD COLUMN `syncStatus` TEXT NOT NULL DEFAULT 'PENDING_CREATE'")
            db.execSQL("ALTER TABLE `$table` ADD COLUMN `updatedAt` INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE `$table` ADD COLUMN `isDeleted` INTEGER NOT NULL DEFAULT 0")
        }
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Add families table
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `families` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `createdByUserId` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`))"
        )
        // Add family_members table
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `family_members` (`id` TEXT NOT NULL, `familyId` TEXT NOT NULL, `userId` TEXT NOT NULL, `name` TEXT NOT NULL, `role` TEXT NOT NULL, `joinedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))"
        )
        
        // Alter transactions
        db.execSQL("ALTER TABLE `transactions` ADD COLUMN `financeScope` TEXT NOT NULL DEFAULT 'PERSONAL'")
        db.execSQL("ALTER TABLE `transactions` ADD COLUMN `familyId` TEXT")
        db.execSQL("ALTER TABLE `transactions` ADD COLUMN `createdByUserId` TEXT")
        
        // Alter budgets
        db.execSQL("ALTER TABLE `budgets` ADD COLUMN `financeScope` TEXT NOT NULL DEFAULT 'PERSONAL'")
        db.execSQL("ALTER TABLE `budgets` ADD COLUMN `familyId` TEXT")
        
        // Alter savings_goals
        db.execSQL("ALTER TABLE `savings_goals` ADD COLUMN `financeScope` TEXT NOT NULL DEFAULT 'PERSONAL'")
        db.execSQL("ALTER TABLE `savings_goals` ADD COLUMN `familyId` TEXT")
    }
}

@Database(
    entities = [
        TransactionEntity::class,
        CategoryEntity::class,
        BudgetEntity::class,
        SavingsGoalEntity::class,
        ScannedItemEntity::class,
        FamilyEntity::class,
        FamilyMemberEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class CashFlowDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun budgetDao(): BudgetDao
    abstract fun savingsGoalDao(): SavingsGoalDao
    abstract fun scannedItemDao(): ScannedItemDao
    abstract fun familyDao(): FamilyDao
    abstract fun familyMemberDao(): FamilyMemberDao

    companion object {
        @Volatile
        private var INSTANCE: CashFlowDatabase? = null

        fun getDatabase(context: Context): CashFlowDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CashFlowDatabase::class.java,
                    "cashflow_database"
                )
                    .addMigrations(MIGRATION_3_4, MIGRATION_4_5)
                    .addCallback(DatabaseCallback(context.applicationContext))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val context: Context
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        seedDefaultData(database)
                    }
                }
            }

            private suspend fun seedDefaultData(database: CashFlowDatabase) {
                // Default Categories
                val categories = listOf(
                    CategoryEntity(name = "Food & Dining", iconName = "Restaurant", colorHex = "#EF4444", type = TransactionType.EXPENSE, isDefault = true),
                    CategoryEntity(name = "Shopping", iconName = "ShoppingBag", colorHex = "#EC4899", type = TransactionType.EXPENSE, isDefault = true),
                    CategoryEntity(name = "Housing & Rent", iconName = "Home", colorHex = "#8B5CF6", type = TransactionType.EXPENSE, isDefault = true),
                    CategoryEntity(name = "Transportation", iconName = "DirectionsCar", colorHex = "#3B82F6", type = TransactionType.EXPENSE, isDefault = true),
                    CategoryEntity(name = "Bills & Utilities", iconName = "Receipt", colorHex = "#06B6D4", type = TransactionType.EXPENSE, isDefault = true),
                    CategoryEntity(name = "Entertainment", iconName = "Movie", colorHex = "#F59E0B", type = TransactionType.EXPENSE, isDefault = true),
                    CategoryEntity(name = "Healthcare", iconName = "MedicalServices", colorHex = "#10B981", type = TransactionType.EXPENSE, isDefault = true),
                    CategoryEntity(name = "Salary & Income", iconName = "Payments", colorHex = "#059669", type = TransactionType.INCOME, isDefault = true),
                    CategoryEntity(name = "Freelance / Business", iconName = "Work", colorHex = "#D97706", type = TransactionType.INCOME, isDefault = true),
                    CategoryEntity(name = "Investments", iconName = "TrendingUp", colorHex = "#6366F1", type = TransactionType.INCOME, isDefault = true)
                )
                database.categoryDao().insertCategories(categories)

                // Seed Initial Sample Transactions
                val now = System.currentTimeMillis()
                val day = 86400000L

                val sampleTransactions = listOf(
                    TransactionEntity(
                        title = "Monthly Salary",
                        amount = 4500.0,
                        type = TransactionType.INCOME,
                        category = "Salary & Income",
                        categoryIconName = "Payments",
                        paymentMethod = "Bank Transfer",
                        dateMillis = now - (1 * day),
                        note = "July Paycheck"
                    ),
                    TransactionEntity(
                        title = "Whole Foods Grocery",
                        amount = 142.80,
                        type = TransactionType.EXPENSE,
                        category = "Food & Dining",
                        categoryIconName = "Restaurant",
                        paymentMethod = "Credit Card",
                        dateMillis = now - (2 * day),
                        note = "Weekly groceries"
                    ),
                    TransactionEntity(
                        title = "Apartment Rent",
                        amount = 1200.0,
                        type = TransactionType.EXPENSE,
                        category = "Housing & Rent",
                        categoryIconName = "Home",
                        paymentMethod = "Bank Transfer",
                        dateMillis = now - (4 * day),
                        note = "Monthly rent payment"
                    ),
                    TransactionEntity(
                        title = "Gas Station Refill",
                        amount = 45.0,
                        type = TransactionType.EXPENSE,
                        category = "Transportation",
                        categoryIconName = "DirectionsCar",
                        paymentMethod = "UPI",
                        dateMillis = now - (5 * day),
                        note = "Fuel for sedan"
                    ),
                    TransactionEntity(
                        title = "Freelance Mobile Project",
                        amount = 850.0,
                        type = TransactionType.INCOME,
                        category = "Freelance / Business",
                        categoryIconName = "Work",
                        paymentMethod = "UPI",
                        dateMillis = now - (6 * day),
                        note = "UI Design milestone"
                    ),
                    TransactionEntity(
                        title = "Electricity & Water Bill",
                        amount = 118.50,
                        type = TransactionType.EXPENSE,
                        category = "Bills & Utilities",
                        categoryIconName = "Receipt",
                        paymentMethod = "Credit Card",
                        dateMillis = now - (8 * day),
                        note = "Utility auto-pay"
                    )
                )
                sampleTransactions.forEach { database.transactionDao().insertTransaction(it) }

                // Seed Sample Budgets
                val currentMonthYear = "2026-07"
                database.budgetDao().insertOrUpdateBudget(
                    BudgetEntity(categoryName = "Food & Dining", monthlyLimit = 500.0, monthYear = currentMonthYear)
                )
                database.budgetDao().insertOrUpdateBudget(
                    BudgetEntity(categoryName = "Shopping", monthlyLimit = 300.0, monthYear = currentMonthYear)
                )
                database.budgetDao().insertOrUpdateBudget(
                    BudgetEntity(categoryName = "Transportation", monthlyLimit = 200.0, monthYear = currentMonthYear)
                )

                // Seed Sample Savings Goal
                database.savingsGoalDao().insertOrUpdateGoal(
                    SavingsGoalEntity(
                        title = "Emergency Savings Fund",
                        targetAmount = 5000.0,
                        currentAmount = 3200.0,
                        targetDateMillis = now + (120 * day),
                        colorHex = "#059669"
                    )
                )
                database.savingsGoalDao().insertOrUpdateGoal(
                    SavingsGoalEntity(
                        title = "New Tech Laptop",
                        targetAmount = 1800.0,
                        currentAmount = 1100.0,
                        targetDateMillis = now + (60 * day),
                        colorHex = "#D97706"
                    )
                )
            }
        }
    }
}
