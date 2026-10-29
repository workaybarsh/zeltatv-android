package tv.zelta.app.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase

@Entity(tableName = "sources")
data class SourceEntity(
    @PrimaryKey val id: String,
    val name: String,
    val type: String,
    val value: String,
    val fileName: String,
    /** JSON array of category ids; small and only read with the source. */
    val categoryIds: String,
    val createdAt: Long,
)

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val createdAt: Long,
)

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val url: String,
    val addedAt: Long,
)

@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey val url: String,
    val lastWatchedAt: Long,
)

@Dao
interface LibraryDao {
    @Query("SELECT * FROM sources ORDER BY createdAt DESC")
    fun sources(): List<SourceEntity>

    @Query("SELECT COUNT(*) FROM sources")
    fun sourceCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertSource(source: SourceEntity)

    @Query("DELETE FROM sources WHERE id = :id")
    fun deleteSource(id: String)

    @Query("DELETE FROM sources")
    fun clearSources()

    @Query("SELECT * FROM categories ORDER BY name COLLATE NOCASE")
    fun categories(): List<CategoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertCategory(category: CategoryEntity)

    @Delete
    fun deleteCategory(category: CategoryEntity)

    @Query("SELECT * FROM favorites")
    fun favorites(): List<FavoriteEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addFavorite(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE url = :url")
    fun removeFavorite(url: String)

    @Query("SELECT * FROM history ORDER BY lastWatchedAt DESC LIMIT 20")
    fun history(): List<HistoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addHistory(history: HistoryEntity)

    @Query("DELETE FROM history WHERE url NOT IN (SELECT url FROM history ORDER BY lastWatchedAt DESC LIMIT 20)")
    fun pruneHistory()
}

@Database(
    entities = [SourceEntity::class, CategoryEntity::class, FavoriteEntity::class, HistoryEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class ZeltaDatabase : RoomDatabase() {
    abstract fun libraryDao(): LibraryDao

    companion object {
        @Volatile
        private var instance: ZeltaDatabase? = null

        fun get(context: Context): ZeltaDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    ZeltaDatabase::class.java,
                    "zeltatv-library.db",
                )
                    // M1 data is small; source editing/import is user-driven. Move DAO
                    // calls to a repository dispatcher in M2 if lists grow substantially.
                    .allowMainThreadQueries()
                    .build()
                    .also { instance = it }
            }
    }
}
