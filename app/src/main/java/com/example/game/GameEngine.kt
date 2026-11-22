package com.example.game

import com.example.data.CareerProfile
import com.example.data.RunOutcome
import com.example.data.VehicleCatalog
import com.example.data.WeaponType
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

data class CareerEvent(
    val cashDelta: Long = 0L,
    val respectDelta: Long = 0L,
    val scoreUpdate: Long = 0L,
    val missionsCompletedDelta: Long = 0L,
    val vehiclesStolenDelta: Long = 0L,
    val maxWantedSeen: Int = 0,
    val runLogTitle: String? = null,
    val runLogOutcome: RunOutcome? = null,
    val runLogCash: Long = 0L,
    val runLogWanted: Int = 0,
)

class GameEngine {
    private var nextId = 100
    private var lastHijackTrigger = 0

    fun createInitialState(career: CareerProfile): GameWorldState {
        nextId = 100
        lastHijackTrigger = 0
        val maxArmor = career.armorLevel.toFloat() * 35f
        val personalKind = when (VehicleCatalog.fromId(career.equippedVehicle)) {
            VehicleCatalog.SEDAN -> VehicleKind.SEDAN
            VehicleCatalog.TAXI -> VehicleKind.TAXI
            VehicleCatalog.MUSCLE -> VehicleKind.MUSCLE
            VehicleCatalog.SUPERCAR -> VehicleKind.SUPERCAR
        }

        val initialVehicles = mutableListOf<WorldVehicle>()
        // Spawn player's personal vehicle right outside Safe House!
        initialVehicles += WorldVehicle(
            id = nextId++,
            kind = personalKind,
            x = CityMap.SAFE_HOUSE_X + 70f,
            y = CityMap.SAFE_HOUSE_Y + 60f,
            angle = 0f,
            speed = 0f,
            hp = personalKind.maxHp,
            isPolice = false,
            isPersonal = true,
            aiActive = false,
        )

        // Populate ambient traffic and parked vehicles across the city
        val roadPool = listOf(VehicleKind.SEDAN, VehicleKind.TAXI, VehicleKind.MUSCLE, VehicleKind.SUPERCAR)
        for (i in 0 until 20) {
            val rx = CityMap.roadCenters[Random.nextInt(CityMap.roadCenters.size)]
            val ry = CityMap.roadCenters[Random.nextInt(CityMap.roadCenters.size)]
            val horizontal = i % 2 == 0
            val kind = if (i % 7 == 0) VehicleKind.POLICE else roadPool[i % roadPool.size]
            val angle = if (horizontal) {
                if (Random.nextBoolean()) 0f else PI.toFloat()
            } else {
                if (Random.nextBoolean()) (PI / 2).toFloat() else (-PI / 2).toFloat()
            }
            val offsetX = if (horizontal) Random.nextFloat() * 240f - 120f else if (angle > 0) -34f else 34f
            val offsetY = if (horizontal) (if (angle == 0f) 34f else -34f) else Random.nextFloat() * 240f - 120f
            initialVehicles += WorldVehicle(
                id = nextId++,
                kind = kind,
                x = (rx + offsetX).coerceIn(90f, CityMap.WORLD_SIZE - 90f),
                y = (ry + offsetY).coerceIn(90f, CityMap.WORLD_SIZE - 90f),
                angle = angle,
                speed = if (i % 4 == 0) 0f else 170f + Random.nextFloat() * 80f,
                hp = kind.maxHp,
                isPolice = kind == VehicleKind.POLICE,
                aiTargetAngle = angle,
                aiActive = i % 4 != 0,
            )
        }

        val initialPeds = mutableListOf<WorldPed>()
        for (i in 0 until 24) {
            val (px, py) = randomSidewalkSpot()
            initialPeds += WorldPed(
                id = nextId++,
                role = PedRole.CIVILIAN,
                x = px,
                y = py,
                angle = Random.nextFloat() * 6.28f,
                hp = 50f,
                maxHp = 50f,
                speed = 55f,
            )
        }

        val initialPickups = listOf(
            StreetPickup(nextId++, PickupKind.MEDKIT, CityMap.SAFE_HOUSE_X - 40f, CityMap.SAFE_HOUSE_Y + 40f, 50L),
            StreetPickup(nextId++, PickupKind.ARMOR, 1806f, 1230f, 50L),
            StreetPickup(nextId++, PickupKind.CASH, 1230f, 654f, 150L),
            StreetPickup(nextId++, PickupKind.CASH, 2382f, 1806f, 200L),
            StreetPickup(nextId++, PickupKind.MEDKIT, 654f, 2382f, 50L),
        )

        return GameWorldState(
            playerX = CityMap.SAFE_HOUSE_X,
            playerY = CityMap.SAFE_HOUSE_Y + 135f,
            playerHp = 100f,
            playerMaxHp = 100f,
            playerArmor = maxArmor,
            playerMaxArmor = maxArmor,
            equippedWeapon = WeaponType.fromId(career.equippedWeapon),
            vehicles = initialVehicles,
            peds = initialPeds,
            pickups = initialPickups,
            banner = BannerMessage(
                headline = "SELAMAT DATANG DI GTA NEON CITY",
                subline = "Versi Kucing! Naik mobil di depanmu atau datangi titik misi kuning di radar!",
                accentHex = 0xFF00F0FF,
                remainingSec = 4.2f,
            ),
        )
    }

    fun startMission(state: GameWorldState, template: MissionTemplate): GameWorldState {
        val cleanedPeds = state.peds.filter { !it.isMissionTarget }.toMutableList()
        val cleanedPickups = state.pickups.filter { it.kind != PickupKind.BRIEFCASE }.toMutableList()

        var targetX = template.targetX
        var targetY = template.targetY
        var targetsRemaining = 0
        var timerSec = 0f
        var wantedLevel = state.wantedLevel
        var heatPoints = state.heatPoints

        when (template.type) {
            MissionObjectiveType.VEHICLE_DELIVERY -> {
                // Ensure target delivery point is clear
            }
            MissionObjectiveType.ELIMINATE_SYNDICATE -> {
                targetsRemaining = 4
                val offsets = listOf(-140f to -90f, 140f to -90f, -120f to 130f, 130f to 130f)
                offsets.forEach { (ox, oy) ->
                    val (sx, sy) = CityMap.resolveCircleBuildingCollision(
                        template.targetX + ox,
                        template.targetY + oy,
                        18f,
                    )
                    cleanedPeds += WorldPed(
                        id = nextId++,
                        role = PedRole.SYNDICATE_GUARD,
                        x = sx,
                        y = sy,
                        angle = Random.nextFloat() * 6.28f,
                        hp = 95f,
                        maxHp = 95f,
                        speed = 92f,
                        isMissionTarget = true,
                    )
                }
            }
            MissionObjectiveType.SURVIVE_HEAT -> {
                timerSec = 25f
                wantedLevel = max(wantedLevel, 3)
                heatPoints = max(heatPoints, 300f)
            }
            MissionObjectiveType.HEIST_ESCAPE -> {
                cleanedPickups += StreetPickup(
                    id = nextId++,
                    kind = PickupKind.BRIEFCASE,
                    x = template.targetX,
                    y = template.targetY,
                    value = template.rewardCash,
                )
                // Guard the briefcase
                for (k in 0 until 2) {
                    val (gx, gy) = CityMap.resolveCircleBuildingCollision(
                        template.targetX + if (k == 0) -65f else 65f,
                        template.targetY + 40f,
                        18f,
                    )
                    cleanedPeds += WorldPed(
                        id = nextId++,
                        role = PedRole.SYNDICATE_GUARD,
                        x = gx,
                        y = gy,
                        angle = 1.57f,
                        hp = 90f,
                        maxHp = 90f,
                        speed = 85f,
                        isMissionTarget = false,
                    )
                }
            }
        }

        return state.copy(
            peds = cleanedPeds,
            pickups = cleanedPickups,
            wantedLevel = wantedLevel,
            heatPoints = heatPoints,
            wantedCooldownSec = if (wantedLevel > 0) 14f else 0f,
            activeMission = ActiveMissionState(
                template = template,
                stage = 0,
                targetsRemaining = targetsRemaining,
                timerRemainingSec = timerSec,
                targetX = targetX,
                targetY = targetY,
                objectiveHint = template.brief,
            ),
            banner = BannerMessage(
                headline = "MISI: ${template.title.uppercase()}",
                subline = template.brief,
                accentHex = 0xFFFFC83D,
                remainingSec = 4.0f,
            ),
            soundEvents = listOf(SoundEvent.MISSION_START, SoundEvent.CAT_MEOW),
        )
    }

    fun summonPersonalVehicle(state: GameWorldState, career: CareerProfile): GameWorldState {
        val catalog = VehicleCatalog.fromId(career.equippedVehicle)
        val kind = when (catalog) {
            VehicleCatalog.SEDAN -> VehicleKind.SEDAN
            VehicleCatalog.TAXI -> VehicleKind.TAXI
            VehicleCatalog.MUSCLE -> VehicleKind.MUSCLE
            VehicleCatalog.SUPERCAR -> VehicleKind.SUPERCAR
        }
        val spawnAngle = state.playerAngle
        val rawX = state.playerX + cos(spawnAngle) * 110f
        val rawY = state.playerY + sin(spawnAngle) * 110f
        val (sx, sy) = CityMap.resolveCircleBuildingCollision(rawX, rawY, 34f)
        val filtered = state.vehicles.filter { !it.isPersonal }.toMutableList()
        filtered += WorldVehicle(
            id = nextId++,
            kind = kind,
            x = sx,
            y = sy,
            angle = spawnAngle,
            speed = 0f,
            hp = kind.maxHp,
            isPolice = false,
            isPersonal = true,
            aiActive = false,
        )
        return state.copy(
            vehicles = filtered,
            banner = BannerMessage(
                headline = "${kind.label.uppercase()} DIKIRIM",
                subline = "Kendaraan pribadimu sudah tiba di dekatmu!",
                accentHex = 0xFF00F0FF,
                remainingSec = 2.8f,
            ),
        )
    }

    fun respawnAfterHospitalOrPrecinct(state: GameWorldState, career: CareerProfile): GameWorldState {
        val maxArmor = career.armorLevel.toFloat() * 35f
        val resetVehicles = state.vehicles.filter { !it.isPolice || hypot(it.x - CityMap.SAFE_HOUSE_X, it.y - CityMap.SAFE_HOUSE_Y) > 650f }
        return state.copy(
            playerX = CityMap.SAFE_HOUSE_X,
            playerY = CityMap.SAFE_HOUSE_Y + 135f,
            playerHp = state.playerMaxHp,
            playerArmor = maxArmor,
            stamina = 100f,
            nitroFuel = 100f,
            inVehicleId = null,
            wantedLevel = 0,
            heatPoints = 0f,
            wantedCooldownSec = 0f,
            bustedProgress = 0f,
            vehicles = resetVehicles,
            projectiles = emptyList(),
            activeMission = null,
            isDeadOrBusted = false,
            banner = BannerMessage(
                headline = "KELUAR DARI SAFE HOUSE",
                subline = "Status buronan bersih. Siap kembali ke jalanan!",
                accentHex = 0xFF2EE58B,
                remainingSec = 2.5f,
            ),
        )
    }

    fun step(
        current: GameWorldState,
        dtRaw: Float,
        input: PlayerInput,
        career: CareerProfile,
    ): Pair<GameWorldState, CareerEvent?> {
        if (current.isDeadOrBusted) {
            val updatedBanner = current.banner?.let {
                val nextT = it.remainingSec - dtRaw
                if (nextT > 0f) it.copy(remainingSec = nextT) else null
            }
            return current.copy(
                banner = updatedBanner,
                clockPulse = current.clockPulse + dtRaw,
                isDrifting = false,
                soundEvents = emptyList(),
            ) to null
        }

        val dt = dtRaw.coerceIn(0.001f, 0.05f)
        var event: CareerEvent? = null
        val soundEvents = mutableListOf<SoundEvent>()
        var isDrifting = false
        var isPlayerMoving = false
        var playerWalkPhase = current.playerWalkPhase

        var px = current.playerX
        var py = current.playerY
        var pAngle = current.playerAngle
        var hp = current.playerHp
        var armor = current.playerArmor
        var stamina = current.stamina
        var nitro = current.nitroFuel
        var inVehicleId = current.inVehicleId
        var wantedLevel = current.wantedLevel
        var heatPoints = current.heatPoints
        var wantedCooldown = current.wantedCooldownSec
        var bustedProgress = current.bustedProgress
        var fireCooldown = max(0f, current.fireCooldownSec - dt)
        var shake = max(0f, current.screenShake - dt * 24f)

        var sessionCash = current.sessionCashEarned
        var sessionScore = current.sessionScore
        var sessionRespect = current.sessionRespectEarned
        var sessionStolen = current.sessionVehiclesStolen
        var sessionMaxWanted = current.sessionMaxWanted

        val vehicles = current.vehicles.toMutableList()
        val peds = current.peds.toMutableList()
        val projectiles = current.projectiles.toMutableList()
        val particles = current.particles.mapNotNull {
            val rem = it.lifeSec - dt
            if (rem <= 0f) null
            else it.copy(x = it.x + it.vx * dt, y = it.y + it.vy * dt, lifeSec = rem)
        }.toMutableList()
        val skids = current.skidMarks.mapNotNull {
            val a = it.alpha - dt * 0.14f
            if (a <= 0.02f) null else it.copy(alpha = a)
        }.toMutableList()
        val pickups = current.pickups.toMutableList()

        var banner = current.banner?.let {
            val rem = it.remainingSec - dt
            if (rem > 0f) it.copy(remainingSec = rem) else null
        }

        // 1. Handle Hijack / Enter / Exit Trigger
        val hijackPressed = input.hijackTriggerCount != lastHijackTrigger
        lastHijackTrigger = input.hijackTriggerCount
        if (hijackPressed) {
            if (inVehicleId != null) {
                val idx = vehicles.indexOfFirst { it.id == inVehicleId }
                if (idx >= 0) {
                    val v = vehicles[idx]
                    val exitAngle = v.angle - (PI / 2f).toFloat()
                    val (ex, ey) = CityMap.resolveCircleBuildingCollision(
                        v.x + cos(exitAngle) * 44f,
                        v.y + sin(exitAngle) * 44f,
                        16f,
                    )
                    px = ex
                    py = ey
                    vehicles[idx] = v.copy(speed = v.speed * 0.45f, aiActive = false)
                }
                inVehicleId = null
                soundEvents += SoundEvent.VEHICLE_EXIT
            } else {
                val nearest = vehicles.minByOrNull { hypot(it.x - px, it.y - py) }
                if (nearest != null && hypot(nearest.x - px, nearest.y - py) <= 88f) {
                    val idx = vehicles.indexOfFirst { it.id == nearest.id }
                    inVehicleId = nearest.id
                    soundEvents += SoundEvent.HIJACK_ENTER
                    if (!nearest.isPersonal && nearest.aiActive) {
                        sessionStolen += 1L
                        sessionScore += 50L
                        event = mergeEvent(event, CareerEvent(vehiclesStolenDelta = 1L, scoreUpdate = sessionScore))
                        // Eject driver as panicked civilian
                        peds += WorldPed(
                            id = nextId++,
                            role = PedRole.CIVILIAN,
                            x = nearest.x + 36f,
                            y = nearest.y + 36f,
                            angle = nearest.angle + 1.5f,
                            hp = 40f,
                            maxHp = 40f,
                            speed = 90f,
                            panicTimerSec = 6f,
                        )
                        heatPoints += if (nearest.isPolice) 95f else 42f
                        wantedCooldown = 14f
                    } else if (nearest.isPolice) {
                        heatPoints += 80f
                        wantedCooldown = 14f
                    }
                    vehicles[idx] = nearest.copy(aiActive = false)
                    px = nearest.x
                    py = nearest.y
                    pAngle = nearest.angle
                }
            }
        }

        // 2. Player On-Foot vs Vehicle Physics
        val stickMag = hypot(input.moveX, input.moveY).coerceAtMost(1f)
        if (inVehicleId != null) {
            val vIdx = vehicles.indexOfFirst { it.id == inVehicleId }
            if (vIdx >= 0) {
                var v = vehicles[vIdx]
                val nitroMultiplier = 1f + career.nitroLevel * 0.14f
                val isBoosting = input.isNitroPressed && nitro > 2f && stickMag > 0.1f
                if (isBoosting) {
                    nitro = max(0f, nitro - dt * 28f)
                    particles += ParticleEffect(
                        x = v.x - cos(v.angle) * (v.kind.length * 0.52f),
                        y = v.y - sin(v.angle) * (v.kind.length * 0.52f),
                        vx = -cos(v.angle) * 140f + (Random.nextFloat() - 0.5f) * 50f,
                        vy = -sin(v.angle) * 140f + (Random.nextFloat() - 0.5f) * 50f,
                        lifeSec = 0.25f,
                        maxLifeSec = 0.25f,
                        size = 8f,
                        colorHex = 0xFF00F0FF,
                    )
                } else {
                    nitro = min(100f, nitro + dt * 10f)
                }

                val targetMaxSpeed = v.kind.maxSpeed * (if (isBoosting) 1.38f * nitroMultiplier else 1f)
                var speed = v.speed
                var angle = v.angle
                var steerAngle = 0f

                if (stickMag > 0.12f) {
                    val desiredAngle = atan2(input.moveY, input.moveX)
                    val angleDiff = normalizeAngle(desiredAngle - angle)
                    val turnAmount = v.kind.turnRate * dt * (0.45f + 0.55f * (abs(speed) / v.kind.maxSpeed))
                    val deltaYaw = angleDiff.coerceIn(-turnAmount, turnAmount)
                    angle = normalizeAngle(angle + deltaYaw)
                    steerAngle = angleDiff.coerceIn(-0.48f, 0.48f)
                    // Drift smoke & skid marks on sharp turns at high speed
                    if (abs(angleDiff) > 0.45f && abs(speed) > 220f) {
                        isDrifting = true
                        val backX = v.x - cos(angle) * 24f
                        val backY = v.y - sin(angle) * 24f
                        skids += SkidMark(backX, backY, v.x, v.y, 0.55f)
                        particles += ParticleEffect(
                            x = backX,
                            y = backY,
                            vx = (Random.nextFloat() - 0.5f) * 30f,
                            vy = (Random.nextFloat() - 0.5f) * 30f,
                            lifeSec = 0.35f,
                            maxLifeSec = 0.35f,
                            size = 7f,
                            colorHex = 0xFF9E9BB8,
                        )
                    }
                    speed = min(targetMaxSpeed, speed + v.kind.acceleration * stickMag * (if (isBoosting) 1.6f else 1f) * dt)
                } else {
                    speed *= (1f - 2.6f * dt).coerceAtLeast(0f)
                    if (abs(speed) < 8f) speed = 0f
                }

                val nextRawX = v.x + cos(angle) * speed * dt
                val nextRawY = v.y + sin(angle) * speed * dt
                val (resolvedX, resolvedY) = CityMap.resolveCircleBuildingCollision(nextRawX, nextRawY, v.kind.width * 0.55f)
                if (hypot(resolvedX - nextRawX, resolvedY - nextRawY) > 1.5f && abs(speed) > 140f) {
                    speed *= 0.45f
                    shake = max(shake, 4f)
                    soundEvents += SoundEvent.VEHICLE_CRASH
                }

                v = v.copy(
                    x = resolvedX,
                    y = resolvedY,
                    angle = angle,
                    speed = speed,
                    sirenPhase = if (v.isPolice) v.sirenPhase + dt * 8f else 0f,
                    steerAngle = steerAngle,
                    wheelSpin = v.wheelSpin + (speed * dt) / 5.4f,
                )
                vehicles[vIdx] = v
                px = v.x
                py = v.y
                pAngle = v.angle
            } else {
                inVehicleId = null
            }
        } else {
            nitro = min(100f, nitro + dt * 12f)
            val isSprinting = input.isNitroPressed && stamina > 3f && stickMag > 0.1f
            if (isSprinting) {
                stamina = max(0f, stamina - dt * 26f)
            } else {
                stamina = min(100f, stamina + dt * 20f)
            }
            if (stickMag > 0.1f) {
                pAngle = atan2(input.moveY, input.moveX)
                val walkSpeed = if (isSprinting) 255f else 178f
                val (nx, ny) = CityMap.resolveCircleBuildingCollision(
                    px + cos(pAngle) * walkSpeed * stickMag * dt,
                    py + sin(pAngle) * walkSpeed * stickMag * dt,
                    15f,
                )
                val movedDist = hypot(nx - px, ny - py)
                px = nx
                py = ny
                if (movedDist > 0.05f) {
                    isPlayerMoving = true
                    val walkRate = if (isSprinting) 13.5f else 9.0f
                    playerWalkPhase = (playerWalkPhase + dt * walkRate) % (2f * PI.toFloat())
                } else {
                    playerWalkPhase = 0f
                }
            } else {
                playerWalkPhase = 0f
            }
        }
        if (inVehicleId != null) {
            isPlayerMoving = false
            playerWalkPhase = 0f
        }

        // 3. Player Firing / Drive-by / Melee
        if (input.isFiring && fireCooldown <= 0f) {
            val weapon = current.equippedWeapon
            fireCooldown = weapon.fireIntervalMs / 1000f

            // Subtle auto-aim assist toward closest hostile ped or police car within 34 deg cone
            val aimAngle = findAssistedAimAngle(px, py, pAngle, peds, vehicles, inVehicleId)

            if (weapon.isMelee && inVehicleId == null) {
                soundEvents += SoundEvent.WEAPON_MELEE
                soundEvents += SoundEvent.CAT_MEOW
                // Claw slash nearby peds
                val hitCenterX = px + cos(aimAngle) * 34f
                val hitCenterY = py + sin(aimAngle) * 34f
                for (i in peds.indices) {
                    val p = peds[i]
                    if (hypot(p.x - hitCenterX, p.y - hitCenterY) <= 44f) {
                        peds[i] = p.copy(hp = p.hp - weapon.damage, panicTimerSec = 5f)
                        particles += sparkBurst(p.x, p.y, 0xFFFFC83D, 5)
                        soundEvents += SoundEvent.HIT_IMPACT
                        soundEvents += SoundEvent.CAT_HISS
                        heatPoints += 18f
                        wantedCooldown = 12f
                    }
                }
            } else {
                val effectiveWeapon = if (inVehicleId != null && weapon == WeaponType.FISTS) WeaponType.PISTOL else weapon
                soundEvents += when (effectiveWeapon) {
                    WeaponType.FISTS -> SoundEvent.WEAPON_MELEE
                    WeaponType.PISTOL -> SoundEvent.WEAPON_PISTOL
                    WeaponType.SMG -> SoundEvent.WEAPON_SMG
                    WeaponType.SHOTGUN -> SoundEvent.WEAPON_SHOTGUN
                    WeaponType.PLASMA -> SoundEvent.WEAPON_PLASMA
                }
                val pellets = max(1, effectiveWeapon.pellets)
                val muzzleX = px + cos(aimAngle) * 26f
                val muzzleY = py + sin(aimAngle) * 26f
                repeat(pellets) {
                    val spread = (Random.nextFloat() - 0.5f) * effectiveWeapon.spreadRad
                    val shotAngle = aimAngle + spread
                    projectiles += Projectile(
                        id = nextId++,
                        x = muzzleX,
                        y = muzzleY,
                        vx = cos(shotAngle) * effectiveWeapon.projectileSpeed,
                        vy = sin(shotAngle) * effectiveWeapon.projectileSpeed,
                        damage = effectiveWeapon.damage,
                        fromPlayer = true,
                        lifeSec = effectiveWeapon.range / effectiveWeapon.projectileSpeed,
                        colorHex = if (effectiveWeapon == WeaponType.PLASMA) 0xFF00F0FF else 0xFFFFC83D,
                        radius = if (effectiveWeapon == WeaponType.PLASMA) 7f else 4.5f,
                    )
                }
                particles += sparkBurst(muzzleX, muzzleY, 0xFFFFC83D, 4)
                heatPoints += if (effectiveWeapon == WeaponType.PLASMA) 35f else 12f
                wantedCooldown = 14f
            }
        }

        // 4. Update Wanted Stars & Spawn Police
        val computedStars = when {
            heatPoints >= 450f -> 5
            heatPoints >= 310f -> 4
            heatPoints >= 190f -> 3
            heatPoints >= 95f -> 2
            heatPoints >= 35f -> 1
            else -> 0
        }
        if (computedStars > wantedLevel) {
            wantedLevel = computedStars
        }
        if (wantedLevel > sessionMaxWanted) {
            sessionMaxWanted = wantedLevel
        }

        if (wantedLevel > 0) {
            val activePoliceCount = vehicles.count { it.isPolice && it.id != inVehicleId } +
                peds.count { it.role == PedRole.POLICE_OFFICER }
            val desiredPoliceVehicles = min(4, wantedLevel)
            if (vehicles.count { it.isPolice && it.id != inVehicleId } < desiredPoliceVehicles) {
                val (sx, sy) = spawnOnNearbyRoad(px, py, 560f, 920f)
                val kind = if (wantedLevel >= 4 && Random.nextBoolean()) VehicleKind.SWAT else VehicleKind.POLICE
                vehicles += WorldVehicle(
                    id = nextId++,
                    kind = kind,
                    x = sx,
                    y = sy,
                    angle = atan2(py - sy, px - sx),
                    speed = 260f,
                    hp = kind.maxHp,
                    isPolice = true,
                    aiActive = true,
                )
            }
            if (peds.count { it.role == PedRole.POLICE_OFFICER } < wantedLevel + 1 && activePoliceCount < 10) {
                val (ox, oy) = spawnOnNearbyRoad(px, py, 340f, 640f)
                peds += WorldPed(
                    id = nextId++,
                    role = PedRole.POLICE_OFFICER,
                    x = ox,
                    y = oy,
                    angle = atan2(py - oy, px - ox),
                    hp = 75f + wantedLevel * 12f,
                    maxHp = 75f + wantedLevel * 12f,
                    speed = 128f,
                )
            }

            val nearestCopDist = min(
                vehicles.filter { it.isPolice && it.id != inVehicleId }
                    .minOfOrNull { hypot(it.x - px, it.y - py) } ?: 9999f,
                peds.filter { it.role == PedRole.POLICE_OFFICER }
                    .minOfOrNull { hypot(it.x - px, it.y - py) } ?: 9999f,
            )
            if (nearestCopDist > 420f) {
                wantedCooldown -= dt
                if (wantedCooldown <= 0f) {
                    wantedLevel = 0
                    heatPoints = 0f
                    bustedProgress = 0f
                    banner = BannerMessage(
                        headline = "LOLOS DARI POLISI!",
                        subline = "Bintang buronan berhasil dihilangkan.",
                        accentHex = 0xFF2EE58B,
                        remainingSec = 2.6f,
                    )
                }
            } else {
                wantedCooldown = 10f + wantedLevel * 2f
            }

            // Busted meter if cornered slowly by police
            val playerSpeed = if (inVehicleId != null) {
                abs(vehicles.firstOrNull { it.id == inVehicleId }?.speed ?: 0f)
            } else {
                stickMag * 180f
            }
            if (nearestCopDist < 68f && playerSpeed < 45f) {
                bustedProgress += dt * 0.42f
            } else {
                bustedProgress = max(0f, bustedProgress - dt * 0.55f)
            }
        } else {
            bustedProgress = 0f
        }

        // 5. Update AI Vehicles (Traffic + Police Pursuit) & Ramming Collisions
        for (i in vehicles.indices) {
            var v = vehicles[i]
            if (v.id == inVehicleId) continue
            if (v.isPolice) {
                v = v.copy(sirenPhase = v.sirenPhase + dt * 10f)
            }
            if (v.aiActive) {
                if (v.isPolice && wantedLevel > 0) {
                    val toPlayerAngle = atan2(py - v.y, px - v.x)
                    val diff = normalizeAngle(toPlayerAngle - v.angle)
                    val newAngle = normalizeAngle(v.angle + diff.coerceIn(-v.kind.turnRate * dt, v.kind.turnRate * dt))
                    val targetSpd = v.kind.maxSpeed * (0.76f + wantedLevel * 0.05f)
                    val spd = min(targetSpd, v.speed + v.kind.acceleration * dt)
                    val (nx, ny) = CityMap.resolveCircleBuildingCollision(
                        v.x + cos(newAngle) * spd * dt,
                        v.y + sin(newAngle) * spd * dt,
                        v.kind.width * 0.55f,
                    )
                    v = v.copy(
                        x = nx,
                        y = ny,
                        angle = newAngle,
                        speed = spd,
                        steerAngle = diff.coerceIn(-0.42f, 0.42f),
                        wheelSpin = v.wheelSpin + (spd * dt) / 5.4f,
                    )
                } else {
                    val spd = v.speed.coerceAtLeast(150f)
                    val nx = v.x + cos(v.angle) * spd * dt
                    val ny = v.y + sin(v.angle) * spd * dt
                    val hitWall = nx < 70f || nx > CityMap.WORLD_SIZE - 70f || ny < 70f || ny > CityMap.WORLD_SIZE - 70f
                    val (rx, ry) = CityMap.resolveCircleBuildingCollision(nx, ny, v.kind.width * 0.52f)
                    val bounced = hitWall || hypot(rx - nx, ry - ny) > 2f
                    val nextAngle = if (bounced) normalizeAngle(v.angle + (PI / 2f).toFloat()) else v.angle
                    v = v.copy(
                        x = rx,
                        y = ry,
                        angle = nextAngle,
                        speed = spd,
                        steerAngle = 0f,
                        wheelSpin = v.wheelSpin + (spd * dt) / 5.4f,
                    )
                }
                vehicles[i] = v
            }

            // Player vehicle ramming NPC vehicles
            if (inVehicleId != null) {
                val pvIdx = vehicles.indexOfFirst { it.id == inVehicleId }
                if (pvIdx >= 0 && pvIdx != i) {
                    val pv = vehicles[pvIdx]
                    val dist = hypot(v.x - pv.x, v.y - pv.y)
                    if (dist < 48f && dist > 0.1f) {
                        val pushX = (v.x - pv.x) / dist
                        val pushY = (v.y - pv.y) / dist
                        val impact = abs(pv.speed) * 0.12f
                        vehicles[i] = v.copy(
                            x = v.x + pushX * 18f,
                            y = v.y + pushY * 18f,
                            hp = v.hp - impact,
                        )
                        vehicles[pvIdx] = pv.copy(speed = pv.speed * 0.72f)
                        if (impact > 18f) {
                            particles += sparkBurst((v.x + pv.x) / 2f, (v.y + pv.y) / 2f, 0xFFFFC83D, 6)
                            shake = max(shake, 3.5f)
                            soundEvents += SoundEvent.VEHICLE_CRASH
                            if (v.isPolice) heatPoints += 25f
                        }
                    }
                }
            }
        }

        // Remove exploded vehicles & award chaos points
        val playerVehicleRef = vehicles.firstOrNull { it.id == inVehicleId }
        val explodedVehicles = vehicles.filter { it.hp <= 0f }
        if (explodedVehicles.isNotEmpty()) {
            soundEvents += SoundEvent.EXPLOSION
            explodedVehicles.forEach { ev ->
                particles += sparkBurst(ev.x, ev.y, 0xFFFF3B5C, 22)
                particles += sparkBurst(ev.x, ev.y, 0xFFFFC83D, 14)
                shake = max(shake, 11f)
                if (ev.id == inVehicleId) {
                    inVehicleId = null
                    hp -= 45f
                } else {
                    sessionCash += 120L
                    sessionScore += 250L
                    heatPoints += 75f
                    wantedCooldown = 14f
                    event = mergeEvent(event, CareerEvent(cashDelta = 120L, scoreUpdate = sessionScore))
                }
            }
            vehicles.removeAll { it.hp <= 0f }
        }

        // 6. Update Pedestrians (Civilians, Syndicate, Police Officers)
        val playerSpeedForRam = abs(playerVehicleRef?.speed ?: 0f)
        for (i in peds.indices) {
            var p = peds[i]
            val distToPlayer = hypot(px - p.x, py - p.y)
            // Ramming ped with player car!
            if (inVehicleId != null && distToPlayer < 34f && playerSpeedForRam > 90f) {
                p = p.copy(hp = 0f)
                particles += sparkBurst(p.x, p.y, 0xFFFF2E93, 8)
                heatPoints += if (p.role == PedRole.POLICE_OFFICER) 55f else 28f
                wantedCooldown = 14f
                peds[i] = p
                continue
            }

            val nextShootCd = max(0f, p.shootCooldownSec - dt)
            when (p.role) {
                PedRole.CIVILIAN -> {
                    val panic = max(0f, p.panicTimerSec - dt)
                    val moveSpd = if (panic > 0f || wantedLevel > 0) 110f else p.speed
                    val moveAng = if (panic > 0f && distToPlayer < 260f) atan2(p.y - py, p.x - px) else p.angle
                    val (nx, ny) = CityMap.resolveCircleBuildingCollision(
                        p.x + cos(moveAng) * moveSpd * dt,
                        p.y + sin(moveAng) * moveSpd * dt,
                        12f,
                    )
                    val movedDist = hypot(nx - p.x, ny - p.y)
                    val newAng = if (movedDist < 0.3f) normalizeAngle(moveAng + 1.8f) else moveAng
                    val nextPhase = if (movedDist > 0.05f) (p.walkPhase + dt * 7f) % (2f * PI.toFloat()) else 0f
                    peds[i] = p.copy(x = nx, y = ny, angle = newAng, panicTimerSec = panic, walkPhase = nextPhase)
                }
                PedRole.SYNDICATE_GUARD, PedRole.POLICE_OFFICER -> {
                    val shouldEngage = p.role == PedRole.SYNDICATE_GUARD || wantedLevel > 0
                    val toPlayer = atan2(py - p.y, px - p.x)
                    var nx = p.x
                    var ny = p.y
                    if (shouldEngage && distToPlayer > 135f && distToPlayer < 750f) {
                        val (rx, ry) = CityMap.resolveCircleBuildingCollision(
                            p.x + cos(toPlayer) * p.speed * dt,
                            p.y + sin(toPlayer) * p.speed * dt,
                            13f,
                        )
                        nx = rx
                        ny = ry
                    }
                    val movedDist = hypot(nx - p.x, ny - p.y)
                    val nextPhase = if (movedDist > 0.05f) (p.walkPhase + dt * 8f) % (2f * PI.toFloat()) else 0f
                    var updatedCd = nextShootCd
                    if (shouldEngage && distToPlayer < 360f && updatedCd <= 0f) {
                        updatedCd = if (p.role == PedRole.POLICE_OFFICER) 0.9f else 1.05f
                        val spread = (Random.nextFloat() - 0.5f) * 0.16f
                        val sAng = toPlayer + spread
                        projectiles += Projectile(
                            id = nextId++,
                            x = nx + cos(sAng) * 18f,
                            y = ny + sin(sAng) * 18f,
                            vx = cos(sAng) * 680f,
                            vy = sin(sAng) * 680f,
                            damage = if (p.role == PedRole.POLICE_OFFICER) 9f else 11f,
                            fromPlayer = false,
                            lifeSec = 0.65f,
                            colorHex = if (p.role == PedRole.POLICE_OFFICER) 0xFF2B7BFF else 0xFFFF3B5C,
                        )
                    }
                    peds[i] = p.copy(
                        x = nx,
                        y = ny,
                        angle = toPlayer,
                        shootCooldownSec = updatedCd,
                        walkPhase = nextPhase,
                    )
                }
            }
        }

        // Handle defeated peds -> drop cash & progress elimination missions
        var defeatedMissionTargets = 0
        val deadPeds = peds.filter { it.hp <= 0f }
        if (deadPeds.isNotEmpty()) {
            deadPeds.forEach { dp ->
                if (dp.isMissionTarget) defeatedMissionTargets++
                val dropCash = when (dp.role) {
                    PedRole.CIVILIAN -> 35L
                    PedRole.POLICE_OFFICER -> 75L
                    PedRole.SYNDICATE_GUARD -> 160L
                }
                pickups += StreetPickup(nextId++, PickupKind.CASH, dp.x, dp.y, dropCash)
                sessionScore += if (dp.role == PedRole.CIVILIAN) 25L else 120L
                event = mergeEvent(event, CareerEvent(scoreUpdate = sessionScore))
            }
            peds.removeAll { it.hp <= 0f }
        }
        while (peds.count { it.role == PedRole.CIVILIAN } < 20) {
            val (cx, cy) = randomSidewalkSpot()
            peds += WorldPed(
                id = nextId++,
                role = PedRole.CIVILIAN,
                x = cx,
                y = cy,
                angle = Random.nextFloat() * 6.28f,
                hp = 50f,
                maxHp = 50f,
                speed = 55f,
            )
        }

        // 7. Update Projectiles & Hit Detection
        val remainingProjectiles = mutableListOf<Projectile>()
        for (proj in projectiles) {
            val nextLife = proj.lifeSec - dt
            if (nextLife <= 0f) continue
            val nx = proj.x + proj.vx * dt
            val ny = proj.y + proj.vy * dt

            // Building hit
            val hitBuilding = CityMap.buildings.any { it.rect.contains(androidx.compose.ui.geometry.Offset(nx, ny)) }
            if (hitBuilding) {
                particles += sparkBurst(nx, ny, proj.colorHex, 3)
                continue
            }

            var consumed = false
            if (proj.fromPlayer) {
                for (i in peds.indices) {
                    val p = peds[i]
                    if (hypot(p.x - nx, p.y - ny) < 18f) {
                        peds[i] = p.copy(hp = p.hp - proj.damage, panicTimerSec = 5f)
                        particles += sparkBurst(nx, ny, 0xFFFF2E93, 5)
                        soundEvents += SoundEvent.HIT_IMPACT
                        heatPoints += if (p.role == PedRole.POLICE_OFFICER) 45f else 22f
                        wantedCooldown = 14f
                        consumed = true
                        break
                    }
                }
                if (!consumed) {
                    for (i in vehicles.indices) {
                        val v = vehicles[i]
                        if (v.id == inVehicleId) continue
                        if (hypot(v.x - nx, v.y - ny) < 32f) {
                            vehicles[i] = v.copy(hp = v.hp - proj.damage * 0.85f)
                            particles += sparkBurst(nx, ny, 0xFFFFC83D, 4)
                            soundEvents += SoundEvent.HIT_IMPACT
                            if (v.isPolice) heatPoints += 22f
                            consumed = true
                            break
                        }
                    }
                }
            } else {
                if (hypot(px - nx, py - ny) < (if (inVehicleId != null) 28f else 16f)) {
                    if (inVehicleId != null) {
                        val vIdx = vehicles.indexOfFirst { it.id == inVehicleId }
                        if (vIdx >= 0) {
                            vehicles[vIdx] = vehicles[vIdx].copy(hp = vehicles[vIdx].hp - proj.damage * 0.65f)
                        }
                    } else {
                        val dmg = proj.damage
                        if (armor > 0f) {
                            val absorbed = min(armor, dmg)
                            armor -= absorbed
                            hp -= (dmg - absorbed)
                        } else {
                            hp -= dmg
                        }
                    }
                    particles += sparkBurst(nx, ny, 0xFFFF3B5C, 4)
                    consumed = true
                }
            }

            if (!consumed) {
                remainingProjectiles += proj.copy(x = nx, y = ny, lifeSec = nextLife)
            }
        }

        // 8. Pickups & Pay 'N' Spray Garages
        var collectedBriefcase = false
        val keptPickups = mutableListOf<StreetPickup>()
        for (pk in pickups) {
            if (hypot(pk.x - px, pk.y - py) < 38f) {
                when (pk.kind) {
                    PickupKind.CASH -> {
                        sessionCash += pk.value
                        sessionScore += pk.value
                        event = mergeEvent(event, CareerEvent(cashDelta = pk.value, scoreUpdate = sessionScore))
                        particles += sparkBurst(pk.x, pk.y, 0xFF2EE58B, 8)
                        soundEvents += SoundEvent.PICKUP_CASH
                    }
                    PickupKind.MEDKIT -> {
                        hp = min(current.playerMaxHp, hp + pk.value.toFloat())
                        particles += sparkBurst(pk.x, pk.y, 0xFF2EE58B, 8)
                        soundEvents += SoundEvent.PICKUP_HEALTH
                    }
                    PickupKind.ARMOR -> {
                        armor = max(armor, 60f)
                        particles += sparkBurst(pk.x, pk.y, 0xFF00F0FF, 8)
                        soundEvents += SoundEvent.PICKUP_HEALTH
                    }
                    PickupKind.BRIEFCASE -> {
                        collectedBriefcase = true
                        particles += sparkBurst(pk.x, pk.y, 0xFFFFC83D, 14)
                        soundEvents += SoundEvent.PICKUP_CASH
                    }
                }
            } else {
                keptPickups += pk
            }
        }

        for (pad in CityMap.payNSprayPads) {
            if (hypot(pad.x - px, pad.y - py) < pad.radius && (wantedLevel > 0 || (inVehicleId != null && (vehicles.firstOrNull { it.id == inVehicleId }?.hp ?: 999f) < 120f))) {
                wantedLevel = 0
                heatPoints = 0f
                wantedCooldown = 0f
                bustedProgress = 0f
                soundEvents += SoundEvent.PAY_N_SPRAY
                if (inVehicleId != null) {
                    val vIdx = vehicles.indexOfFirst { it.id == inVehicleId }
                    if (vIdx >= 0) {
                        val v = vehicles[vIdx]
                        val resprayPalette = listOf(0xFFFF2E93L, 0xFF00F0FFL, 0xFFFFC83DL, 0xFF2EE58BL)
                        vehicles[vIdx] = v.copy(
                            hp = v.kind.maxHp,
                            customBodyHex = resprayPalette[Random.nextInt(resprayPalette.size)],
                        )
                    }
                }
                banner = BannerMessage(
                    headline = "PAY 'N' SPRAY!",
                    subline = "Cat ulang selesai — kendaraan pulih & buronan bersih!",
                    accentHex = 0xFF2EE58B,
                    remainingSec = 3.0f,
                )
            }
        }

        // 9. Active Mission Progression (or auto-trigger when stepping on a mission beacon!)
        var activeMission = current.activeMission
        if (activeMission == null) {
            val triggered = CityMap.missions.firstOrNull {
                hypot(it.markerX - px, it.markerY - py) < 58f
            }
            if (triggered != null) {
                val started = startMission(
                    current.copy(
                        peds = peds,
                        pickups = keptPickups,
                        wantedLevel = wantedLevel,
                        heatPoints = heatPoints,
                    ),
                    triggered,
                )
                return started to event
            }
        } else {
            val tmpl = activeMission.template
            var completed = false
            when (tmpl.type) {
                MissionObjectiveType.VEHICLE_DELIVERY -> {
                    if (inVehicleId != null && hypot(activeMission.targetX - px, activeMission.targetY - py) < 76f) {
                        completed = true
                    } else if (inVehicleId == null) {
                        activeMission = activeMission.copy(objectiveHint = "Bajak mobil apa saja lalu bawa ke titik tujuan di Dermaga!")
                    } else {
                        activeMission = activeMission.copy(objectiveHint = "Kemudikan ke titik tujuan di Dermaga Industri!")
                    }
                }
                MissionObjectiveType.ELIMINATE_SYNDICATE -> {
                    val remaining = max(0, activeMission.targetsRemaining - defeatedMissionTargets)
                    if (remaining == 0 && peds.none { it.isMissionTarget }) {
                        completed = true
                    } else {
                        activeMission = activeMission.copy(
                            targetsRemaining = remaining,
                            objectiveHint = "Sisa target sindikat: $remaining orang di Vice Plaza!",
                        )
                    }
                }
                MissionObjectiveType.SURVIVE_HEAT -> {
                    val rem = activeMission.timerRemainingSec - dt
                    if (rem <= 0f) {
                        completed = true
                    } else {
                        activeMission = activeMission.copy(
                            timerRemainingSec = rem,
                            objectiveHint = "Bertahan dari polisi: ${rem.toInt() + 1} detik tersisa!",
                        )
                    }
                }
                MissionObjectiveType.HEIST_ESCAPE -> {
                    if (activeMission.stage == 0 && collectedBriefcase) {
                        heatPoints = max(heatPoints, 320f)
                        wantedLevel = max(wantedLevel, 4)
                        wantedCooldown = 16f
                        activeMission = activeMission.copy(
                            stage = 1,
                            targetX = CityMap.SAFE_HOUSE_X,
                            targetY = CityMap.SAFE_HOUSE_Y + 90f,
                            objectiveHint = "Koper diraih! Kabur kembali ke Safe House!",
                        )
                        banner = BannerMessage(
                            headline = "KOPER DIDAPATKAN!",
                            subline = "Polisi mengejar — tancap gas ke Safe House!",
                            accentHex = 0xFFFFC83D,
                            remainingSec = 3.0f,
                        )
                    } else if (activeMission.stage == 1 && hypot(activeMission.targetX - px, activeMission.targetY - py) < 76f) {
                        completed = true
                    }
                }
            }

            if (completed) {
                sessionCash += tmpl.rewardCash
                sessionRespect += tmpl.rewardRespect
                sessionScore += tmpl.rewardCash + tmpl.rewardRespect
                wantedLevel = 0
                heatPoints = 0f
                bustedProgress = 0f
                activeMission = null
                soundEvents += SoundEvent.MISSION_PASSED
                banner = BannerMessage(
                    headline = "MISSION PASSED!",
                    subline = "${tmpl.title} • +$${tmpl.rewardCash} • +${tmpl.rewardRespect} Respect",
                    accentHex = 0xFFFFC83D,
                    remainingSec = 4.5f,
                )
                event = mergeEvent(
                    event,
                    CareerEvent(
                        cashDelta = tmpl.rewardCash,
                        respectDelta = tmpl.rewardRespect,
                        scoreUpdate = sessionScore,
                        missionsCompletedDelta = 1L,
                        maxWantedSeen = sessionMaxWanted,
                        runLogTitle = tmpl.title,
                        runLogOutcome = if (tmpl.type == MissionObjectiveType.HEIST_ESCAPE) RunOutcome.HEIST else RunOutcome.PASSED,
                        runLogCash = tmpl.rewardCash,
                        runLogWanted = sessionMaxWanted,
                    ),
                )
            }
        }

        // 10. Check WASTED / BUSTED terminal states
        var isDeadOrBusted = false
        if (hp <= 0f) {
            hp = 0f
            isDeadOrBusted = true
            soundEvents += SoundEvent.PLAYER_WASTED
            banner = BannerMessage(
                headline = "WASTED",
                subline = "Biaya rumah sakit -$100 • Ketuk untuk lanjut dari Safe House",
                accentHex = 0xFFFF3B5C,
                remainingSec = 999f,
                isTerminalOverlay = true,
            )
            event = mergeEvent(
                event,
                CareerEvent(
                    cashDelta = -100L,
                    maxWantedSeen = sessionMaxWanted,
                    runLogTitle = activeMission?.template?.title ?: "Bentrok Jalanan",
                    runLogOutcome = RunOutcome.WASTED,
                    runLogCash = -100L,
                    runLogWanted = sessionMaxWanted,
                ),
            )
        } else if (bustedProgress >= 1f) {
            isDeadOrBusted = true
            soundEvents += SoundEvent.PLAYER_BUSTED
            banner = BannerMessage(
                headline = "BUSTED",
                subline = "Tertangkap polisi! Denda -$150 • Ketuk untuk bebas bersyarat",
                accentHex = 0xFF2B7BFF,
                remainingSec = 999f,
                isTerminalOverlay = true,
            )
            event = mergeEvent(
                event,
                CareerEvent(
                    cashDelta = -150L,
                    maxWantedSeen = sessionMaxWanted,
                    runLogTitle = activeMission?.template?.title ?: "Kejaran Polisi",
                    runLogOutcome = RunOutcome.BUSTED,
                    runLogCash = -150L,
                    runLogWanted = sessionMaxWanted,
                ),
            )
        }

        // 11. Nearest vehicle indicator for Hijack button + District notification
        val nearestVeh = if (inVehicleId == null) {
            vehicles.minByOrNull { hypot(it.x - px, it.y - py) }?.takeIf { hypot(it.x - px, it.y - py) <= 88f }
        } else null

        val district = CityMap.districtAt(px, py)
        val districtTimer = if (district != current.currentDistrict) 2.5f else max(0f, current.districtBannerTimerSec - dt)

        val nextState = current.copy(
            playerX = px,
            playerY = py,
            playerAngle = pAngle,
            playerHp = hp,
            playerArmor = armor,
            stamina = stamina,
            nitroFuel = nitro,
            inVehicleId = inVehicleId,
            nearestVehicleId = nearestVeh?.id,
            wantedLevel = wantedLevel,
            heatPoints = heatPoints,
            wantedCooldownSec = wantedCooldown,
            bustedProgress = bustedProgress,
            fireCooldownSec = fireCooldown,
            sessionCashEarned = sessionCash,
            sessionScore = sessionScore,
            sessionRespectEarned = sessionRespect,
            sessionVehiclesStolen = sessionStolen,
            sessionMaxWanted = sessionMaxWanted,
            vehicles = vehicles,
            peds = peds,
            projectiles = remainingProjectiles,
            particles = particles,
            skidMarks = skids,
            pickups = keptPickups,
            activeMission = activeMission,
            banner = banner,
            screenShake = shake,
            clockPulse = current.clockPulse + dt,
            currentDistrict = district,
            districtBannerTimerSec = districtTimer,
            isDeadOrBusted = isDeadOrBusted,
            isDrifting = isDrifting,
            playerWalkPhase = playerWalkPhase,
            isPlayerMoving = isPlayerMoving,
            soundEvents = soundEvents,
        )

        return nextState to event
    }

    private fun findAssistedAimAngle(
        px: Float,
        py: Float,
        baseAngle: Float,
        peds: List<WorldPed>,
        vehicles: List<WorldVehicle>,
        inVehicleId: Int?,
    ): Float {
        var bestAngle = baseAngle
        var bestDist = 420f
        for (p in peds) {
            if (p.role == PedRole.CIVILIAN && !p.isMissionTarget) continue
            val d = hypot(p.x - px, p.y - py)
            if (d < bestDist) {
                val a = atan2(p.y - py, p.x - px)
                if (abs(normalizeAngle(a - baseAngle)) < 0.65f) {
                    bestDist = d
                    bestAngle = a
                }
            }
        }
        if (bestDist == 420f) {
            for (v in vehicles) {
                if (!v.isPolice || v.id == inVehicleId) continue
                val d = hypot(v.x - px, v.y - py)
                if (d < 420f) {
                    val a = atan2(v.y - py, v.x - px)
                    if (abs(normalizeAngle(a - baseAngle)) < 0.55f) {
                        bestAngle = a
                        break
                    }
                }
            }
        }
        return bestAngle
    }

    private fun sparkBurst(x: Float, y: Float, colorHex: Long, count: Int): List<ParticleEffect> =
        List(count) {
            val a = Random.nextFloat() * 6.283f
            val spd = 60f + Random.nextFloat() * 190f
            ParticleEffect(
                x = x,
                y = y,
                vx = cos(a) * spd,
                vy = sin(a) * spd,
                lifeSec = 0.22f + Random.nextFloat() * 0.25f,
                maxLifeSec = 0.45f,
                size = 3.5f + Random.nextFloat() * 4.5f,
                colorHex = colorHex,
            )
        }

    private fun spawnOnNearbyRoad(px: Float, py: Float, minDist: Float, maxDist: Float): Pair<Float, Float> {
        repeat(12) {
            val rx = CityMap.roadCenters[Random.nextInt(CityMap.roadCenters.size)]
            val ry = CityMap.roadCenters[Random.nextInt(CityMap.roadCenters.size)]
            val d = hypot(rx - px, ry - py)
            if (d in minDist..maxDist) return rx to ry
        }
        val fallbackX = (px + if (px < CityMap.WORLD_SIZE / 2f) 620f else -620f).coerceIn(100f, CityMap.WORLD_SIZE - 100f)
        val fallbackY = (py + if (py < CityMap.WORLD_SIZE / 2f) 420f else -420f).coerceIn(100f, CityMap.WORLD_SIZE - 100f)
        return CityMap.resolveCircleBuildingCollision(fallbackX, fallbackY, 34f)
    }

    private fun randomSidewalkSpot(): Pair<Float, Float> {
        val rx = CityMap.roadCenters[Random.nextInt(CityMap.roadCenters.size)] + (Random.nextFloat() - 0.5f) * 120f
        val ry = CityMap.roadCenters[Random.nextInt(CityMap.roadCenters.size)] + (Random.nextFloat() - 0.5f) * 120f
        return CityMap.resolveCircleBuildingCollision(
            rx.coerceIn(90f, CityMap.WORLD_SIZE - 90f),
            ry.coerceIn(90f, CityMap.WORLD_SIZE - 90f),
            16f,
        )
    }

    private fun normalizeAngle(a: Float): Float {
        var res = a
        while (res > PI.toFloat()) res -= (2f * PI).toFloat()
        while (res < -PI.toFloat()) res += (2f * PI).toFloat()
        return res
    }

    private fun mergeEvent(a: CareerEvent?, b: CareerEvent): CareerEvent {
        if (a == null) return b
        return CareerEvent(
            cashDelta = a.cashDelta + b.cashDelta,
            respectDelta = a.respectDelta + b.respectDelta,
            scoreUpdate = max(a.scoreUpdate, b.scoreUpdate),
            missionsCompletedDelta = a.missionsCompletedDelta + b.missionsCompletedDelta,
            vehiclesStolenDelta = a.vehiclesStolenDelta + b.vehiclesStolenDelta,
            maxWantedSeen = max(a.maxWantedSeen, b.maxWantedSeen),
            runLogTitle = b.runLogTitle ?: a.runLogTitle,
            runLogOutcome = b.runLogOutcome ?: a.runLogOutcome,
            runLogCash = if (b.runLogOutcome != null) b.runLogCash else a.runLogCash,
            runLogWanted = max(a.runLogWanted, b.runLogWanted),
        )
    }
}
