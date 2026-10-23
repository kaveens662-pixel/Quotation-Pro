package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.domain.model.QuotationStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface ClientDao {
    @Query("SELECT * FROM clients ORDER BY created_at DESC")
    fun getAllClients(): Flow<List<ClientEntity>>

    @Query("SELECT * FROM clients WHERE id = :id")
    fun getClientById(id: Long): Flow<ClientEntity?>

    @Query("SELECT * FROM clients WHERE id = :id")
    suspend fun getClientByIdDirect(id: Long): ClientEntity?

    @Query("SELECT * FROM clients ORDER BY created_at DESC")
    suspend fun getAllClientsDirect(): List<ClientEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(client: ClientEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(clients: List<ClientEntity>)

    @Update
    suspend fun update(client: ClientEntity)

    @Delete
    suspend fun delete(client: ClientEntity)

    @Query("DELETE FROM clients WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM clients")
    suspend fun deleteAll()
}

@Dao
interface CatalogItemDao {
    @Query("SELECT * FROM catalog_items ORDER BY name ASC")
    fun getAllItems(): Flow<List<CatalogItemEntity>>

    @Query("SELECT * FROM catalog_items WHERE is_archived = 0 ORDER BY name ASC")
    fun getActiveItems(): Flow<List<CatalogItemEntity>>

    @Query("SELECT * FROM catalog_items WHERE id = :id")
    fun getItemById(id: Long): Flow<CatalogItemEntity?>

    @Query("SELECT * FROM catalog_items WHERE id = :id")
    suspend fun getItemByIdDirect(id: Long): CatalogItemEntity?

    @Query("SELECT * FROM catalog_items ORDER BY name ASC")
    suspend fun getAllItemsDirect(): List<CatalogItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: CatalogItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<CatalogItemEntity>)

    @Update
    suspend fun update(item: CatalogItemEntity)

    @Delete
    suspend fun delete(item: CatalogItemEntity)

    @Query("DELETE FROM catalog_items")
    suspend fun deleteAll()
}

@Dao
interface QuotationDao {
    @Query("SELECT * FROM quotations ORDER BY issue_date DESC, created_at DESC")
    fun getAllQuotations(): Flow<List<QuotationEntity>>

    @Query("SELECT * FROM quotations WHERE id = :id")
    fun getQuotationById(id: Long): Flow<QuotationEntity?>

    @Query("SELECT * FROM quotations WHERE id = :id")
    suspend fun getQuotationByIdDirect(id: Long): QuotationEntity?

    @Query("SELECT * FROM quotations WHERE status = :status ORDER BY issue_date DESC")
    fun getQuotationsByStatus(status: QuotationStatus): Flow<List<QuotationEntity>>

    @Query("SELECT * FROM quotations ORDER BY issue_date DESC")
    suspend fun getAllQuotationsDirect(): List<QuotationEntity>

    @Query("SELECT quotation_number FROM quotations ORDER BY id DESC LIMIT 1")
    suspend fun getLastQuotationNumber(): String?

    @Query("SELECT COUNT(*) FROM quotations WHERE quotation_number = :number")
    suspend fun countByNumber(number: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(quotation: QuotationEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(quotations: List<QuotationEntity>)

    @Update
    suspend fun update(quotation: QuotationEntity)

    @Query("UPDATE quotations SET status = :status, modified_at = :modifiedAt WHERE id = :id")
    suspend fun updateStatus(id: Long, status: QuotationStatus, modifiedAt: Long = System.currentTimeMillis())

    @Delete
    suspend fun delete(quotation: QuotationEntity)

    @Query("DELETE FROM quotations WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM quotations")
    suspend fun deleteAll()
}

@Dao
interface QuotationItemDao {
    @Query("SELECT * FROM quotation_items WHERE quotation_id = :quotationId ORDER BY id ASC")
    fun getItemsForQuotation(quotationId: Long): Flow<List<QuotationItemEntity>>

    @Query("SELECT * FROM quotation_items WHERE quotation_id = :quotationId ORDER BY id ASC")
    suspend fun getItemsForQuotationDirect(quotationId: Long): List<QuotationItemEntity>

    @Query("SELECT * FROM quotation_items ORDER BY quotation_id ASC, id ASC")
    suspend fun getAllItemsDirect(): List<QuotationItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: QuotationItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<QuotationItemEntity>)

    @Query("DELETE FROM quotation_items WHERE quotation_id = :quotationId")
    suspend fun deleteByQuotationId(quotationId: Long)

    @Query("DELETE FROM quotation_items")
    suspend fun deleteAll()
}

@Dao
interface RecurringExpenseDao {
    @Query("SELECT * FROM recurring_expenses ORDER BY is_active DESC, name ASC")
    fun getAllExpenses(): Flow<List<RecurringExpenseEntity>>

    @Query("SELECT * FROM recurring_expenses WHERE is_active = 1 ORDER BY name ASC")
    fun getActiveExpenses(): Flow<List<RecurringExpenseEntity>>

    @Query("SELECT * FROM recurring_expenses WHERE id = :id")
    fun getExpenseById(id: Long): Flow<RecurringExpenseEntity?>

    @Query("SELECT * FROM recurring_expenses ORDER BY is_active DESC, name ASC")
    suspend fun getAllExpensesDirect(): List<RecurringExpenseEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(expense: RecurringExpenseEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(expenses: List<RecurringExpenseEntity>)

    @Update
    suspend fun update(expense: RecurringExpenseEntity)

    @Delete
    suspend fun delete(expense: RecurringExpenseEntity)

    @Query("DELETE FROM recurring_expenses WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM recurring_expenses")
    suspend fun deleteAll()
}

@Dao
interface BusinessProfileDao {
    @Query("SELECT * FROM business_profile WHERE id = 1 LIMIT 1")
    fun getProfile(): Flow<BusinessProfileEntity?>

    @Query("SELECT * FROM business_profile WHERE id = 1 LIMIT 1")
    suspend fun getProfileDirect(): BusinessProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(profile: BusinessProfileEntity)
}
