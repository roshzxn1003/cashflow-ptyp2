import re

with open('/app/applet/app/src/main/java/com/example/data/network/SyncEngine.kt', 'r') as f:
    content = f.read()

pull_impl = """
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
"""

content = content.replace("""            for (remoteTx in remoteTxs) {
                // Find local by serverId
                // If not found, insert
                // Note: requires getTransactionByServerId in DAO. For simplicity here, we'll fetch all and filter
                // In production, add a getTransactionByServerId query.
            }""", pull_impl)

with open('/app/applet/app/src/main/java/com/example/data/network/SyncEngine.kt', 'w') as f:
    f.write(content)
