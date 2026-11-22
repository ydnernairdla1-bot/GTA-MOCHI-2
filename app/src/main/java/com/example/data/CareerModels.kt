package com.example.data

data class CareerProfile(
    val userId: String = "local_player",
    val callsign: String = "Mochi",
    val cash: Long = 500L,
    val respect: Long = 0L,
    val highScore: Long = 0L,
    val missionsCompleted: Long = 0L,
    val vehiclesStolen: Long = 0L,
    val maxWantedSurvived: Long = 0L,
    val unlockedWeapons: List<String> = listOf(WeaponType.FISTS.id, WeaponType.PISTOL.id),
    val equippedWeapon: String = WeaponType.PISTOL.id,
    val ownedVehicles: List<String> = listOf(VehicleCatalog.SEDAN.id),
    val equippedVehicle: String = VehicleCatalog.SEDAN.id,
    val armorLevel: Long = 0L,
    val nitroLevel: Long = 0L,
)

data class MissionRun(
    val runId: String = "",
    val title: String = "",
    val outcome: String = RunOutcome.PASSED.name,
    val cashEarned: Long = 0L,
    val wantedPeak: Long = 0L,
    val createdAtMs: Long = 0L,
)

enum class RunOutcome {
    PASSED,
    BUSTED,
    WASTED,
    HEIST;

    companion object {
        fun fromRaw(raw: String?): RunOutcome =
            entries.firstOrNull { it.name == raw } ?: PASSED
    }
}

enum class WeaponType(
    val id: String,
    val displayName: String,
    val description: String,
    val price: Long,
    val damage: Float,
    val fireIntervalMs: Long,
    val projectileSpeed: Float,
    val range: Float,
    val pellets: Int,
    val spreadRad: Float,
    val isMelee: Boolean,
) {
    FISTS(
        id = "FISTS",
        displayName = "Cakaran Meong",
        description = "Serangan cakar kucing jarak dekat tanpa peluru.",
        price = 0L,
        damage = 34f,
        fireIntervalMs = 340L,
        projectileSpeed = 0f,
        range = 64f,
        pellets = 0,
        spreadRad = 0f,
        isMelee = true,
    ),
    PISTOL(
        id = "PISTOL",
        displayName = "Vice 9mm",
        description = "Pistol semi-otomatis akurat untuk baku tembak cepat.",
        price = 0L,
        damage = 38f,
        fireIntervalMs = 300L,
        projectileSpeed = 980f,
        range = 460f,
        pellets = 1,
        spreadRad = 0.04f,
        isMelee = false,
    ),
    SMG(
        id = "SMG",
        displayName = "Neon Uzi",
        description = "Tembakan beruntun super cepat untuk melumpuhkan pengejar.",
        price = 1200L,
        damage = 22f,
        fireIntervalMs = 110L,
        projectileSpeed = 1040f,
        range = 420f,
        pellets = 1,
        spreadRad = 0.14f,
        isMelee = false,
    ),
    SHOTGUN(
        id = "SHOTGUN",
        displayName = "Street Sweeper",
        description = "5 proyektil sekaligus. Mematikan di jarak dekat dan ke mobil polisi.",
        price = 2600L,
        damage = 24f,
        fireIntervalMs = 680L,
        projectileSpeed = 920f,
        range = 310f,
        pellets = 5,
        spreadRad = 0.28f,
        isMelee = false,
    ),
    PLASMA(
        id = "PLASMA",
        displayName = "Cyber Rail",
        description = "Senjata berat berenergi tinggi yang meledakkan kendaraan.",
        price = 5000L,
        damage = 95f,
        fireIntervalMs = 760L,
        projectileSpeed = 1300f,
        range = 580f,
        pellets = 1,
        spreadRad = 0.01f,
        isMelee = false,
    );

    companion object {
        fun fromId(raw: String?): WeaponType =
            entries.firstOrNull { it.id == raw } ?: PISTOL
    }
}

enum class VehicleCatalog(
    val id: String,
    val displayName: String,
    val subtitle: String,
    val price: Long,
    val maxSpeed: Float,
    val acceleration: Float,
    val turnRate: Float,
    val maxHp: Float,
    val primaryColorHex: Long,
    val accentColorHex: Long,
) {
    SEDAN(
        id = "SEDAN",
        displayName = "Vice Cruiser",
        subtitle = "Sedan harian seimbang untuk jelajah kota.",
        price = 0L,
        maxSpeed = 430f,
        acceleration = 360f,
        turnRate = 3.2f,
        maxHp = 140f,
        primaryColorHex = 0xFF3E4A7A,
        accentColorHex = 0xFF00F0FF,
    ),
    TAXI(
        id = "TAXI",
        displayName = "Metro Cab",
        subtitle = "Taksi kuning lincah dengan handling tajam.",
        price = 900L,
        maxSpeed = 455f,
        acceleration = 390f,
        turnRate = 3.5f,
        maxHp = 150f,
        primaryColorHex = 0xFFFFC83D,
        accentColorHex = 0xFF1A1826,
    ),
    MUSCLE(
        id = "MUSCLE",
        displayName = "Diablo V8",
        subtitle = "Muscle car berat bertenaga besar, tahan benturan.",
        price = 2400L,
        maxSpeed = 510f,
        acceleration = 430f,
        turnRate = 3.0f,
        maxHp = 220f,
        primaryColorHex = 0xFFFF3B5C,
        accentColorHex = 0xFFFFC83D,
    ),
    SUPERCAR(
        id = "SUPERCAR",
        displayName = "Cheetah Neon",
        subtitle = "Supercar flagship berkecepatan puncak tertinggi.",
        price = 4800L,
        maxSpeed = 610f,
        acceleration = 540f,
        turnRate = 3.7f,
        maxHp = 180f,
        primaryColorHex = 0xFFFF2E93,
        accentColorHex = 0xFF00F0FF,
    );

    companion object {
        fun fromId(raw: String?): VehicleCatalog =
            entries.firstOrNull { it.id == raw } ?: SUPERCAR
    }
}
