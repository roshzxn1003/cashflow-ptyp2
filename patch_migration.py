import re

with open('/app/applet/app/src/main/java/com/example/data/database/CashFlowDatabase.kt', 'r') as f:
    content = f.read()

migration_new = """val MIGRATION_4_5 = object : Migration(4, 5) {
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
"""

content = content.replace("val MIGRATION_3_4 =", migration_new + "\nval MIGRATION_3_4 =")
content = content.replace("version = 4,", "version = 5,")
content = content.replace(".addMigrations(MIGRATION_3_4)", ".addMigrations(MIGRATION_3_4, MIGRATION_4_5)")

with open('/app/applet/app/src/main/java/com/example/data/database/CashFlowDatabase.kt', 'w') as f:
    f.write(content)
