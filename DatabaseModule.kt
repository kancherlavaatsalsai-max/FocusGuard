package com.focusguard.app.di

import android.content.Context
import androidx.room.Room
import com.focusguard.app.data.db.AppDatabase
import com.focusguard.app.data.db.dao.BlockedAppDao
import com.focusguard.app.data.db.dao.ScrollBattleDao
import com.focusguard.app.data.db.dao.ScrollSessionDao
import com.focusguard.app.data.db.dao.SessionDao
import com.focusguard.app.data.db.dao.UsageRecordDao
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
    fun provideDatabase(@ApplicationContext ctx: Context): AppDatabase =
        Room.databaseBuilder(ctx, AppDatabase::class.java, "focus_guard.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun provideSessionDao(db: AppDatabase): SessionDao = db.sessionDao()
    @Provides fun provideBlockedAppDao(db: AppDatabase): BlockedAppDao = db.blockedAppDao()
    @Provides fun provideUsageRecordDao(db: AppDatabase): UsageRecordDao = db.usageRecordDao()
    @Provides fun provideScrollSessionDao(db: AppDatabase): ScrollSessionDao = db.scrollSessionDao()
    @Provides fun provideScrollBattleDao(db: AppDatabase): ScrollBattleDao = db.scrollBattleDao()
}
