package com.focusguard.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.focusguard.app.data.db.dao.BlockedAppDao
import com.focusguard.app.data.db.dao.ScrollBattleDao
import com.focusguard.app.data.db.dao.ScrollSessionDao
import com.focusguard.app.data.db.dao.SessionDao
import com.focusguard.app.data.db.dao.UsageRecordDao
import com.focusguard.app.data.db.entity.BlockedAppEntity
import com.focusguard.app.data.db.entity.ScrollBattleEntity
import com.focusguard.app.data.db.entity.ScrollSessionEntity
import com.focusguard.app.data.db.entity.SessionEntity
import com.focusguard.app.data.db.entity.UsageRecordEntity

@Database(
    entities = [
        SessionEntity::class,
        BlockedAppEntity::class,
        UsageRecordEntity::class,
        ScrollSessionEntity::class,
        ScrollBattleEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao
    abstract fun blockedAppDao(): BlockedAppDao
    abstract fun usageRecordDao(): UsageRecordDao
    abstract fun scrollSessionDao(): ScrollSessionDao
    abstract fun scrollBattleDao(): ScrollBattleDao
}
