package com.example.game

import androidx.compose.ui.geometry.Rect
import kotlin.math.max
import kotlin.math.min

enum class District(val title: String, val accentHex: Long) {
    DOWNTOWN_NEON("Downtown Neon", 0xFFFF2E93),
    VICE_PLAZA("Vice Plaza", 0xFFFFC83D),
    OCEAN_DRIVE("Ocean Drive", 0xFF00F0FF),
    INDUSTRIAL_DOCKS("Dermaga Industri", 0xFF2EE58B),
}

data class BuildingBlock(
    val rect: Rect,
    val district: District,
    val roofColorHex: Long,
    val trimColorHex: Long,
    val heightOffset: Float,
    val label: String? = null,
)

data class PayNSprayPad(
    val id: String,
    val name: String,
    val x: Float,
    val y: Float,
    val radius: Float = 72f,
)

data class MissionTemplate(
    val id: String,
    val title: String,
    val brief: String,
    val markerX: Float,
    val markerY: Float,
    val targetX: Float,
    val targetY: Float,
    val rewardCash: Long,
    val rewardRespect: Long,
    val type: MissionObjectiveType,
)

enum class MissionObjectiveType {
    VEHICLE_DELIVERY,
    ELIMINATE_SYNDICATE,
    SURVIVE_HEAT,
    HEIST_ESCAPE,
}

object CityMap {
    const val WORLD_SIZE = 3400f
    const val ROAD_WIDTH = 156f
    const val BLOCK_SIZE = 420f
    const val CELL_STEP = ROAD_WIDTH + BLOCK_SIZE // 576f
    const val GRID_COUNT = 5 // 5x5 blocks -> 6 road avenues each axis
    const val OCEAN_LEFT_LIMIT = 0f
    const val SAFE_HOUSE_X = 870f
    const val SAFE_HOUSE_Y = 870f

    // Road center coordinates: 78, 654, 1230, 1806, 2382, 2958
    val roadCenters: List<Float> = List(GRID_COUNT + 1) { index ->
        ROAD_WIDTH / 2f + index * CELL_STEP
    }

    val buildings: List<BuildingBlock> = buildList {
        for (gx in 0 until GRID_COUNT) {
            for (gy in 0 until GRID_COUNT) {
                val cellLeft = ROAD_WIDTH + gx * CELL_STEP
                val cellTop = ROAD_WIDTH + gy * CELL_STEP
                val district = when {
                    gx < 2 && gy < 3 -> District.DOWNTOWN_NEON
                    gx >= 2 && gy < 3 -> District.VICE_PLAZA
                    gx < 2 && gy >= 3 -> District.OCEAN_DRIVE
                    else -> District.INDUSTRIAL_DOCKS
                }

                // Leave Safe House plaza open at (1, 1) and Pay'N'Spray plaza at (3, 1) and (2, 3)
                if ((gx == 1 && gy == 1) || (gx == 3 && gy == 1) || (gx == 2 && gy == 3)) {
                    // Smaller corner kiosk only
                    add(
                        BuildingBlock(
                            rect = Rect(
                                left = cellLeft + 26f,
                                top = cellTop + 26f,
                                right = cellLeft + 150f,
                                bottom = cellTop + 140f,
                            ),
                            district = district,
                            roofColorHex = 0xFF1F1B38,
                            trimColorHex = district.accentHex,
                            heightOffset = 14f,
                            label = if (gx == 1 && gy == 1) "SAFE HOUSE" else "GARASI",
                        )
                    )
                    continue
                }

                // Split block into 2 stylized buildings with an alleyway between them
                val trim = district.accentHex
                val roofA = if ((gx + gy) % 2 == 0) 0xFF1B1733 else 0xFF231D42
                val roofB = if ((gx + gy) % 2 == 0) 0xFF251F47 else 0xFF1A2238
                add(
                    BuildingBlock(
                        rect = Rect(
                            left = cellLeft + 28f,
                            top = cellTop + 28f,
                            right = cellLeft + BLOCK_SIZE / 2f - 14f,
                            bottom = cellTop + BLOCK_SIZE - 28f,
                        ),
                        district = district,
                        roofColorHex = roofA,
                        trimColorHex = trim,
                        heightOffset = 18f + ((gx * 3 + gy * 5) % 16),
                        label = when {
                            gx == 2 && gy == 0 -> "CASINO"
                            gx == 0 && gy == 1 -> "NEON CLUB"
                            gx == 4 && gy == 4 -> "DERMAGA"
                            else -> null
                        },
                    )
                )
                add(
                    BuildingBlock(
                        rect = Rect(
                            left = cellLeft + BLOCK_SIZE / 2f + 14f,
                            top = cellTop + 28f,
                            right = cellLeft + BLOCK_SIZE - 28f,
                            bottom = cellTop + BLOCK_SIZE - 28f,
                        ),
                        district = district,
                        roofColorHex = roofB,
                        trimColorHex = if (district == District.DOWNTOWN_NEON) 0xFF00F0FF else trim,
                        heightOffset = 14f + ((gx * 7 + gy * 2) % 14),
                    )
                )
            }
        }
    }

    val payNSprayPads: List<PayNSprayPad> = listOf(
        PayNSprayPad(
            id = "pns_plaza",
            name = "Pay 'N' Spray Plaza",
            x = ROAD_WIDTH + 3 * CELL_STEP + 270f,
            y = ROAD_WIDTH + 1 * CELL_STEP + 240f,
        ),
        PayNSprayPad(
            id = "pns_docks",
            name = "Pay 'N' Spray Selatan",
            x = ROAD_WIDTH + 2 * CELL_STEP + 270f,
            y = ROAD_WIDTH + 3 * CELL_STEP + 240f,
        ),
    )

    val palmPositions: List<Pair<Float, Float>> = buildList {
        for (x in roadCenters) {
            for (y in roadCenters) {
                add((x - 90f) to (y - 90f))
                add((x + 90f) to (y + 90f))
            }
        }
    }.filter { (x, y) -> x in 60f..(WORLD_SIZE - 60f) && y in 60f..(WORLD_SIZE - 60f) }

    val missions: List<MissionTemplate> = listOf(
        MissionTemplate(
            id = "HOTWIRE_RUN",
            title = "Hotwire Run",
            brief = "Bajak mobil apa saja dan antarkan ke titik pengiriman di Dermaga Industri sebelum polisi mencegat.",
            markerX = 940f,
            markerY = 654f,
            targetX = 2958f,
            targetY = 2958f,
            rewardCash = 1500L,
            rewardRespect = 180L,
            type = MissionObjectiveType.VEHICLE_DELIVERY,
        ),
        MissionTemplate(
            id = "SYNDICATE_SHAKEDOWN",
            title = "Pembersihan Sindikat",
            brief = "Lumpuhkan 4 penjaga sindikat bertanda merah di kawasan Vice Plaza dan Downtown.",
            markerX = 1806f,
            markerY = 654f,
            targetX = 1806f,
            targetY = 1230f,
            rewardCash = 2200L,
            rewardRespect = 250L,
            type = MissionObjectiveType.ELIMINATE_SYNDICATE,
        ),
        MissionTemplate(
            id = "MIDNIGHT_HEAT",
            title = "Panas Tengah Malam",
            brief = "Pancing kejaran polisi dan bertahan hidup selama 25 detik di tengah kota!",
            markerX = 1230f,
            markerY = 1806f,
            targetX = 1230f,
            targetY = 1806f,
            rewardCash = 2800L,
            rewardRespect = 320L,
            type = MissionObjectiveType.SURVIVE_HEAT,
        ),
        MissionTemplate(
            id = "ARMORED_HEIST",
            title = "Perampokan Kurir",
            brief = "Ambil koper uang sindikat di Casino Vice Plaza lalu kabur kembali ke Safe House.",
            markerX = 1806f,
            markerY = 1230f,
            targetX = 1480f,
            targetY = 360f,
            rewardCash = 3500L,
            rewardRespect = 420L,
            type = MissionObjectiveType.HEIST_ESCAPE,
        ),
    )

    fun districtAt(x: Float, y: Float): District = when {
        x < 1300f && y < 1850f -> District.DOWNTOWN_NEON
        x >= 1300f && y < 1850f -> District.VICE_PLAZA
        x < 1300f && y >= 1850f -> District.OCEAN_DRIVE
        else -> District.INDUSTRIAL_DOCKS
    }

    fun isOnRoad(x: Float, y: Float): Boolean {
        val halfRoad = ROAD_WIDTH / 2f
        val onVertical = roadCenters.any { cx -> kotlin.math.abs(x - cx) <= halfRoad }
        val onHorizontal = roadCenters.any { cy -> kotlin.math.abs(y - cy) <= halfRoad }
        return onVertical || onHorizontal
    }

    fun resolveCircleBuildingCollision(
        x: Float,
        y: Float,
        radius: Float,
    ): Pair<Float, Float> {
        var rx = x.coerceIn(radius + 20f, WORLD_SIZE - radius - 20f)
        var ry = y.coerceIn(radius + 20f, WORLD_SIZE - radius - 20f)
        for (b in buildings) {
            val rect = b.rect
            val closestX = max(rect.left, min(rx, rect.right))
            val closestY = max(rect.top, min(ry, rect.bottom))
            val dx = rx - closestX
            val dy = ry - closestY
            val distSq = dx * dx + dy * dy
            if (distSq < radius * radius) {
                val dist = kotlin.math.sqrt(distSq)
                if (dist > 0.0001f) {
                    val push = radius - dist
                    rx += (dx / dist) * push
                    ry += (dy / dist) * push
                } else {
                    rx += radius
                }
            }
        }
        return rx to ry
    }
}
