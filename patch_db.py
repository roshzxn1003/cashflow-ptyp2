import re

with open('/app/applet/app/src/main/java/com/example/data/database/CashFlowDatabase.kt', 'r') as f:
    content = f.read()

# Add migration 5 to 6
mig_str = """
val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `user_profiles` (`id` TEXT NOT NULL, `fullName` TEXT NOT NULL, `email` TEXT NOT NULL, `avatarUrl` TEXT, `serverId` TEXT, `syncStatus` TEXT NOT NULL, `updatedAt` INTEGER NOT NULL, `isDeleted` INTEGER NOT NULL, PRIMARY KEY(`id`))"
        )
    }
}
"""

content = content.replace("val MIGRATION_4_5 = object : Migration(4, 5) {", mig_str + "\nval MIGRATION_4_5 = object : Migration(4, 5) {")

# Add entity
content = content.replace("FamilyMemberEntity::class", "FamilyMemberEntity::class,\n        UserProfileEntity::class")

# Increment version
content = content.replace("version = 5", "version = 6")

# Add DAO
content = content.replace("abstract fun familyMemberDao(): FamilyMemberDao", "abstract fun familyMemberDao(): FamilyMemberDao\n    abstract fun userProfileDao(): UserProfileDao")

# Add migration to builder
content = content.replace(".addMigrations(MIGRATION_3_4, MIGRATION_4_5)", ".addMigrations(MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)")

with open('/app/applet/app/src/main/java/com/example/data/database/CashFlowDatabase.kt', 'w') as f:
    f.write(content)
