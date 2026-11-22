package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "career_profile")
data class CareerEntity(
    @PrimaryKey val id: Int = 1,
    val callsign: String = "Vercetti",
    val cash: Long = 500L,
    val respect: Long = 0L,
    val highScore: Long = 0L,
    val missionsCompleted: Long = 0L,
    val vehiclesStolen: Long = 0L,
    val maxWantedSurvived: Long = 0L,
    val unlockedWeaponsCsv: String = "FISTS,PISTOL",
    val equippedWeapon: String = WeaponType.PISTOL.id,
    val ownedVehiclesCsv: String = "SEDAN",
    val equippedVehicle: String = VehicleCatalog.SEDAN.id,
    val armorLevel: Long = 0L,
    val nitroLevel: Long = 0L,
)

@Entity(tableName = "mission_runs")
data class MissionRunEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val outcome: String,
    val cashEarned: Long,
    val wantedPeak: Long,
    val createdAtMs: Long = System.currentTimeMillis(),
)

@Dao
interface CareerDao {
    @Query("SELECT * FROM career_profile WHERE id = 1 LIMIT 1")
    fun observeCareer(): Flow<CareerEntity?>

    @Query("SELECT * FROM career_profile WHERE id = 1 LIMIT 1")
    suspend fun getCareerOnce(): CareerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCareer(entity: CareerEntity)

    @Query("SELECT * FROM mission_runs ORDER BY createdAtMs DESC LIMIT 20")
    fun observeRecentRuns(): Flow<List<MissionRunEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRun(run: MissionRunEntity)

    @Query("DELETE FROM mission_runs")
    suspend fun clearRuns()
}

@Database(
    entities = [CareerEntity::class, MissionRunEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class NeonViceDatabase : RoomDatabase() {
    abstract fun careerDao(): CareerDao

    companion object {
        @Volatile
        private var INSTANCE: NeonViceDatabase? = null

        fun getInstance(context: Context): NeonViceDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    NeonViceDatabase::class.java,
                    "neon_vice_local.db",
                ).build().also { INSTANCE = it }
            }
        }
    }
}
