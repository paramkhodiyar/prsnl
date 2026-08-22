package com.prsnl.storage.di

import android.content.Context
import androidx.room.Room
import com.prsnl.document.repository.FolderRepository
import com.prsnl.document.repository.NotebookRepository
import com.prsnl.storage.PageFileStorage
import com.prsnl.storage.PrsnlDatabase
import com.prsnl.storage.dao.FolderDao
import com.prsnl.storage.dao.NotebookDao
import com.prsnl.storage.dao.PageDao
import com.prsnl.storage.repository.FolderRepositoryImpl
import com.prsnl.storage.repository.NotebookRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun providePrsnlDatabase(@ApplicationContext context: Context): PrsnlDatabase {
        return Room.databaseBuilder(
            context,
            PrsnlDatabase::class.java,
            "prsnl_database.db"
        ).fallbackToDestructiveMigration()
        .build()
    }

    @Provides
    fun provideFolderDao(database: PrsnlDatabase): FolderDao = database.folderDao()

    @Provides
    fun provideNotebookDao(database: PrsnlDatabase): NotebookDao = database.notebookDao()

    @Provides
    fun providePageDao(database: PrsnlDatabase): PageDao = database.pageDao()

    @Provides
    @Singleton
    fun providePageFileStorage(@ApplicationContext context: Context): PageFileStorage {
        return PageFileStorage(context.filesDir)
    }

    @Provides
    @Singleton
    fun provideNotebookRepository(
        notebookDao: NotebookDao,
        pageDao: PageDao,
        fileStorage: PageFileStorage
    ): NotebookRepository {
        return NotebookRepositoryImpl(notebookDao, pageDao, fileStorage)
    }

    @Provides
    @Singleton
    fun provideFolderRepository(
        folderDao: FolderDao
    ): FolderRepository {
        return FolderRepositoryImpl(folderDao)
    }
}
