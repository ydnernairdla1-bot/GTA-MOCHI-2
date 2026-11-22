package com.example.game.render3d

import com.example.game.CityMap
import com.example.game.GameWorldState
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

enum class CameraMode(val label: String) {
    CHASE_3D("CHASE 3D"),
    DRONE_3D("DRONE 3D"),
}

class CameraController {
    @Volatile
    var mode: CameraMode = CameraMode.CHASE_3D

    @Volatile
    var activeYawRad: Float = -PI.toFloat() / 2f
        private set

    @Volatile
    var userYawOffset: Float = 0f

    @Volatile
    var userPitchOffsetDeg: Float = 0f

    var camX: Float = CityMap.SAFE_HOUSE_X
        private set
    var camY: Float = 62f
        private set
    var camZ: Float = CityMap.SAFE_HOUSE_Y + 230f
        private set

    var lookX: Float = CityMap.SAFE_HOUSE_X
        private set
    var lookY: Float = 16f
        private set
    var lookZ: Float = CityMap.SAFE_HOUSE_Y + 120f
        private set

    var fovDeg: Float = 54f
        private set

    private var isInitialized: Boolean = false
    private var smoothYaw: Float = -PI.toFloat() / 2f
    private var smoothDist: Float = 86f
    private var smoothSafeDist: Float = 86f
    private var smoothHeight: Float = 42f
    private var smoothFocusX: Float = CityMap.SAFE_HOUSE_X
    private var smoothFocusZ: Float = CityMap.SAFE_HOUSE_Y + 135f
    private var smoothLookY: Float = 14f
    private var manualOrbitCooldown: Float = 0f

    fun toggleMode(): CameraMode {
        val next = if (mode == CameraMode.CHASE_3D) CameraMode.DRONE_3D else CameraMode.CHASE_3D
        mode = next
        userYawOffset = 0f
        userPitchOffsetDeg = 0f
        return next
    }

    fun orbitBy(deltaX: Float, deltaY: Float) {
        userYawOffset = normalizeAngle(userYawOffset + deltaX * 0.0055f)
        userPitchOffsetDeg = (userPitchOffsetDeg + deltaY * 0.08f).coerceIn(-12f, 24f)
        manualOrbitCooldown = 2.6f
    }

    fun update(world: GameWorldState, dtRaw: Float) {
        val dt = dtRaw.coerceIn(0.001f, 0.05f)
        val inCar = world.inVehicleId != null
        val activeVeh = world.inVehicleId?.let { vid -> world.vehicles.firstOrNull { it.id == vid } }
        val vehSpeed = abs(activeVeh?.speed ?: 0f)

        if (!isInitialized) {
            isInitialized = true
            smoothFocusX = world.playerX
            smoothFocusZ = world.playerY
            smoothDist = if (inCar) 122f else 86f
            smoothSafeDist = smoothDist
            smoothHeight = if (inCar) 54f else 42f
            smoothLookY = if (inCar) 11f else 14f
        }

        if (manualOrbitCooldown > 0f) {
            manualOrbitCooldown = max(0f, manualOrbitCooldown - dt)
        } else if (abs(userYawOffset) > 0.001f && mode == CameraMode.CHASE_3D && inCar && vehSpeed > 45f) {
            userYawOffset *= (1f - 1.5f * dt).coerceAtLeast(0f)
        }

        val targetBaseYaw = when (mode) {
            CameraMode.CHASE_3D -> {
                if (inCar && activeVeh != null) {
                    if (vehSpeed > 22f) activeVeh.angle else smoothYaw
                } else {
                    // Keep camera yaw steady when on foot so moving/turning does not shake or swing the camera
                    smoothYaw
                }
            }
            CameraMode.DRONE_3D -> -PI.toFloat() / 2f
        }

        val yawRate = if (mode == CameraMode.CHASE_3D) 3.4f else 5.0f
        smoothYaw = lerpAngle(smoothYaw, targetBaseYaw, min(1f, dt * yawRate))

        val effectiveYaw = normalizeAngle(smoothYaw + userYawOffset)
        activeYawRad = effectiveYaw

        val desiredDist = when (mode) {
            CameraMode.CHASE_3D -> if (inCar) {
                122f + (vehSpeed / 600f).coerceIn(0f, 1f) * 44f
            } else {
                86f
            }
            CameraMode.DRONE_3D -> if (inCar) 265f else 225f
        }
        val desiredHeight = when (mode) {
            CameraMode.CHASE_3D -> (if (inCar) 54f + (vehSpeed / 600f).coerceIn(0f, 1f) * 14f else 42f) + userPitchOffsetDeg * 1.4f
            CameraMode.DRONE_3D -> 235f + userPitchOffsetDeg * 3.0f
        }.coerceIn(24f, 340f)

        smoothDist += (desiredDist - smoothDist) * min(1f, dt * 4.5f)
        smoothHeight += (desiredHeight - smoothHeight) * min(1f, dt * 4.5f)

        val targetFov = if (inCar) 58f + (vehSpeed / 620f).coerceIn(0f, 1f) * 12f else 54f
        fovDeg += (targetFov - fovDeg) * min(1f, dt * 4f)

        // Unified steady focus anchor so camera and look target move in lockstep without jitter
        val focusAlpha = min(1f, dt * 16f)
        smoothFocusX += (world.playerX - smoothFocusX) * focusAlpha
        smoothFocusZ += (world.playerY - smoothFocusZ) * focusAlpha

        val targetLookY = if (inCar) 11f else 14f
        smoothLookY += (targetLookY - smoothLookY) * min(1f, dt * 8f)

        val leadDist = if (mode == CameraMode.CHASE_3D) (if (inCar) 34f else 18f) else 0f
        val fwdX = cos(effectiveYaw)
        val fwdZ = sin(effectiveYaw)

        val rawCamX = smoothFocusX - fwdX * smoothDist
        val rawCamZ = smoothFocusZ - fwdZ * smoothDist

        // Prevent tall skyscrapers from clipping in front of the chase camera with smooth distance transitions
        val clippedFraction = if (mode == CameraMode.CHASE_3D) {
            raycastBuildingsFraction(smoothFocusX, smoothFocusZ, rawCamX, rawCamZ)
        } else 1f
        val targetSafeDist = max(36f, smoothDist * clippedFraction)
        val distRate = if (targetSafeDist < smoothSafeDist) 14f else 6f
        smoothSafeDist += (targetSafeDist - smoothSafeDist) * min(1f, dt * distRate)

        camX = smoothFocusX - fwdX * smoothSafeDist
        camZ = smoothFocusZ - fwdZ * smoothSafeDist
        camY = smoothHeight

        lookX = smoothFocusX + fwdX * leadDist
        lookY = smoothLookY
        lookZ = smoothFocusZ + fwdZ * leadDist
    }

    fun screenStickToWorld(stickX: Float, stickY: Float): Pair<Float, Float> {
        if (abs(stickX) < 0.001f && abs(stickY) < 0.001f) return 0f to 0f
        val yaw = activeYawRad
        val fwdX = cos(yaw)
        val fwdY = sin(yaw)
        val rightX = -sin(yaw)
        val rightY = cos(yaw)
        val forwardAmt = -stickY
        val rightAmt = stickX
        return (fwdX * forwardAmt + rightX * rightAmt) to (fwdY * forwardAmt + rightY * rightAmt)
    }

    private fun raycastBuildingsFraction(x0: Float, z0: Float, x1: Float, z1: Float): Float {
        var bestT = 1f
        val dx = x1 - x0
        val dz = z1 - z0
        if (hypot(dx, dz) < 1f) return 1f
        for (b in CityMap.buildings) {
            val r = b.rect
            val minX = r.left - 8f
            val maxX = r.right + 8f
            val minZ = r.top - 8f
            val maxZ = r.bottom + 8f
            var tMin = 0f
            var tMax = bestT

            if (abs(dx) < 1e-5f) {
                if (x0 < minX || x0 > maxX) continue
            } else {
                val invD = 1f / dx
                var t1 = (minX - x0) * invD
                var t2 = (maxX - x0) * invD
                if (t1 > t2) {
                    val tmp = t1; t1 = t2; t2 = tmp
                }
                tMin = max(tMin, t1)
                tMax = min(tMax, t2)
                if (tMin > tMax) continue
            }

            if (abs(dz) < 1e-5f) {
                if (z0 < minZ || z0 > maxZ) continue
            } else {
                val invD = 1f / dz
                var t1 = (minZ - z0) * invD
                var t2 = (maxZ - z0) * invD
                if (t1 > t2) {
                    val tmp = t1; t1 = t2; t2 = tmp
                }
                tMin = max(tMin, t1)
                tMax = min(tMax, t2)
                if (tMin > tMax) continue
            }

            if (tMin in 0.05f..bestT) {
                bestT = max(0.32f, tMin * 0.9f)
            }
        }
        return bestT
    }

    private fun lerpAngle(from: Float, to: Float, alpha: Float): Float {
        val diff = normalizeAngle(to - from)
        return normalizeAngle(from + diff * alpha)
    }

    private fun normalizeAngle(a: Float): Float {
        var res = a
        val twoPi = (2.0 * PI).toFloat()
        while (res > PI.toFloat()) res -= twoPi
        while (res < -PI.toFloat()) res += twoPi
        return res
    }
}
