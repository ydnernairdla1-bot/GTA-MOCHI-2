package com.example.data

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CareerRepository(private val dao: CareerDao) {
    constructor(context: Context) : this(NeonViceDatabase.getInstance(context).careerDao())

    fun observeCareer(): Flow<CareerProfile?> =
        dao.observeCareer().map { entity -> entity?.toDomain() }

    fun observeRecentRuns(): Flow<List<MissionRun>> =
        dao.observeRecentRuns().map { list ->
            list.map {
                MissionRun(
                    runId = it.id.toString(),
                    title = it.title,
                    outcome = it.outcome,
                    cashEarned = it.cashEarned,
                    wantedPeak = it.wantedPeak,
                    createdAtMs = it.createdAtMs,
                )
            }
        }

    suspend fun ensureProfileExists() {
        if (dao.getCareerOnce() == null) {
            dao.upsertCareer(CareerEntity())
        }
    }

    suspend fun saveCareerProgress(
        cash: Long,
        respect: Long,
        highScore: Long,
        missionsCompleted: Long,
        vehiclesStolen: Long,
        maxWantedSurvived: Long,
        unlockedWeapons: List<String>,
        equippedWeapon: String,
        ownedVehicles: List<String>,
        equippedVehicle: String,
        armorLevel: Long,
        nitroLevel: Long,
    ) {
        val current = dao.getCareerOnce() ?: CareerEntity()
        dao.upsertCareer(
            current.copy(
                cash = cash.coerceIn(0L, 999_999_999L),
                respect = respect.coerceIn(0L, 999_999_999L),
                highScore = highScore.coerceIn(0L, 999_999_999L),
                missionsCompleted = missionsCompleted.coerceIn(0L, 100_000L),
                vehiclesStolen = vehiclesStolen.coerceIn(0L, 100_000L),
                maxWantedSurvived = maxWantedSurvived.coerceIn(0L, 5L),
                unlockedWeaponsCsv = unlockedWeapons.ifEmpty { listOf(WeaponType.FISTS.id, WeaponType.PISTOL.id) }
                    .distinct()
                    .joinToString(","),
                equippedWeapon = equippedWeapon.ifBlank { WeaponType.PISTOL.id },
                ownedVehiclesCsv = ownedVehicles.ifEmpty { listOf(VehicleCatalog.SEDAN.id) }
                    .distinct()
                    .joinToString(","),
                equippedVehicle = equippedVehicle.ifBlank { VehicleCatalog.SEDAN.id },
                armorLevel = armorLevel.coerceIn(0L, 3L),
                nitroLevel = nitroLevel.coerceIn(0L, 3L),
            )
        )
    }

    suspend fun updateCallsign(callsign: String) {
        val trimmed = callsign.trim().take(30)
        require(trimmed.length >= 2) { "Callsign harus minimal 2 karakter." }
        val current = dao.getCareerOnce() ?: CareerEntity()
        dao.upsertCareer(current.copy(callsign = trimmed))
    }

    suspend fun recordMissionRun(
        title: String,
        outcome: RunOutcome,
        cashEarned: Long,
        wantedPeak: Long,
    ) {
        dao.insertRun(
            MissionRunEntity(
                title = title.trim().take(80).ifBlank { "Street Run" },
                outcome = outcome.name,
                cashEarned = cashEarned,
                wantedPeak = wantedPeak.coerceIn(0L, 5L),
                createdAtMs = System.currentTimeMillis(),
            )
        )
    }

    suspend fun resetCareer() {
        dao.upsertCareer(CareerEntity())
        dao.clearRuns()
    }

    private fun CareerEntity.toDomain(): CareerProfile = CareerProfile(
        userId = "local_player",
        callsign = callsign,
        cash = cash,
        respect = respect,
        highScore = highScore,
        missionsCompleted = missionsCompleted,
        vehiclesStolen = vehiclesStolen,
        maxWantedSurvived = maxWantedSurvived,
        unlockedWeapons = unlockedWeaponsCsv.split(",").filter { it.isNotBlank() }
            .ifEmpty { listOf(WeaponType.FISTS.id, WeaponType.PISTOL.id) },
        equippedWeapon = equippedWeapon,
        ownedVehicles = ownedVehiclesCsv.split(",").filter { it.isNotBlank() }
            .ifEmpty { listOf(VehicleCatalog.SEDAN.id) },
        equippedVehicle = equippedVehicle,
        armorLevel = armorLevel,
        nitroLevel = nitroLevel,
    )
}
