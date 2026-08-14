package com.example.data.network

import android.util.Log
import com.example.data.dao.TransactionDao
import com.example.data.dao.FamilyDao
import com.example.data.models.TransactionEntity
import com.example.data.models.FamilyEntity
import com.example.data.models.FinanceScope
import com.example.data.models.TransactionType
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SyncEngine(
    private val transactionDao: TransactionDao,
    private val familyDao: FamilyDao,
    private val authService: SupabaseAuthService
) {
    private val supabase = SupabaseClientConfig.supabase

    suspend fun syncTransactions() {
        val user = authService.currentUser.value ?: return
        
        withContext(Dispatchers.IO) {
            try {
                // 1. Push local changes
                pushPendingTransactions(user.id)
                
                // 2. Pull remote changes
                pullRemoteTransactions(user.id)
                
            } catch (e: Exception) {
                Log.e("SyncEngine", "Error syncing transactions", e)
            }
        }
    }

    private suspend fun pushPendingTransactions(userId: String) {
        // Pending Creates
        val creates = transactionDao.getPendingCreates()
        for (localTx in creates) {
            val dto = TransactionDto(
                id = localTx.serverId ?: java.util.UUID.randomUUID().toString(),
                userId = userId,
                familyId = localTx.familyId,
                financeScope = localTx.financeScope.name,
                amount = localTx.amount,
                transactionType = localTx.type.name,
                categoryId = null, // simplified
                description = localTx.title,
                paymentMethod = localTx.paymentMethod,
                transactionDate = java.time.Instant.ofEpochMilli(localTx.dateMillis).toString(),
                isDeleted = false
            )
            
            try {
                supabase.postgrest["transactions"].upsert(dto)
                transactionDao.updateTransaction(localTx.copy(
                    serverId = dto.id,
                    syncStatus = "SYNCED"
                ))
            } catch (e: Exception) {
                Log.e("SyncEngine", "Error pushing create", e)
            }
        }

        // Pending Updates
        val updates = transactionDao.getPendingUpdates()
        for (localTx in updates) {
            val serverId = localTx.serverId ?: continue
            val dto = TransactionDto(
                id = serverId,
                userId = userId,
                familyId = localTx.familyId,
                financeScope = localTx.financeScope.name,
                amount = localTx.amount,
                transactionType = localTx.type.name,
                categoryId = null,
                description = localTx.title,
                paymentMethod = localTx.paymentMethod,
                transactionDate = java.time.Instant.ofEpochMilli(localTx.dateMillis).toString(),
                isDeleted = localTx.isDeleted
            )
            
            try {
                supabase.postgrest["transactions"].upsert(dto)
                transactionDao.updateTransaction(localTx.copy(
                    syncStatus = "SYNCED"
                ))
            } catch (e: Exception) {
                Log.e("SyncEngine", "Error pushing update", e)
            }
        }

        // Pending Deletes
        val deletes = transactionDao.getPendingDeletes()
        for (localTx in deletes) {
            val serverId = localTx.serverId
            if (serverId != null) {
                try {
                    // Soft delete on server or hard delete, assuming soft delete with is_deleted flag
                    supabase.postgrest["transactions"].update({
                        set("is_deleted", true)
                    }) {
                        filter {
                            eq("id", serverId)
                        }
                    }
                    transactionDao.deleteTransactionById(localTx.id)
                } catch (e: Exception) {
                    Log.e("SyncEngine", "Error pushing delete", e)
                }
            } else {
                transactionDao.deleteTransactionById(localTx.id)
            }
        }
    }

    private suspend fun pullRemoteTransactions(userId: String) {
        try {
            val remoteTxs = supabase.postgrest["transactions"]
                .select(columns = Columns.ALL) {
                    filter {
                        eq("user_id", userId)
                    }
                }
                .decodeList<TransactionDto>()


            for (remoteTx in remoteTxs) {
                val localTx = transactionDao.getTransactionByServerId(remoteTx.id)
                if (localTx == null) {
                    if (!remoteTx.isDeleted) {
                        val newTx = TransactionEntity(
                            title = remoteTx.description,
                            amount = remoteTx.amount,
                            type = TransactionType.valueOf(remoteTx.transactionType),
                            category = remoteTx.categoryId ?: "Unknown",
                            paymentMethod = remoteTx.paymentMethod,
                            dateMillis = java.time.Instant.parse(remoteTx.transactionDate).toEpochMilli(),
                            financeScope = FinanceScope.valueOf(remoteTx.financeScope),
                            familyId = remoteTx.familyId,
                            createdByUserId = remoteTx.userId,
                            serverId = remoteTx.id,
                            syncStatus = "SYNCED",
                            updatedAt = System.currentTimeMillis(),
                            isDeleted = false
                        )
                        transactionDao.insertTransaction(newTx)
                    }
                } else {
                    if (remoteTx.isDeleted) {
                        transactionDao.deleteTransactionById(localTx.id)
                    } else if (localTx.syncStatus == "SYNCED") {
                        // Update local if remote is newer or just overwrite for simplicity since SYNCED means local hasn't changed
                        val updatedTx = localTx.copy(
                            title = remoteTx.description,
                            amount = remoteTx.amount,
                            type = TransactionType.valueOf(remoteTx.transactionType),
                            category = remoteTx.categoryId ?: localTx.category,
                            paymentMethod = remoteTx.paymentMethod,
                            dateMillis = java.time.Instant.parse(remoteTx.transactionDate).toEpochMilli(),
                            financeScope = FinanceScope.valueOf(remoteTx.financeScope),
                            familyId = remoteTx.familyId
                        )
                        transactionDao.updateTransaction(updatedTx)
                    }
                }
            }

        } catch (e: Exception) {
             Log.e("SyncEngine", "Error pulling transactions", e)
        }
    }
}
