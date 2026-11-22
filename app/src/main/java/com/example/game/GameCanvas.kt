package com.example.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.ui.theme.CashGreen
import com.example.ui.theme.DangerRed
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.PoliceBlue
import com.example.ui.theme.ViceGold
import com.example.ui.theme.ViceVoid
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun GameCanvas(
    world: GameWorldState,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val viewW = size.width
        val viewH = size.height
        val camX = world.playerX - viewW / 2f
        val camY = world.playerY - viewH / 2f

        // Dark asphalt city base
        drawRect(color = Color(0xFF141224))

        translate(left = -camX, top = -camY) {
            // 1. Ocean Drive coastal strip on far left edge
            drawRect(
                color = Color(0xFF07233B),
                topLeft = Offset(-300f, -300f),
                size = Size(340f, CityMap.WORLD_SIZE + 600f),
            )
            drawRect(
                color = ElectricCyan.copy(alpha = 0.28f),
                topLeft = Offset(34f, 0f),
                size = Size(8f, CityMap.WORLD_SIZE),
            )

            // 2. City block sidewalks
            val halfRoad = CityMap.ROAD_WIDTH / 2f
            for (gx in 0 until CityMap.GRID_COUNT) {
                for (gy in 0 until CityMap.GRID_COUNT) {
                    val left = CityMap.ROAD_WIDTH + gx * CityMap.CELL_STEP
                    val top = CityMap.ROAD_WIDTH + gy * CityMap.CELL_STEP
                    if (left + CityMap.BLOCK_SIZE < camX - 40f || left > camX + viewW + 40f ||
                        top + CityMap.BLOCK_SIZE < camY - 40f || top > camY + viewH + 40f
                    ) continue
                    drawRoundRect(
                        color = Color(0xFF231F3A),
                        topLeft = Offset(left, top),
                        size = Size(CityMap.BLOCK_SIZE, CityMap.BLOCK_SIZE),
                        cornerRadius = CornerRadius(14f, 14f),
                    )
                    drawRoundRect(
                        color = Color(0xFF342E54),
                        topLeft = Offset(left, top),
                        size = Size(CityMap.BLOCK_SIZE, CityMap.BLOCK_SIZE),
                        cornerRadius = CornerRadius(14f, 14f),
                        style = Stroke(width = 3f),
                    )
                }
            }

            // 3. Road lane dashes
            for (rc in CityMap.roadCenters) {
                if (rc in (camX - 120f)..(camX + viewW + 120f)) {
                    var y = 40f
                    while (y < CityMap.WORLD_SIZE - 40f) {
                        drawLine(
                            color = ViceGold.copy(alpha = 0.48f),
                            start = Offset(rc, y),
                            end = Offset(rc, y + 36f),
                            strokeWidth = 3f,
                        )
                        y += 72f
                    }
                }
                if (rc in (camY - 120f)..(camY + viewH + 120f)) {
                    var x = 40f
                    while (x < CityMap.WORLD_SIZE - 40f) {
                        drawLine(
                            color = ViceGold.copy(alpha = 0.48f),
                            start = Offset(x, rc),
                            end = Offset(x + 36f, rc),
                            strokeWidth = 3f,
                        )
                        x += 72f
                    }
                }
            }

            // 4. Tire skid marks
            for (skid in world.skidMarks) {
                drawLine(
                    color = Color.Black.copy(alpha = skid.alpha * 0.55f),
                    start = Offset(skid.x1, skid.y1),
                    end = Offset(skid.x2, skid.y2),
                    strokeWidth = 5f,
                    cap = StrokeCap.Round,
                )
            }

            // 5. Pay 'N' Spray pads & Safe House pad
            drawCircle(
                color = ElectricCyan.copy(alpha = 0.16f + 0.08f * sin(world.clockPulse * 4f)),
                radius = 52f,
                center = Offset(CityMap.SAFE_HOUSE_X, CityMap.SAFE_HOUSE_Y + 90f),
            )
            drawCircle(
                color = ElectricCyan,
                radius = 52f,
                center = Offset(CityMap.SAFE_HOUSE_X, CityMap.SAFE_HOUSE_Y + 90f),
                style = Stroke(width = 3f),
            )

            for (pad in CityMap.payNSprayPads) {
                val pulse = 0.2f + 0.12f * sin(world.clockPulse * 5f)
                drawCircle(
                    color = CashGreen.copy(alpha = pulse),
                    radius = pad.radius,
                    center = Offset(pad.x, pad.y),
                )
                drawCircle(
                    color = CashGreen,
                    radius = pad.radius,
                    center = Offset(pad.x, pad.y),
                    style = Stroke(width = 3.5f),
                )
            }

            // 6. Mission markers (available beacons when idle, or target beacon when active)
            val activeMission = world.activeMission
            if (activeMission == null) {
                for (m in CityMap.missions) {
                    val pulseR = 38f + 10f * sin(world.clockPulse * 5f)
                    drawCircle(
                        color = ViceGold.copy(alpha = 0.22f),
                        radius = pulseR,
                        center = Offset(m.markerX, m.markerY),
                    )
                    drawCircle(
                        color = ViceGold,
                        radius = 24f,
                        center = Offset(m.markerX, m.markerY),
                        style = Stroke(width = 3.5f),
                    )
                }
            } else if (activeMission.template.type != MissionObjectiveType.ELIMINATE_SYNDICATE &&
                activeMission.template.type != MissionObjectiveType.SURVIVE_HEAT
            ) {
                val pulseR = 46f + 12f * sin(world.clockPulse * 6f)
                drawCircle(
                    color = NeonMagenta.copy(alpha = 0.25f),
                    radius = pulseR,
                    center = Offset(activeMission.targetX, activeMission.targetY),
                )
                drawCircle(
                    color = NeonMagenta,
                    radius = 34f,
                    center = Offset(activeMission.targetX, activeMission.targetY),
                    style = Stroke(width = 4f),
                )
            }

            // 7. Pickups (Cash, Medkit, Armor, Briefcase)
            for (pk in world.pickups) {
                if (pk.x !in (camX - 60f)..(camX + viewW + 60f) ||
                    pk.y !in (camY - 60f)..(camY + viewH + 60f)
                ) continue
                val color = when (pk.kind) {
                    PickupKind.CASH -> CashGreen
                    PickupKind.MEDKIT -> DangerRed
                    PickupKind.ARMOR -> ElectricCyan
                    PickupKind.BRIEFCASE -> ViceGold
                }
                val bob = sin(world.clockPulse * 5f + pk.id) * 3f
                drawCircle(color = color.copy(alpha = 0.28f), radius = 18f, center = Offset(pk.x, pk.y + bob))
                drawRoundRect(
                    color = color,
                    topLeft = Offset(pk.x - 9f, pk.y - 7f + bob),
                    size = Size(18f, 14f),
                    cornerRadius = CornerRadius(4f, 4f),
                )
            }

            // 8. Pedestrians
            for (ped in world.peds) {
                if (ped.x !in (camX - 60f)..(camX + viewW + 60f) ||
                    ped.y !in (camY - 60f)..(camY + viewH + 60f)
                ) continue
                drawPed(ped, world.clockPulse)
            }

            // 9. Vehicles
            for (veh in world.vehicles) {
                if (veh.x !in (camX - 110f)..(camX + viewW + 110f) ||
                    veh.y !in (camY - 110f)..(camY + viewH + 110f)
                ) continue
                val isNearest = veh.id == world.nearestVehicleId
                drawVehicle(veh, isNearest)
            }

            // 10. Player on foot (if not inside a car)
            if (world.inVehicleId == null) {
                drawPlayer(world)
            }

            // 11. Projectiles
            for (proj in world.projectiles) {
                drawCircle(
                    color = Color(proj.colorHex),
                    radius = proj.radius,
                    center = Offset(proj.x, proj.y),
                )
            }

            // 12. 2.5D Buildings (drawn over ground entities for authentic rooftop depth!)
            for (b in CityMap.buildings) {
                val r = b.rect
                if (r.right < camX - 60f || r.left > camX + viewW + 60f ||
                    r.bottom < camY - 60f || r.top > camY + viewH + 60f
                ) continue
                // Drop shadow
                drawRoundRect(
                    color = Color.Black.copy(alpha = 0.45f),
                    topLeft = Offset(r.left + 12f, r.top + 14f),
                    size = Size(r.width, r.height),
                    cornerRadius = CornerRadius(8f, 8f),
                )
                // Extruded wall base
                drawRoundRect(
                    color = Color(0xFF100E1F),
                    topLeft = Offset(r.left, r.top),
                    size = Size(r.width, r.height),
                    cornerRadius = CornerRadius(8f, 8f),
                )
                // Elevated rooftop
                val roofTop = r.top - b.heightOffset
                val roofLeft = r.left - b.heightOffset * 0.25f
                drawRoundRect(
                    color = Color(b.roofColorHex),
                    topLeft = Offset(roofLeft, roofTop),
                    size = Size(r.width, r.height),
                    cornerRadius = CornerRadius(8f, 8f),
                )
                drawRoundRect(
                    color = Color(b.trimColorHex).copy(alpha = 0.72f),
                    topLeft = Offset(roofLeft, roofTop),
                    size = Size(r.width, r.height),
                    cornerRadius = CornerRadius(8f, 8f),
                    style = Stroke(width = 2.5f),
                )
            }

            // 13. Palm trees along intersections
            for ((tx, ty) in CityMap.palmPositions) {
                if (tx !in (camX - 60f)..(camX + viewW + 60f) ||
                    ty !in (camY - 60f)..(camY + viewH + 60f)
                ) continue
                for (frond in 0 until 6) {
                    val a = frond * (PI / 3.0).toFloat() + sin(world.clockPulse * 1.5f) * 0.05f
                    drawLine(
                        color = Color(0xFF1CC978).copy(alpha = 0.85f),
                        start = Offset(tx, ty),
                        end = Offset(tx + cos(a) * 20f, ty + sin(a) * 20f),
                        strokeWidth = 4.5f,
                        cap = StrokeCap.Round,
                    )
                }
                drawCircle(color = Color(0xFF6B4226), radius = 4f, center = Offset(tx, ty))
            }

            // 14. Particles (drawn last so sparks & explosions pop above rooftops)
            for (pt in world.particles) {
                val alpha = (pt.lifeSec / pt.maxLifeSec).coerceIn(0f, 1f)
                drawCircle(
                    color = Color(pt.colorHex).copy(alpha = alpha),
                    radius = pt.size * alpha,
                    center = Offset(pt.x, pt.y),
                )
            }
        }

        // 15. Offscreen Objective Waypoint Arrow around screen center
        val targetCoords: Pair<Float, Float>? = world.activeMission?.let { m ->
            if (m.template.type == MissionObjectiveType.ELIMINATE_SYNDICATE) {
                world.peds.firstOrNull { it.isMissionTarget }?.let { it.x to it.y }
            } else if (m.template.type == MissionObjectiveType.SURVIVE_HEAT) {
                null
            } else {
                m.targetX to m.targetY
            }
        } ?: CityMap.missions.minByOrNull { hypot(it.markerX - world.playerX, it.markerY - world.playerY) }
            ?.let { it.markerX to it.markerY }

        if (targetCoords != null) {
            val dx = targetCoords.first - world.playerX
            val dy = targetCoords.second - world.playerY
            val dist = hypot(dx, dy)
            if (dist > 220f) {
                val angle = atan2(dy, dx)
                val arrowRadius = minOf(viewW, viewH) * 0.34f
                val ax = viewW / 2f + cos(angle) * arrowRadius
                val ay = viewH / 2f + sin(angle) * arrowRadius
                val arrowColor = if (world.activeMission != null) NeonMagenta else ViceGold
                rotate(degrees = Math.toDegrees(angle.toDouble()).toFloat(), pivot = Offset(ax, ay)) {
                    val path = Path().apply {
                        moveTo(ax + 14f, ay)
                        lineTo(ax - 8f, ay - 9f)
                        lineTo(ax - 4f, ay)
                        lineTo(ax - 8f, ay + 9f)
                        close()
                    }
                    drawPath(path = path, color = arrowColor)
                }
            }
        }
    }
}

private fun DrawScope.drawVehicle(veh: WorldVehicle, isHighlighted: Boolean) {
    val bodyColor = Color(veh.customBodyHex ?: veh.kind.bodyHex)
    val stripeColor = Color(veh.kind.stripeHex)
    val deg = Math.toDegrees(veh.angle.toDouble()).toFloat()
    rotate(degrees = deg, pivot = Offset(veh.x, veh.y)) {
        val halfL = veh.kind.length / 2f
        val halfW = veh.kind.width / 2f
        if (isHighlighted) {
            drawRoundRect(
                color = ElectricCyan.copy(alpha = 0.55f),
                topLeft = Offset(veh.x - halfL - 5f, veh.y - halfW - 5f),
                size = Size(veh.kind.length + 10f, veh.kind.width + 10f),
                cornerRadius = CornerRadius(10f, 10f),
                style = Stroke(width = 3f),
            )
        }
        // Shadow
        drawRoundRect(
            color = Color.Black.copy(alpha = 0.45f),
            topLeft = Offset(veh.x - halfL + 3f, veh.y - halfW + 4f),
            size = Size(veh.kind.length, veh.kind.width),
            cornerRadius = CornerRadius(8f, 8f),
        )
        // Headlight beams when moving or personal
        if (veh.speed > 30f || veh.isPersonal || veh.isPolice) {
            drawCircle(
                color = ElectricCyan.copy(alpha = 0.14f),
                radius = 32f,
                center = Offset(veh.x + halfL + 18f, veh.y),
            )
        }
        // Chassis
        drawRoundRect(
            color = bodyColor,
            topLeft = Offset(veh.x - halfL, veh.y - halfW),
            size = Size(veh.kind.length, veh.kind.width),
            cornerRadius = CornerRadius(8f, 8f),
        )
        // Racing stripe / Police door panel
        drawRect(
            color = stripeColor,
            topLeft = Offset(veh.x - halfL * 0.65f, veh.y - 4f),
            size = Size(veh.kind.length * 0.7f, 8f),
        )
        // Windshield
        drawRoundRect(
            color = Color(0xFF0B1424),
            topLeft = Offset(veh.x - 4f, veh.y - halfW + 5f),
            size = Size(18f, veh.kind.width - 10f),
            cornerRadius = CornerRadius(4f, 4f),
        )
        // Taillights & headlights
        drawCircle(color = DangerRed, radius = 2.8f, center = Offset(veh.x - halfL + 3f, veh.y - halfW + 6f))
        drawCircle(color = DangerRed, radius = 2.8f, center = Offset(veh.x - halfL + 3f, veh.y + halfW - 6f))
        drawCircle(color = ElectricCyan, radius = 3f, center = Offset(veh.x + halfL - 3f, veh.y - halfW + 6f))
        drawCircle(color = ElectricCyan, radius = 3f, center = Offset(veh.x + halfL - 3f, veh.y + halfW - 6f))

        // Police flashing red/blue lightbar
        if (veh.isPolice) {
            val redOn = sin(veh.sirenPhase) > 0f
            drawRect(
                color = if (redOn) DangerRed else PoliceBlue,
                topLeft = Offset(veh.x - 10f, veh.y - halfW + 5f),
                size = Size(6f, (veh.kind.width - 10f) / 2f),
            )
            drawRect(
                color = if (redOn) PoliceBlue else DangerRed,
                topLeft = Offset(veh.x - 10f, veh.y),
                size = Size(6f, (veh.kind.width - 10f) / 2f),
            )
        }
    }
}

private fun DrawScope.drawPed(ped: WorldPed, clockPulse: Float) {
    val bodyColor = when (ped.role) {
        PedRole.CIVILIAN -> Color(0xFF8A85B5)
        PedRole.SYNDICATE_GUARD -> DangerRed
        PedRole.POLICE_OFFICER -> PoliceBlue
    }
    if (ped.isMissionTarget) {
        drawCircle(
            color = DangerRed.copy(alpha = 0.35f + 0.15f * sin(clockPulse * 8f)),
            radius = 22f,
            center = Offset(ped.x, ped.y),
            style = Stroke(width = 3f),
        )
    }
    rotate(degrees = Math.toDegrees(ped.angle.toDouble()).toFloat(), pivot = Offset(ped.x, ped.y)) {
        drawCircle(color = Color.Black.copy(alpha = 0.4f), radius = 12f, center = Offset(ped.x + 2f, ped.y + 3f))
        drawCircle(color = bodyColor, radius = 11f, center = Offset(ped.x, ped.y))
        drawCircle(color = Color(0xFFFFD6B8), radius = 6f, center = Offset(ped.x + 2f, ped.y))
        if (ped.role != PedRole.CIVILIAN) {
            drawLine(
                color = Color.White,
                start = Offset(ped.x + 6f, ped.y + 5f),
                end = Offset(ped.x + 17f, ped.y + 5f),
                strokeWidth = 3f,
            )
        }
    }
}

private fun DrawScope.drawPlayer(world: GameWorldState) {
    val deg = Math.toDegrees(world.playerAngle.toDouble()).toFloat()
    rotate(degrees = deg, pivot = Offset(world.playerX, world.playerY)) {
        drawCircle(
            color = ElectricCyan.copy(alpha = 0.26f),
            radius = 20f,
            center = Offset(world.playerX, world.playerY),
        )
        drawCircle(
            color = NeonMagenta,
            radius = 12.5f,
            center = Offset(world.playerX, world.playerY),
        )
        drawCircle(
            color = Color(0xFFFFDCB8),
            radius = 6.5f,
            center = Offset(world.playerX + 2f, world.playerY),
        )
        if (!world.equippedWeapon.isMelee) {
            drawLine(
                color = ElectricCyan,
                start = Offset(world.playerX + 7f, world.playerY + 5f),
                end = Offset(world.playerX + 20f, world.playerY + 5f),
                strokeWidth = 3.5f,
                cap = StrokeCap.Round,
            )
        }
    }
}

@Composable
fun MinimapRadar(
    world: GameWorldState,
    contentDesc: String,
    modifier: Modifier = Modifier,
    radarSize: androidx.compose.ui.unit.Dp = 96.dp,
    cameraYawRad: Float = -PI.toFloat() / 2f,
) {
    Box(
        modifier = modifier
            .size(radarSize)
            .clip(RoundedCornerShape(14.dp))
            .background(ViceVoid.copy(alpha = 0.88f))
            .border(BorderStroke(1.5.dp, ElectricCyan.copy(alpha = 0.72f)), RoundedCornerShape(14.dp))
            .semantics { contentDescription = contentDesc },
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val scale = size.width / 1700f
            val cx = size.width / 2f
            val cy = size.height / 2f
            val mapRotateDeg = -Math.toDegrees((cameraYawRad + PI / 2.0)).toFloat()

            rotate(degrees = mapRotateDeg, pivot = Offset(cx, cy)) {
                // Ocean coastal strip on west edge
                val coastX = cx + (12f - world.playerX) * scale
                drawRect(
                    color = Color(0xFF0A3254),
                    topLeft = Offset(coastX - size.width, -size.height),
                    size = Size(size.width, size.height * 3f),
                )

                // Roads on radar
                for (rc in CityMap.roadCenters) {
                    val rx = cx + (rc - world.playerX) * scale
                    val ry = cy + (rc - world.playerY) * scale
                    drawLine(
                        color = Color(0xFF423C6E),
                        start = Offset(rx, -size.height),
                        end = Offset(rx, size.height * 2f),
                        strokeWidth = 7f,
                    )
                    drawLine(
                        color = Color(0xFF423C6E),
                        start = Offset(-size.width, ry),
                        end = Offset(size.width * 2f, ry),
                        strokeWidth = 7f,
                    )
                }

                // Building footprints for clear street layout
                for (b in CityMap.buildings) {
                    val bx = cx + (b.rect.left - world.playerX) * scale
                    val by = cy + (b.rect.top - world.playerY) * scale
                    val bw = b.rect.width * scale
                    val bh = b.rect.height * scale
                    if (bx + bw < -10f || bx > size.width + 10f || by + bh < -10f || by > size.height + 10f) continue
                    drawRoundRect(
                        color = Color(0xFF201D38),
                        topLeft = Offset(bx, by),
                        size = Size(bw, bh),
                        cornerRadius = CornerRadius(2f, 2f),
                    )
                    drawRoundRect(
                        color = Color(b.trimColorHex).copy(alpha = 0.24f),
                        topLeft = Offset(bx, by),
                        size = Size(bw, bh),
                        cornerRadius = CornerRadius(2f, 2f),
                        style = Stroke(width = 1f),
                    )
                }

                // Safe House cyan marker
                val shx = cx + (CityMap.SAFE_HOUSE_X - world.playerX) * scale
                val shy = cy + (CityMap.SAFE_HOUSE_Y + 90f - world.playerY) * scale
                drawCircle(color = ElectricCyan.copy(alpha = 0.35f), radius = 5.5f, center = Offset(shx, shy))
                drawCircle(color = ElectricCyan, radius = 3.2f, center = Offset(shx, shy))

                // Pay 'N' Spray dots
                for (pad in CityMap.payNSprayPads) {
                    val px = cx + (pad.x - world.playerX) * scale
                    val py = cy + (pad.y - world.playerY) * scale
                    drawCircle(color = CashGreen.copy(alpha = 0.32f), radius = 5.5f, center = Offset(px, py))
                    drawCircle(color = CashGreen, radius = 3.4f, center = Offset(px, py))
                }

                // Police blips
                for (veh in world.vehicles) {
                    if (!veh.isPolice) continue
                    val vx = cx + (veh.x - world.playerX) * scale
                    val vy = cy + (veh.y - world.playerY) * scale
                    val flash = sin(world.clockPulse * 14f) > 0f
                    drawCircle(
                        color = if (flash) DangerRed else PoliceBlue,
                        radius = 3.8f,
                        center = Offset(vx, vy),
                    )
                }

                // Mission markers & GPS target line
                val active = world.activeMission
                if (active != null) {
                    val mx = cx + (active.targetX - world.playerX) * scale
                    val my = cy + (active.targetY - world.playerY) * scale
                    drawLine(
                        color = NeonMagenta.copy(alpha = 0.55f),
                        start = Offset(cx, cy),
                        end = Offset(mx, my),
                        strokeWidth = 2f,
                    )
                    drawCircle(color = NeonMagenta.copy(alpha = 0.32f), radius = 7.5f, center = Offset(mx, my))
                    drawCircle(color = NeonMagenta, radius = 4.8f, center = Offset(mx, my))
                } else {
                    for (m in CityMap.missions) {
                        val mx = cx + (m.markerX - world.playerX) * scale
                        val my = cy + (m.markerY - world.playerY) * scale
                        drawCircle(color = ViceGold.copy(alpha = 0.30f), radius = 6.2f, center = Offset(mx, my))
                        drawCircle(color = ViceGold, radius = 4.0f, center = Offset(mx, my))
                    }
                }

                // Player directional triangle in world space
                rotate(degrees = Math.toDegrees(world.playerAngle.toDouble()).toFloat(), pivot = Offset(cx, cy)) {
                    val path = Path().apply {
                        moveTo(cx + 8.5f, cy)
                        lineTo(cx - 5f, cy - 5f)
                        lineTo(cx - 2.8f, cy)
                        lineTo(cx - 5f, cy + 5f)
                        close()
                    }
                    drawPath(path = path, color = Color.White)
                }
            }

            // Camera FOV cone + subtle radar outer ring
            val conePath = Path().apply {
                moveTo(cx, cy)
                lineTo(cx - 17f, cy - 32f)
                lineTo(cx + 17f, cy - 32f)
                close()
            }
            drawPath(path = conePath, color = ElectricCyan.copy(alpha = 0.14f))
            drawCircle(
                color = ElectricCyan.copy(alpha = 0.18f),
                radius = size.minDimension * 0.40f,
                center = Offset(cx, cy),
                style = Stroke(width = 1f),
            )
        }
    }
}
