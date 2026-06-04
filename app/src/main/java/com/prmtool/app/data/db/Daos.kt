package com.prmtool.app.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(contact: ContactEntity)

    @Update
    suspend fun update(contact: ContactEntity)

    @Query("SELECT * FROM contacts ORDER BY createdAt DESC LIMIT 200")
    fun recent(): Flow<List<ContactEntity>>

    @Query("SELECT * FROM contacts WHERE clientId = :clientId")
    suspend fun getByClientId(clientId: String): ContactEntity?

    @Query("SELECT * FROM contacts WHERE clientId = :clientId")
    fun observe(clientId: String): Flow<ContactEntity?>

    @Query("UPDATE contacts SET status = :status WHERE clientId = :clientId")
    suspend fun updateStatus(clientId: String, status: String)

    @Query("DELETE FROM contacts WHERE clientId = :clientId")
    suspend fun delete(clientId: String)
}

@Dao
interface EventDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: EventEntity)

    @Delete
    suspend fun delete(event: EventEntity)

    @Query("SELECT * FROM events ORDER BY startMillis DESC")
    fun all(): Flow<List<EventEntity>>

    @Query("SELECT * FROM events WHERE :now BETWEEN startMillis AND endMillis")
    suspend fun activeAt(now: Long): List<EventEntity>
}

@Dao
interface SourceDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(source: SourceEntity)

    @Delete
    suspend fun delete(source: SourceEntity)

    @Query("SELECT * FROM sources ORDER BY name COLLATE NOCASE ASC")
    fun all(): Flow<List<SourceEntity>>
}
