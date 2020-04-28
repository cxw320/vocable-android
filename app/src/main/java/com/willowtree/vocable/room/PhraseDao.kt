package com.willowtree.vocable.room

import androidx.room.*

@Dao
interface PhraseDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhrase(phrase: Phrase)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhrases(vararg phrases: Phrase)

    @Delete
    suspend fun deletePhrase(phrase: Phrase)

    @Update
    suspend fun updatePhrase(phrase: Phrase)
}