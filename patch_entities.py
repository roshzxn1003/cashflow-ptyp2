import re

with open('/app/applet/app/src/main/java/com/example/data/models/Entities.kt', 'r') as f:
    content = f.read()

# Transactions
tx_old = "    val createdByUserId: String? = null\n)"
tx_new = """    val createdByUserId: String? = null,
    val serverId: String? = null,
    val syncStatus: String = "PENDING_CREATE",
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false
)"""
content = content.replace(tx_old, tx_new)

# Categories
cat_old = "    val isDefault: Boolean = false\n)"
cat_new = """    val isDefault: Boolean = false,
    val serverId: String? = null,
    val syncStatus: String = "PENDING_CREATE",
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false
)"""
content = content.replace(cat_old, cat_new)

# Budgets
bud_old = "    val familyId: String? = null\n)"
bud_new = """    val familyId: String? = null,
    val serverId: String? = null,
    val syncStatus: String = "PENDING_CREATE",
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false
)"""
content = content.replace(bud_old, bud_new)

# SavingsGoals
sav_old = "    val familyId: String? = null\n)"
sav_new = """    val familyId: String? = null,
    val serverId: String? = null,
    val syncStatus: String = "PENDING_CREATE",
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false
)"""
content = content.replace(sav_old, sav_new)

# FamilyEntity
fam_old = "    val createdAt: Long\n)"
fam_new = """    val createdAt: Long,
    val serverId: String? = null,
    val syncStatus: String = "PENDING_CREATE",
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false
)"""
content = content.replace(fam_old, fam_new)

# FamilyMemberEntity
fammem_old = "    val joinedAt: Long\n)"
fammem_new = """    val joinedAt: Long,
    val serverId: String? = null,
    val syncStatus: String = "PENDING_CREATE",
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false
)"""
content = content.replace(fammem_old, fammem_new)

with open('/app/applet/app/src/main/java/com/example/data/models/Entities.kt', 'w') as f:
    f.write(content)
