package com.example.myspacepersonalhub.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.myspacepersonalhub.core.database.converter.Converters
import com.example.myspacepersonalhub.core.database.dao.ContactDao
import com.example.myspacepersonalhub.core.database.dao.MoneyDao
import com.example.myspacepersonalhub.core.database.dao.NoteDao
import com.example.myspacepersonalhub.core.database.dao.QuickAccessDao
import com.example.myspacepersonalhub.core.database.dao.TaskDao
import com.example.myspacepersonalhub.core.database.dao.TrackerDao
import com.example.myspacepersonalhub.core.database.dao.VaultDao
import com.example.myspacepersonalhub.core.database.entity.BudgetEntity
import com.example.myspacepersonalhub.core.database.entity.ContactEntity
import com.example.myspacepersonalhub.core.database.entity.GoalEntity
import com.example.myspacepersonalhub.core.database.entity.HabitCompletionEntity
import com.example.myspacepersonalhub.core.database.entity.HabitEntity
import com.example.myspacepersonalhub.core.database.entity.NoteEntity
import com.example.myspacepersonalhub.core.database.entity.QuickAccessEntity
import com.example.myspacepersonalhub.core.database.entity.TaskEntity
import com.example.myspacepersonalhub.core.database.entity.TransactionEntity
import com.example.myspacepersonalhub.core.database.entity.VaultItemEntity

@Database(
    entities = [
        ContactEntity::class,
        NoteEntity::class,
        TaskEntity::class,
        TransactionEntity::class,
        BudgetEntity::class,
        VaultItemEntity::class,
        HabitEntity::class,
        HabitCompletionEntity::class,
        GoalEntity::class,
        QuickAccessEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class MySpaceDatabase : RoomDatabase() {
    abstract fun contactDao(): ContactDao
    abstract fun noteDao(): NoteDao
    abstract fun taskDao(): TaskDao
    abstract fun moneyDao(): MoneyDao
    abstract fun vaultDao(): VaultDao
    abstract fun trackerDao(): TrackerDao
    abstract fun quickAccessDao(): QuickAccessDao

    companion object {
        @Volatile
        private var INSTANCE: MySpaceDatabase? = null

        fun getDatabase(context: Context): MySpaceDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MySpaceDatabase::class.java,
                    "myspace_hub.db"
                )
                .fallbackToDestructiveMigration(false)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
