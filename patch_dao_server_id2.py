import re

with open('/app/applet/app/src/main/java/com/example/data/dao/CashFlowDao.kt', 'r') as f:
    content = f.read()

tx_new = """
    @Query("SELECT * FROM transactions WHERE serverId = :serverId LIMIT 1")
    suspend fun getTransactionByServerId(serverId: String): TransactionEntity?
}
"""

content = content.replace("    suspend fun getPendingDeletes(): List<TransactionEntity>", "    suspend fun getPendingDeletes(): List<TransactionEntity>\n" + tx_new.replace("}\n", ""))

with open('/app/applet/app/src/main/java/com/example/data/dao/CashFlowDao.kt', 'w') as f:
    f.write(content)
