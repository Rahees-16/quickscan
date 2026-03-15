package com.rahees.quickscan.data.repository

import com.rahees.quickscan.data.local.ScanDao
import com.rahees.quickscan.data.local.ScanEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

interface ScanRepository {
    fun getAllScans(): Flow<List<ScanEntity>>
    fun searchScans(query: String): Flow<List<ScanEntity>>
    fun getByType(type: String): Flow<List<ScanEntity>>
    fun getFavorites(): Flow<List<ScanEntity>>
    suspend fun insert(scan: ScanEntity): Long
    suspend fun delete(id: Long)
    suspend fun deleteAll()
    suspend fun toggleFavorite(id: Long)
}

@Singleton
class ScanRepositoryImpl @Inject constructor(
    private val scanDao: ScanDao
) : ScanRepository {

    override fun getAllScans(): Flow<List<ScanEntity>> = scanDao.getAllScans()

    override fun searchScans(query: String): Flow<List<ScanEntity>> = scanDao.searchScans(query)

    override fun getByType(type: String): Flow<List<ScanEntity>> = scanDao.getByType(type)

    override fun getFavorites(): Flow<List<ScanEntity>> = scanDao.getFavorites()

    override suspend fun insert(scan: ScanEntity): Long = scanDao.insert(scan)

    override suspend fun delete(id: Long) = scanDao.delete(id)

    override suspend fun deleteAll() = scanDao.deleteAll()

    override suspend fun toggleFavorite(id: Long) = scanDao.toggleFavorite(id)
}
