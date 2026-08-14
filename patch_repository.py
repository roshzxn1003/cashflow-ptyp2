import re

with open('/app/applet/app/src/main/java/com/example/data/repository/CashFlowRepository.kt', 'r') as f:
    content = f.read()

content = content.replace(
    "transactionDao.updateTransaction(transaction)",
    "transactionDao.updateTransaction(transaction.copy(syncStatus = \"PENDING_UPDATE\", updatedAt = System.currentTimeMillis()))"
)

content = content.replace(
    "transactionDao.deleteTransaction(transaction)",
    "transactionDao.updateTransaction(transaction.copy(isDeleted = true, syncStatus = \"PENDING_DELETE\", updatedAt = System.currentTimeMillis()))"
)

# Insert already handles default value of PENDING_CREATE

with open('/app/applet/app/src/main/java/com/example/data/repository/CashFlowRepository.kt', 'w') as f:
    f.write(content)
