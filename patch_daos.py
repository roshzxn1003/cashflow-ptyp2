import re

with open('/app/applet/app/src/main/java/com/example/data/dao/CashFlowDao.kt', 'r') as f:
    content = f.read()

# For TransactionDao
tx_new = """
    @Query("SELECT * FROM transactions WHERE syncStatus = 'PENDING_CREATE'")
    suspend fun getPendingCreates(): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE syncStatus = 'PENDING_UPDATE'")
    suspend fun getPendingUpdates(): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE syncStatus = 'PENDING_DELETE'")
    suspend fun getPendingDeletes(): List<TransactionEntity>
}
"""
content = content.replace("interface TransactionDao {\n", "interface TransactionDao {\n" + tx_new.replace("}\n", ""))

with open('/app/applet/app/src/main/java/com/example/data/dao/CashFlowDao.kt', 'w') as f:
    f.write(content)

with open('/app/applet/app/src/main/java/com/example/data/dao/FamilyDaos.kt', 'r') as f:
    family_content = f.read()

fam_new = """
    @Query("SELECT * FROM families WHERE syncStatus = 'PENDING_CREATE'")
    suspend fun getPendingCreates(): List<FamilyEntity>
    
    @Query("SELECT * FROM families WHERE syncStatus = 'PENDING_UPDATE'")
    suspend fun getPendingUpdates(): List<FamilyEntity>
"""
family_content = family_content.replace("interface FamilyDao {\n", "interface FamilyDao {\n" + fam_new)

with open('/app/applet/app/src/main/java/com/example/data/dao/FamilyDaos.kt', 'w') as f:
    f.write(family_content)
