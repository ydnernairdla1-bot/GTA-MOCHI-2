package com.example.game

import com.example.data.WeaponType

enum class VehicleKind(
    val label: String,
    val length: Float,
    val width: Float,
    val maxSpeed: Float,
    val acceleration: Float,
    val turnRate: Float,
    val maxHp: Float,
    val bodyHex: Long,
    val stripeHex: Long,
) {
    SEDAN("Vice Cruiser", 62f, 32f, 430f, 360f, 3.2f, 140f, 0xFF3E4A7A, 0xFF00F0FF),
    TAXI("Metro Cab", 60f, 32f, 455f, 390f, 3.5f, 150f, 0xFFFFC83D, 0xFF1A1826),
    MUSCLE("Diablo V8", 66f, 34f, 510f, 430f, 3.0f, 220f, 0xFFFF3B5C, 0xFFFFC83D),
    SUPERCAR("Cheetah Neon", 64f, 34f, 610f, 540f, 3.7f, 180f, 0xFFFF2E93, 0xFF00F0FF),
    POLICE("NCPD Interceptor", 64f, 33f, 535f, 460f, 3.4f, 195f, 0xFF161C33, 0xFFFFFFFF),
    SWAT("Enforcer Van", 76f, 38f, 445f, 340f, 2.5f, 340f, 0xFF1B2336, 0xFF2B7BFF),
}

enum class PedRole {
    CIVILIAN,
    SYNDICATE_GUARD,
    POLICE_OFFICER,
}

enum class PickupKind {
    CASH,
    MEDKIT,
    ARMOR,
    BRIEFCASE,
}

data class WorldVehicle(
    val id: Int,
    val kind: VehicleKind,
    val x: Float,
    val y: Float,
    val angle: Float,
    val speed: Float,
    val hp: Float,
    val isPolice: Boolean,
    val isPersonal: Boolean = false,
    val aiTargetAngle: Float = angle,
    val aiActive: Boolean = false,
    val customBodyHex: Long? = null,
    val sirenPhase: Float = 0f,
    val steerAngle: Float = 0f,
    val wheelSpin: Float = 0f,
)

data class WorldPed(
    val id: Int,
    val role: PedRole,
    val x: Float,
    val y: Float,
    val angle: Float,
    val hp: Float,
    val maxHp: Float,
    val speed: Float,
    val isMissionTarget: Boolean = false,
    val shootCooldownSec: Float = 0f,
    val panicTimerSec: Float = 0f,
    val walkPhase: Float = 0f,
)

data class Projectile(
    val id: Int,
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float,
    val damage: Float,
    val fromPlayer: Boolean,
    val lifeSec: Float,
    val colorHex: Long,
    val radius: Float = 4.5f,
)

data class ParticleEffect(
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float,
    val lifeSec: Float,
    val maxLifeSec: Float,
    val size: Float,
    val colorHex: Long,
)

data class SkidMark(
    val x1: Float,
    val y1: Float,
    val x2: Float,
    val y2: Float,
    val alpha: Float,
)

data class StreetPickup(
    val id: Int,
    val kind: PickupKind,
    val x: Float,
    val y: Float,
    val value: Long,
)

data class ActiveMissionState(
    val template: MissionTemplate,
    val stage: Int = 0, // 0 = in progress, 1 = return/escape stage for heist
    val targetsRemaining: Int = 0,
    val timerRemainingSec: Float = 0f,
    val targetX: Float = template.targetX,
    val targetY: Float = template.targetY,
    val objectiveHint: String = template.brief,
)

data class BannerMessage(
    val headline: String,
    val subline: String,
    val accentHex: Long,
    val remainingSec: Float,
    val isTerminalOverlay: Boolean = false,
)

data class PlayerInput(
    val moveX: Float = 0f,
    val moveY: Float = 0f,
    val isFiring: Boolean = false,
    val isNitroPressed: Boolean = false,
    val hijackTriggerCount: Int = 0,
)

enum class SoundEvent {
    HIJACK_ENTER,
    VEHICLE_EXIT,
    WEAPON_MELEE,
    WEAPON_PISTOL,
    WEAPON_SMG,
    WEAPON_SHOTGUN,
    WEAPON_PLASMA,
    WEAPON_SWITCH,
    HIT_IMPACT,
    VEHICLE_CRASH,
    EXPLOSION,
    PICKUP_CASH,
    PICKUP_HEALTH,
    PAY_N_SPRAY,
    MISSION_START,
    MISSION_PASSED,
    PLAYER_WASTED,
    PLAYER_BUSTED,
    CAT_MEOW,
    CAT_HISS,
}

data class GameWorldState(
    val playerX: Float = CityMap.SAFE_HOUSE_X,
    val playerY: Float = CityMap.SAFE_HOUSE_Y + 120f,
    val playerAngle: Float = -1.57f,
    val playerHp: Float = 100f,
    val playerMaxHp: Float = 100f,
    val playerArmor: Float = 0f,
    val playerMaxArmor: Float = 0f,
    val stamina: Float = 100f,
    val nitroFuel: Float = 100f,
    val inVehicleId: Int? = null,
    val nearestVehicleId: Int? = null,
    val wantedLevel: Int = 0,
    val heatPoints: Float = 0f,
    val wantedCooldownSec: Float = 0f,
    val bustedProgress: Float = 0f,
    val equippedWeapon: WeaponType = WeaponType.PISTOL,
    val fireCooldownSec: Float = 0f,
    val sessionCashEarned: Long = 0L,
    val sessionScore: Long = 0L,
    val sessionRespectEarned: Long = 0L,
    val sessionVehiclesStolen: Long = 0L,
    val sessionMaxWanted: Int = 0,
    val vehicles: List<WorldVehicle> = emptyList(),
    val peds: List<WorldPed> = emptyList(),
    val projectiles: List<Projectile> = emptyList(),
    val particles: List<ParticleEffect> = emptyList(),
    val skidMarks: List<SkidMark> = emptyList(),
    val pickups: List<StreetPickup> = emptyList(),
    val activeMission: ActiveMissionState? = null,
    val banner: BannerMessage? = null,
    val screenShake: Float = 0f,
    val clockPulse: Float = 0f,
    val currentDistrict: District = District.DOWNTOWN_NEON,
    val districtBannerTimerSec: Float = 0f,
    val isDeadOrBusted: Boolean = false,
    val isDrifting: Boolean = false,
    val playerWalkPhase: Float = 0f,
    val isPlayerMoving: Boolean = false,
    val soundEvents: List<SoundEvent> = emptyList(),
)
