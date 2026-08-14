package com.example.data.dao

import androidx.room.*
import com.example.data.models.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FamilyDao {

    @Query("SELECT * FROM families WHERE syncStatus = 'PENDING_CREATE'")
    suspend fun getPendingCreates(): List<FamilyEntity>
    
    @Query("SELECT * FROM families WHERE syncStatus = 'PENDING_UPDATE'")
    suspend fun getPendingUpdates(): List<FamilyEntity>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFamily(family: FamilyEntity)

    @Update
    suspend fun updateFamily(family: FamilyEntity)

    @Delete
    suspend fun deleteFamily(family: FamilyEntity)

    @Query("SELECT * FROM families WHERE id = :id")
    suspend fun getFamilyById(id: String): FamilyEntity?

    @Query("SELECT families.* FROM families INNER JOIN family_members ON families.id = family_members.familyId WHERE family_members.userId = :userId")
    fun getAllFamiliesForUser(userId: String): Flow<List<FamilyEntity>>
}

@Dao
interface FamilyMemberDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMember(member: FamilyMemberEntity)

    @Update
    suspend fun updateMember(member: FamilyMemberEntity)

    @Delete
    suspend fun deleteMember(member: FamilyMemberEntity)

    @Query("SELECT * FROM family_members WHERE familyId = :familyId")
    fun getMembersByFamilyId(familyId: String): Flow<List<FamilyMemberEntity>>

    @Query("SELECT * FROM family_members WHERE userId = :userId")
    fun getMemberByUserId(userId: String): Flow<List<FamilyMemberEntity>>

    @Query("SELECT * FROM family_members WHERE familyId = :familyId AND userId = :userId")
    suspend fun getMemberByFamilyAndUser(familyId: String, userId: String): FamilyMemberEntity?
}
