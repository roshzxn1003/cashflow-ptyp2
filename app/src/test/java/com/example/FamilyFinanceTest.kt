package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.database.CashFlowDatabase
import com.example.data.models.*
import com.example.data.repository.CashFlowRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.junit.Assert.*

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [32])
class FamilyFinanceTest {
    private lateinit var db: CashFlowDatabase
    private lateinit var repo: CashFlowRepository

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, CashFlowDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repo = CashFlowRepository(
            db.transactionDao(),
            db.categoryDao(),
            db.budgetDao(),
            db.savingsGoalDao(),
            db.scannedItemDao(),
            db.familyDao(),
            db.familyMemberDao()
        )
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun testPersonalTransaction() = runBlocking {
        val tx = TransactionEntity(
            title = "Transport",
            amount = 500.0,
            type = TransactionType.EXPENSE,
            category = "Transport",
            financeScope = FinanceScope.PERSONAL
        )
        repo.addTransaction(tx)

        val personal = repo.getTransactionsByScope(FinanceScope.PERSONAL).first()
        assertEquals(1, personal.size)
        assertEquals(FinanceScope.PERSONAL, personal[0].financeScope)
        assertNull(personal[0].familyId)
    }

    @Test
    fun testFamilyTransaction() = runBlocking {
        val tx = TransactionEntity(
            title = "Groceries",
            amount = 850.0,
            type = TransactionType.EXPENSE,
            category = "Groceries",
            financeScope = FinanceScope.FAMILY,
            familyId = "family_123",
            createdByUserId = "user_456"
        )
        repo.addTransaction(tx)

        val family = repo.getFamilyTransactions("family_123").first()
        assertEquals(1, family.size)
        assertEquals(FinanceScope.FAMILY, family[0].financeScope)
        assertEquals("family_123", family[0].familyId)
        assertEquals("user_456", family[0].createdByUserId)
    }
}
