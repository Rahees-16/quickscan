package com.rahees.quickscan.di

import android.content.Context
import androidx.room.Room
import com.rahees.quickscan.data.local.ScanDao
import com.rahees.quickscan.data.local.ScanDatabase
import com.rahees.quickscan.data.repository.ScanRepository
import com.rahees.quickscan.data.repository.ScanRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideScanDatabase(@ApplicationContext context: Context): ScanDatabase {
        return Room.databaseBuilder(
            context,
            ScanDatabase::class.java,
            "quickscan_database"
        ).build()
    }

    @Provides
    @Singleton
    fun provideScanDao(database: ScanDatabase): ScanDao {
        return database.scanDao()
    }

    @Provides
    @Singleton
    fun provideScanRepository(scanDao: ScanDao): ScanRepository {
        return ScanRepositoryImpl(scanDao)
    }
}
