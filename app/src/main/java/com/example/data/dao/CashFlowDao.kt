package com.example.data.dao

import androidx.room.*
import com.example.data.models.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Query("SELECT * FROM transactions WHERE syncStatus = 'PENDING_CREATE'")
    suspend fun getPendingCreates(): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE syncStatus = 'PENDING_UPDATE'")
    suspend fun getPendingUpdates(): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE syncStatus = 'PENDING_DELETE'")
    suspend fun getPendingDeletes(): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE serverId = :serverId LIMIT 1")
    suspend fun getTransactionByServerId(serverId: String): TransactionEntity?

    @Query("SELECT * FROM transactions ORDER BY dateMillis DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>
    
    @Query("SELECT * FROM transactions WHERE financeScope = :scope ORDER BY dateMillis DESC")
    fun getTransactionsByScope(scope: FinanceScope): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE financeScope = :scope AND type = :type ORDER BY dateMillis DESC")
    fun getTransactionsByScopeAndType(scope: FinanceScope, type: TransactionType): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE familyId = :familyId ORDER BY dateMillis DESC")
    fun getFamilyTransactions(familyId: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE familyId = :familyId AND createdByUserId = :userId ORDER BY dateMillis DESC")
    fun getFamilyTransactionsByMember(familyId: String, userId: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE familyId = :familyId AND type = 'INCOME' ORDER BY dateMillis DESC")
    fun getFamilyIncome(familyId: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE familyId = :familyId AND type = 'EXPENSE' ORDER BY dateMillis DESC")
    fun getFamilyExpenses(familyId: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE type = :type ORDER BY dateMillis DESC")
    fun getTransactionsByType(type: TransactionType): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE category = :category ORDER BY dateMillis DESC")
    fun getTransactionsByCategory(category: String): Flow<List<TransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransactionById(id: Long)

    @Query("DELETE FROM transactions")
    suspend fun deleteAllTransactions()
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY name ASC")
    fun getAllCategories(): Flow<List<CategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCategories(categories: List<CategoryEntity>)

    @Delete
    suspend fun deleteCategory(category: CategoryEntity)
}

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budgets ORDER BY id DESC")
    fun getAllBudgets(): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets WHERE financeScope = :scope ORDER BY id DESC")
    fun getBudgetsByScope(scope: FinanceScope): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets WHERE familyId = :familyId ORDER BY id DESC")
    fun getBudgetsByFamilyId(familyId: String): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets WHERE monthYear = :monthYear")
    fun getBudgetsForMonth(monthYear: String): Flow<List<BudgetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateBudget(budget: BudgetEntity): Long

    @Delete
    suspend fun deleteBudget(budget: BudgetEntity)
}

@Dao
interface SavingsGoalDao {
    @Query("SELECT * FROM savings_goals ORDER BY targetDateMillis ASC")
    fun getAllGoals(): Flow<List<SavingsGoalEntity>>

    @Query("SELECT * FROM savings_goals WHERE financeScope = :scope ORDER BY targetDateMillis ASC")
    fun getGoalsByScope(scope: FinanceScope): Flow<List<SavingsGoalEntity>>

    @Query("SELECT * FROM savings_goals WHERE familyId = :familyId ORDER BY targetDateMillis ASC")
    fun getGoalsByFamilyId(familyId: String): Flow<List<SavingsGoalEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateGoal(goal: SavingsGoalEntity): Long

    @Delete
    suspend fun deleteGoal(goal: SavingsGoalEntity)
}

@Dao
interface ScannedItemDao {
    @Query("SELECT * FROM scanned_items ORDER BY addedDateMillis DESC")
    fun getAllScannedItems(): Flow<List<ScannedItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScannedItem(item: ScannedItemEntity): Long

    @Delete
    suspend fun deleteScannedItem(item: ScannedItemEntity)
}
