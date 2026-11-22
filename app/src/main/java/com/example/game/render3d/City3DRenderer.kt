package com.example.game.render3d

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import com.example.data.WeaponType
import com.example.game.CityMap
import com.example.game.GameWorldState
import com.example.game.MissionObjectiveType
import com.example.game.PedRole
import com.example.game.PickupKind
import com.example.game.VehicleKind
import com.example.game.WorldPed
import com.example.game.WorldVehicle
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

class City3DRenderer(
    private val cameraController: CameraController,
) : GLSurfaceView.Renderer {

    @Volatile
    private var latestWorld: GameWorldState = GameWorldState()

    private var litShader: LitShader? = null
    private var skyShader: SkyShader? = null
    private var textures: TextureLibrary? = null
    private var geometry: CityGeometry? = null

    private val projMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val vpMatrix = FloatArray(16)
    private val modelMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)
    private val scratchMatrix = FloatArray(16)

    private val pointPos = FloatArray(12)
    private val pointColor = FloatArray(12)

    private var aspectRatio = 16f / 9f
    private var lastFrameNs = System.nanoTime()

    fun updateWorld(world: GameWorldState) {
        latestWorld = world
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.08f, 0.11f, 0.19f, 1f)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glDepthFunc(GLES20.GL_LEQUAL)

        litShader = LitShader()
        skyShader = SkyShader()
        textures = TextureLibrary()
        geometry = CityGeometry()
        lastFrameNs = System.nanoTime()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        aspectRatio = width.toFloat() / height.coerceAtLeast(1).toFloat()
    }

    override fun onDrawFrame(gl: GL10?) {
        val shader = litShader ?: return
        val sky = skyShader ?: return
        val tex = textures ?: return
        val geo = geometry ?: return

        val nowNs = System.nanoTime()
        val dt = ((nowNs - lastFrameNs) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
        lastFrameNs = nowNs

        val world = latestWorld
        cameraController.update(world, dt)

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        Matrix.perspectiveM(projMatrix, 0, cameraController.fovDeg, aspectRatio, 6f, 2800f)
        Matrix.setLookAtM(
            viewMatrix, 0,
            cameraController.camX, cameraController.camY, cameraController.camZ,
            cameraController.lookX, cameraController.lookY, cameraController.lookZ,
            0f, 1f, 0f,
        )
        Matrix.multiplyMM(vpMatrix, 0, projMatrix, 0, viewMatrix, 0)

        // 1. Atmospheric Sky Dome
        drawSkyDome(sky, geo, world.clockPulse)

        // 2. Setup Crisp High-Clarity World Lighting & Balanced Atmospheric Fog
        GLES20.glUseProgram(shader.program)
        GLES20.glUniform3f(shader.uCameraPos, cameraController.camX, cameraController.camY, cameraController.camZ)
        GLES20.glUniform3f(shader.uSunDir, -0.52f, 0.72f, -0.42f)
        GLES20.glUniform3f(shader.uSunColor, 1.04f, 0.90f, 0.78f)
        GLES20.glUniform3f(shader.uSkyAmbient, 0.35f, 0.42f, 0.56f)
        GLES20.glUniform3f(shader.uGroundAmbient, 0.20f, 0.19f, 0.23f)
        GLES20.glUniform3f(shader.uFogColor, 0.19f, 0.20f, 0.32f)
        GLES20.glUniform1f(shader.uFogDensity, 0.00040f)

        setupDynamicPointLights(world, shader)

        // 3. Opaque Static 3D Metropolis (batched VBOs)
        drawIdentityMesh(shader, geo.oceanMesh, tex.oceanWater, 1f, 1f, 1f, 1f, spec = 0.76f, shininess = 52f)
        drawIdentityMesh(shader, geo.beachMesh, tex.beachSand, 0.96f, 0.92f, 0.84f, 1f, spec = 0.08f, shininess = 8f)
        drawIdentityMesh(shader, geo.roadMesh, tex.roadAsphalt, 1f, 1f, 1f, 1f, spec = 0.38f, shininess = 32f)
        drawIdentityMesh(shader, geo.intersectionMesh, tex.intersectionAsphalt, 1f, 1f, 1f, 1f, spec = 0.38f, shininess = 32f)
        drawIdentityMesh(shader, geo.sidewalkMesh, tex.sidewalkConcrete, 0.98f, 0.98f, 1.0f, 1f, spec = 0.14f, shininess = 14f)
        drawIdentityMesh(shader, geo.sidewalkCurbMesh, tex.sidewalkConcrete, 1.05f, 1.04f, 1.02f, 1f, spec = 0.22f, shininess = 18f)

        // Storefronts, Awnings, Skyscrapers & Glowing Neon Architecture
        drawIdentityMesh(shader, geo.storefrontsMesh, tex.storefront, 1f, 0.98f, 0.95f, 1f, spec = 0.46f, shininess = 30f, emissive = 0.16f)
        drawIdentityMesh(shader, geo.storeAwningsMesh, tex.fabricWeave, 0.18f, 0.46f, 0.56f, 1f, spec = 0.18f, shininess = 14f)
        drawIdentityMesh(shader, geo.buildingsGlassMesh, tex.facadeGlass, 0.98f, 1.0f, 1.0f, 1f, spec = 0.72f, shininess = 48f, emissive = 0.12f)
        drawIdentityMesh(shader, geo.buildingsConcreteMesh, tex.facadeConcrete, 0.98f, 0.96f, 0.95f, 1f, spec = 0.16f, shininess = 16f, emissive = 0.07f)
        drawIdentityMesh(shader, geo.buildingsArtDecoMesh, tex.facadeArtDeco, 1.0f, 0.96f, 0.92f, 1f, spec = 0.24f, shininess = 22f, emissive = 0.09f)
        drawIdentityMesh(shader, geo.rooftopsMesh, tex.roofTar, 0.88f, 0.90f, 0.94f, 1f, spec = 0.12f, shininess = 12f)
        drawIdentityMesh(shader, geo.buildingNeonCyanMesh, 0, 0.06f, 0.94f, 1.0f, 1f, spec = 0.2f, shininess = 12f, emissive = 0.95f)
        drawIdentityMesh(shader, geo.buildingNeonMagentaMesh, 0, 1.0f, 0.20f, 0.62f, 1f, spec = 0.2f, shininess = 12f, emissive = 0.95f)
        drawIdentityMesh(shader, geo.buildingNeonGoldMesh, 0, 1.0f, 0.82f, 0.26f, 1f, spec = 0.2f, shininess = 12f, emissive = 0.95f)

        // Streetlights, Traffic Lights, Benches & Fire Hydrants
        drawIdentityMesh(shader, geo.lampPolesMesh, 0, 0.18f, 0.20f, 0.24f, 1f, spec = 0.45f, shininess = 30f)
        drawIdentityMesh(shader, geo.lampBulbsMesh, 0, 1.0f, 0.90f, 0.68f, 1f, spec = 0.1f, shininess = 8f, emissive = 0.96f)
        drawIdentityMesh(shader, geo.streetFurnitureMetalMesh, 0, 0.24f, 0.26f, 0.30f, 1f, spec = 0.54f, shininess = 34f)
        drawIdentityMesh(shader, geo.streetFurnitureWoodMesh, tex.palmBark, 0.78f, 0.58f, 0.40f, 1f, spec = 0.12f, shininess = 10f)
        drawIdentityMesh(shader, geo.hydrantRedMesh, 0, 0.88f, 0.14f, 0.16f, 1f, spec = 0.58f, shininess = 36f)
        drawIdentityMesh(shader, geo.trafficGreenGlowMesh, 0, 0.16f, 0.96f, 0.46f, 1f, spec = 0.1f, shininess = 8f, emissive = 0.95f)
        drawIdentityMesh(shader, geo.trafficRedGlowMesh, 0, 0.98f, 0.18f, 0.20f, 1f, spec = 0.1f, shininess = 8f, emissive = 0.95f)

        // Realistic Palm Trees (bark + serrated foliage)
        drawIdentityMesh(shader, geo.palmTrunksMesh, tex.palmBark, 0.94f, 0.88f, 0.80f, 1f, spec = 0.08f, shininess = 8f)
        drawIdentityMesh(shader, geo.palmFrondsMesh, tex.palmFrond, 0.98f, 1.0f, 0.94f, 1f, spec = 0.16f, shininess = 14f)

        // 4. Dynamic 3D Vehicles with Textured Alloy Rims
        for (veh in world.vehicles) {
            val dCam = hypot(veh.x - cameraController.camX, veh.y - cameraController.camZ)
            if (dCam > 1550f) continue
            drawVehicle3D(shader, geo, tex, veh)
        }

        // 5. Realistic Multi-Part Articulated 3D Cat Citizens, Cops & Player
        for (ped in world.peds) {
            val dCam = hypot(ped.x - cameraController.camX, ped.y - cameraController.camZ)
            if (dCam > 1150f) continue
            drawPed3D(shader, geo, tex, ped, world.clockPulse)
        }
        if (world.inVehicleId == null) {
            drawPlayer3D(shader, geo, tex, world)
        }

        // 6. Realistic 3D Pickups (Cash stack, Medkit case, Tactical Armor, Briefcase)
        for (pk in world.pickups) {
            val dCam = hypot(pk.x - cameraController.camX, pk.y - cameraController.camZ)
            if (dCam > 1100f) continue
            val bob = sin(world.clockPulse * 4.5f + pk.id) * 1.6f
            val spinDeg = (world.clockPulse * 85f + pk.id * 35f) % 360f
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, pk.x, 6.5f + bob, pk.y)
            Matrix.rotateM(modelMatrix, 0, spinDeg, 0f, 1f, 0f)
            when (pk.kind) {
                PickupKind.CASH -> submitMesh(shader, geo.cashStackMesh, tex.cashBill, 1f, 1f, 1f, 1f, spec = 0.20f, shininess = 16f, emissive = 0.18f)
                PickupKind.MEDKIT -> submitMesh(shader, geo.medkitMesh, tex.medkitCase, 1f, 1f, 1f, 1f, spec = 0.45f, shininess = 28f, emissive = 0.18f)
                PickupKind.ARMOR -> submitMesh(shader, geo.armorVestMesh, tex.fabricWeave, 0.18f, 0.62f, 0.85f, 1f, spec = 0.38f, shininess = 22f, emissive = 0.20f)
                PickupKind.BRIEFCASE -> submitMesh(shader, geo.briefcaseMesh, tex.briefcaseLeather, 1f, 1f, 1f, 1f, spec = 0.55f, shininess = 32f, emissive = 0.22f)
            }
        }

        // 7. Projectiles
        for (proj in world.projectiles) {
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, proj.x, 12.5f, proj.y)
            Matrix.scaleM(modelMatrix, 0, proj.radius * 0.55f, proj.radius * 0.55f, proj.radius * 0.55f)
            setHexTint(proj.colorHex, 1f)
            submitMesh(shader, geo.unitSphereMesh, 0, tintR, tintG, tintB, 1f, spec = 0.2f, shininess = 8f, emissive = 0.95f)
        }

        // 8. Transparent & Additive Volumetric Ground Pools, Shadows, Skid Marks, Beacons & Particles
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
        GLES20.glDepthMask(false)

        // Warm streetlight ground pools
        drawIdentityMesh(shader, geo.streetLightPoolsMesh, tex.lightPool, 1.0f, 0.88f, 0.68f, 0.42f, spec = 0f, shininess = 4f, emissive = 0.65f)

        // Skid marks
        for (skid in world.skidMarks) {
            val dx = skid.x2 - skid.x1
            val dz = skid.y2 - skid.y1
            val len = hypot(dx, dz)
            if (len < 1f) continue
            val angDeg = Math.toDegrees(kotlin.math.atan2(dz, dx).toDouble()).toFloat()
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, (skid.x1 + skid.x2) * 0.5f, 0.22f, (skid.y1 + skid.y2) * 0.5f)
            Matrix.rotateM(modelMatrix, 0, -angDeg, 0f, 1f, 0f)
            Matrix.scaleM(modelMatrix, 0, len + 2f, 1f, 4.2f)
            submitMesh(shader, geo.shadowQuadMesh, 0, 0.04f, 0.04f, 0.06f, skid.alpha * 0.65f, spec = 0f, shininess = 4f)
        }

        // Vehicle shadows + headlight ground beams + nearest car highlight
        for (veh in world.vehicles) {
            val dCam = hypot(veh.x - cameraController.camX, veh.y - cameraController.camZ)
            if (dCam > 1300f) continue
            val deg = Math.toDegrees(veh.angle.toDouble()).toFloat()
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, veh.x + 2.5f, 0.32f, veh.y + 3.5f)
            Matrix.rotateM(modelMatrix, 0, -deg, 0f, 1f, 0f)
            Matrix.scaleM(modelMatrix, 0, veh.kind.length * 1.12f, 1f, veh.kind.width * 1.18f)
            submitMesh(shader, geo.shadowQuadMesh, 0, 0.02f, 0.02f, 0.04f, 0.52f, spec = 0f, shininess = 4f)

            if (veh.id == world.nearestVehicleId) {
                val pulse = 0.25f + 0.12f * sin(world.clockPulse * 6f)
                Matrix.setIdentityM(modelMatrix, 0)
                Matrix.translateM(modelMatrix, 0, veh.x, 0.36f, veh.y)
                Matrix.rotateM(modelMatrix, 0, -deg, 0f, 1f, 0f)
                Matrix.scaleM(modelMatrix, 0, veh.kind.length * 1.24f, 1f, veh.kind.width * 1.32f)
                submitMesh(shader, geo.shadowQuadMesh, 0, 0f, 0.94f, 1f, pulse, spec = 0f, shininess = 4f, emissive = 0.6f)
            }

            if (abs(veh.speed) > 20f || veh.isPersonal || veh.isPolice) {
                val fwdX = cos(veh.angle)
                val fwdZ = sin(veh.angle)
                val beamDist = veh.kind.length * 0.5f + 38f
                Matrix.setIdentityM(modelMatrix, 0)
                Matrix.translateM(modelMatrix, 0, veh.x + fwdX * beamDist, 0.38f, veh.y + fwdZ * beamDist)
                Matrix.rotateM(modelMatrix, 0, -deg, 0f, 1f, 0f)
                Matrix.scaleM(modelMatrix, 0, 72f, 1f, veh.kind.width * 1.55f)
                submitMesh(shader, geo.shadowQuadMesh, tex.lightPool, 0.98f, 0.95f, 0.84f, 0.28f, spec = 0f, shininess = 4f, emissive = 0.8f)
            }
        }

        // Ped & Player soft circular ground shadows
        for (ped in world.peds) {
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, ped.x + 1.5f, 0.32f, ped.y + 2f)
            Matrix.scaleM(modelMatrix, 0, 14f, 1f, 14f)
            submitMesh(shader, geo.shadowQuadMesh, tex.lightPool, 0.02f, 0.02f, 0.04f, 0.55f, spec = 0f, shininess = 4f)
            if (ped.isMissionTarget) {
                Matrix.setIdentityM(modelMatrix, 0)
                Matrix.translateM(modelMatrix, 0, ped.x, 0.4f, ped.y)
                Matrix.scaleM(modelMatrix, 0, 16f, 38f, 16f)
                submitMesh(shader, geo.beaconCylinderMesh, 0, 1f, 0.22f, 0.35f, 0.28f, spec = 0f, shininess = 4f, emissive = 0.9f)
            }
        }
        if (world.inVehicleId == null) {
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, world.playerX + 1.5f, 0.32f, world.playerY + 2f)
            Matrix.scaleM(modelMatrix, 0, 16f, 1f, 16f)
            submitMesh(shader, geo.shadowQuadMesh, tex.lightPool, 0.02f, 0.02f, 0.04f, 0.6f, spec = 0f, shininess = 4f)
        }

        drawBeacons3D(shader, geo, world)

        for (pt in world.particles) {
            val alpha = (pt.lifeSec / pt.maxLifeSec).coerceIn(0f, 1f)
            val rad = pt.size * (0.4f + 0.8f * alpha)
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, pt.x, 8f + (1f - alpha) * 14f, pt.y)
            Matrix.scaleM(modelMatrix, 0, rad, rad, rad)
            setHexTint(pt.colorHex, alpha * 0.88f)
            submitMesh(shader, geo.unitSphereMesh, 0, tintR, tintG, tintB, alpha * 0.88f, spec = 0f, shininess = 4f, emissive = 0.9f)
        }

        GLES20.glDepthMask(true)
        GLES20.glDisable(GLES20.GL_BLEND)
    }

    private fun drawSkyDome(sky: SkyShader, geo: CityGeometry, time: Float) {
        GLES20.glDepthMask(false)
        GLES20.glDisable(GLES20.GL_DEPTH_TEST)
        GLES20.glUseProgram(sky.program)

        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, cameraController.camX, cameraController.camY, cameraController.camZ)
        Matrix.scaleM(modelMatrix, 0, 1800f, 1800f, 1800f)
        Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0)
        GLES20.glUniformMatrix4fv(sky.uMVP, 1, false, mvpMatrix, 0)
        GLES20.glUniform1f(sky.uTime, time)

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, geo.skyDomeMesh.vboId)
        val stride = GpuMesh.FLOATS_PER_VERTEX * 4
        GLES20.glEnableVertexAttribArray(sky.aPosition)
        GLES20.glVertexAttribPointer(sky.aPosition, 3, GLES20.GL_FLOAT, false, stride, 0)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, geo.skyDomeMesh.vertexCount)

        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glDepthMask(true)
    }

    private fun setupDynamicPointLights(world: GameWorldState, shader: LitShader) {
        val hAngle = world.playerAngle
        val hDist = if (world.inVehicleId != null) 62f else 18f
        pointPos[0] = world.playerX + cos(hAngle) * hDist
        pointPos[1] = 16f
        pointPos[2] = world.playerY + sin(hAngle) * hDist
        if (world.inVehicleId != null) {
            pointColor[0] = 1.65f; pointColor[1] = 1.55f; pointColor[2] = 1.32f
        } else {
            pointColor[0] = 0.55f; pointColor[1] = 0.65f; pointColor[2] = 0.85f
        }

        val nearbyPolice = world.vehicles.filter { it.isPolice }
            .sortedBy { hypot(it.x - world.playerX, it.y - world.playerY) }
        for (slot in 1..2) {
            val idx = slot - 1
            val cop = nearbyPolice.getOrNull(idx)
            if (cop != null && hypot(cop.x - world.playerX, cop.y - world.playerY) < 700f) {
                pointPos[slot * 3] = cop.x
                pointPos[slot * 3 + 1] = 24f
                pointPos[slot * 3 + 2] = cop.y
                val redOn = sin(cop.sirenPhase) > 0f
                if (redOn) {
                    pointColor[slot * 3] = 1.85f; pointColor[slot * 3 + 1] = 0.18f; pointColor[slot * 3 + 2] = 0.25f
                } else {
                    pointColor[slot * 3] = 0.18f; pointColor[slot * 3 + 1] = 0.55f; pointColor[slot * 3 + 2] = 2.0f
                }
            } else {
                pointPos[slot * 3] = world.playerX
                pointPos[slot * 3 + 1] = -900f
                pointPos[slot * 3 + 2] = world.playerY
                pointColor[slot * 3] = 0f; pointColor[slot * 3 + 1] = 0f; pointColor[slot * 3 + 2] = 0f
            }
        }

        val brightPt = world.particles.firstOrNull()
        if (brightPt != null) {
            pointPos[9] = brightPt.x
            pointPos[10] = 18f
            pointPos[11] = brightPt.y
            setHexTint(brightPt.colorHex, 1f)
            pointColor[9] = tintR * 1.6f; pointColor[10] = tintG * 1.6f; pointColor[11] = tintB * 1.6f
        } else {
            pointPos[9] = CityMap.SAFE_HOUSE_X
            pointPos[10] = 22f
            pointPos[11] = CityMap.SAFE_HOUSE_Y + 90f
            pointColor[9] = 0.2f; pointColor[10] = 0.85f; pointColor[11] = 0.95f
        }

        GLES20.glUniform3fv(shader.uPointPos, 4, pointPos, 0)
        GLES20.glUniform3fv(shader.uPointColor, 4, pointColor, 0)
    }

    private fun drawVehicle3D(shader: LitShader, geo: CityGeometry, tex: TextureLibrary, veh: WorldVehicle) {
        val model = geo.vehicleModels[veh.kind] ?: return
        val deg = Math.toDegrees(veh.angle.toDouble()).toFloat()

        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, veh.x, 0f, veh.y)
        Matrix.rotateM(modelMatrix, 0, -deg, 0f, 1f, 0f)

        setHexTint(veh.customBodyHex ?: veh.kind.bodyHex, 1f)
        submitMesh(shader, model.chassis, tex.carPaint, tintR, tintG, tintB, 1f, spec = 0.94f, shininess = 72f)
        submitMesh(shader, model.glass, 0, 0.08f, 0.12f, 0.19f, 1f, spec = 0.96f, shininess = 96f)

        if (veh.kind == VehicleKind.POLICE) {
            submitMesh(shader, model.trim, 0, 0.94f, 0.95f, 0.98f, 1f, spec = 0.7f, shininess = 48f)
        } else {
            submitMesh(shader, model.trim, 0, 0.74f, 0.78f, 0.84f, 1f, spec = 0.85f, shininess = 64f)
        }

        model.accent?.let { accentMesh ->
            setHexTint(veh.kind.stripeHex, 1f)
            submitMesh(shader, accentMesh, 0, tintR, tintG, tintB, 1f, spec = 0.82f, shininess = 56f)
        }

        submitMesh(shader, model.headlights, 0, 0.98f, 0.96f, 0.86f, 1f, spec = 0.2f, shininess = 8f, emissive = 0.95f)
        submitMesh(shader, model.taillights, 0, 0.96f, 0.12f, 0.18f, 1f, spec = 0.2f, shininess = 8f, emissive = 0.85f)

        model.roofSign?.let {
            submitMesh(shader, it, 0, 1.0f, 0.82f, 0.22f, 1f, spec = 0.2f, shininess = 12f, emissive = 0.75f)
        }
        if (veh.isPolice && model.leftLightbar != null && model.rightLightbar != null) {
            val redOn = sin(veh.sirenPhase) > 0f
            if (redOn) {
                submitMesh(shader, model.leftLightbar, 0, 1.0f, 0.16f, 0.22f, 1f, spec = 0.3f, shininess = 16f, emissive = 0.98f)
                submitMesh(shader, model.rightLightbar, 0, 0.15f, 0.42f, 1.0f, 1f, spec = 0.3f, shininess = 16f, emissive = 0.35f)
            } else {
                submitMesh(shader, model.leftLightbar, 0, 1.0f, 0.16f, 0.22f, 1f, spec = 0.3f, shininess = 16f, emissive = 0.35f)
                submitMesh(shader, model.rightLightbar, 0, 0.15f, 0.42f, 1.0f, 1f, spec = 0.3f, shininess = 16f, emissive = 0.98f)
            }
        }

        val wheelSteerDeg = Math.toDegrees(veh.steerAngle.toDouble()).toFloat().coerceIn(-30f, 30f)
        val spinDeg = Math.toDegrees(veh.wheelSpin.toDouble()).toFloat()
        val wheelCoords = listOf(
            Triple(model.frontWheelX, -model.wheelZ, true),
            Triple(model.frontWheelX, model.wheelZ, true),
            Triple(model.rearWheelX, -model.wheelZ, false),
            Triple(model.rearWheelX, model.wheelZ, false),
        )
        for ((wx, wz, isFront) in wheelCoords) {
            Matrix.setIdentityM(scratchMatrix, 0)
            Matrix.translateM(scratchMatrix, 0, veh.x, 0f, veh.y)
            Matrix.rotateM(scratchMatrix, 0, -deg, 0f, 1f, 0f)
            Matrix.translateM(scratchMatrix, 0, wx, model.wheelRadius, wz)
            if (isFront) {
                Matrix.rotateM(scratchMatrix, 0, -wheelSteerDeg, 0f, 1f, 0f)
            }
            Matrix.rotateM(scratchMatrix, 0, -spinDeg, 0f, 0f, 1f)
            System.arraycopy(scratchMatrix, 0, modelMatrix, 0, 16)
            submitMesh(shader, geo.wheelMesh, tex.tireWheel, 1f, 1f, 1f, 1f, spec = 0.48f, shininess = 32f)
        }
    }

    private fun drawPed3D(shader: LitShader, geo: CityGeometry, tex: TextureLibrary, ped: WorldPed, clockPulse: Float) {
        val furPalettes = arrayOf(
            Triple(0.78f, 0.60f, 0.44f), // Coklat Cream
            Triple(0.84f, 0.70f, 0.54f), // Warm Cream Caramel
            Triple(0.18f, 0.19f, 0.23f), // Tuxedo Black
            Triple(0.90f, 0.84f, 0.72f), // Siamese Cream
            Triple(0.50f, 0.56f, 0.64f), // Russian Blue
            Triple(0.92f, 0.92f, 0.95f), // Snow White
        )
        val (furR, furG, furB) = when (ped.role) {
            PedRole.POLICE_OFFICER -> Triple(0.52f, 0.58f, 0.68f) // Blue-gray Police Cat
            PedRole.SYNDICATE_GUARD -> Triple(0.24f, 0.22f, 0.26f) // Tough Alley Cat
            PedRole.CIVILIAN -> furPalettes[ped.id % furPalettes.size]
        }

        val (coatR, coatG, coatB) = when (ped.role) {
            PedRole.CIVILIAN -> when (ped.id % 5) {
                0 -> Triple(0.94f, 0.88f, 0.76f) // Cream jacket
                1 -> Triple(0.88f, 0.81f, 0.69f) // Light cream-sand coat
                2 -> Triple(0.24f, 0.48f, 0.42f)
                3 -> Triple(0.58f, 0.28f, 0.32f)
                else -> Triple(0.32f, 0.34f, 0.38f)
            }
            PedRole.SYNDICATE_GUARD -> Triple(0.52f, 0.12f, 0.18f)
            PedRole.POLICE_OFFICER -> Triple(0.12f, 0.24f, 0.52f)
        }
        val (pantsR, pantsG, pantsB) = when (ped.role) {
            PedRole.POLICE_OFFICER -> Triple(0.10f, 0.16f, 0.34f)
            PedRole.SYNDICATE_GUARD -> Triple(0.14f, 0.14f, 0.16f)
            PedRole.CIVILIAN -> if (ped.id % 2 == 0) Triple(0.88f, 0.82f, 0.70f) else Triple(0.28f, 0.24f, 0.20f)
        }

        val deg = Math.toDegrees(ped.angle.toDouble()).toFloat()
        val swing = if (abs(ped.walkPhase) > 0.001f) sin(ped.walkPhase) * 28f else 0f
        val tailSway = sin(clockPulse * 3.4f + ped.id * 1.1f) * 14f
        drawRealisticCatCharacter(
            shader = shader,
            geo = geo,
            tex = tex,
            x = ped.x,
            z = ped.y,
            deg = deg,
            swingDeg = swing,
            tailSwayDeg = tailSway,
            coatR = coatR, coatG = coatG, coatB = coatB,
            pantsR = pantsR, pantsG = pantsG, pantsB = pantsB,
            furR = furR, furG = furG, furB = furB,
            weaponType = if (ped.role != PedRole.CIVILIAN) WeaponType.PISTOL else WeaponType.FISTS,
            isPoliceCap = ped.role == PedRole.POLICE_OFFICER,
            hasSunglasses = ped.role == PedRole.SYNDICATE_GUARD,
            isPunching = false,
        )
    }

    private fun drawPlayer3D(shader: LitShader, geo: CityGeometry, tex: TextureLibrary, world: GameWorldState) {
        val deg = Math.toDegrees(world.playerAngle.toDouble()).toFloat()
        val swing = if (world.isPlayerMoving) sin(world.playerWalkPhase) * 26f else 0f
        val tailSway = sin(world.clockPulse * (if (world.isPlayerMoving) 5.5f else 3.0f)) * 16f
        val isPunching = world.equippedWeapon.isMelee && world.fireCooldownSec > 0.08f
        drawRealisticCatCharacter(
            shader = shader,
            geo = geo,
            tex = tex,
            x = world.playerX,
            z = world.playerY,
            deg = deg,
            swingDeg = swing,
            tailSwayDeg = tailSway,
            coatR = 0.95f, coatG = 0.89f, coatB = 0.77f, // Warm cream jacket
            pantsR = 0.89f, pantsG = 0.82f, pantsB = 0.70f, // Soft cream trousers
            furR = 0.78f, furG = 0.60f, furB = 0.44f, // Coklat Cream Hero Cat
            weaponType = world.equippedWeapon,
            isPoliceCap = false,
            hasSunglasses = true,
            isPunching = isPunching,
        )
    }

    private fun drawRealisticCatCharacter(
        shader: LitShader,
        geo: CityGeometry,
        tex: TextureLibrary,
        x: Float,
        z: Float,
        deg: Float,
        swingDeg: Float,
        tailSwayDeg: Float,
        coatR: Float, coatG: Float, coatB: Float,
        pantsR: Float, pantsG: Float, pantsB: Float,
        furR: Float, furG: Float, furB: Float,
        weaponType: WeaponType,
        isPoliceCap: Boolean,
        hasSunglasses: Boolean,
        isPunching: Boolean,
    ) {
        val hasWeapon = !weaponType.isMelee
        // Sculpted Tailored Jacket / Torso + Inner Undershirt Panel
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, x, 11.4f, z)
        Matrix.rotateM(modelMatrix, 0, -deg, 0f, 1f, 0f)
        submitMesh(shader, geo.humanTorsoMesh, tex.fabricWeave, coatR, coatG, coatB, 1f, spec = 0.24f, shininess = 20f)
        submitMesh(shader, geo.humanInnerShirtMesh, tex.fabricWeave, (coatR * 1.04f).coerceAtMost(1f), (coatG * 1.02f).coerceAtMost(1f), (coatB * 0.98f).coerceAtMost(1f), 1f, spec = 0.16f, shininess = 14f)

        // Cat Collar & Gold Tag at neck
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, x, 15.1f, z)
        Matrix.rotateM(modelMatrix, 0, -deg, 0f, 1f, 0f)
        submitMesh(shader, geo.catCollarMesh, 0, 0.86f, 0.64f, 0.22f, 1f, spec = 0.75f, shininess = 48f)

        // Leather Belt with Buckle
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, x, 7.9f, z)
        Matrix.rotateM(modelMatrix, 0, -deg, 0f, 1f, 0f)
        submitMesh(shader, geo.humanBeltMesh, 0, 0.22f, 0.15f, 0.10f, 1f, spec = 0.45f, shininess = 28f)

        // Swishing Feline Tail + Lighter Cream Tail Tip attached at hip
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, x, 8.2f, z)
        Matrix.rotateM(modelMatrix, 0, -deg + tailSwayDeg, 0f, 1f, 0f)
        submitMesh(shader, geo.catTailMesh, tex.catFur, furR, furG, furB, 1f, spec = 0.14f, shininess = 12f)
        submitMesh(shader, geo.catTailTipMesh, tex.catFur, (furR + 0.18f).coerceAtMost(1f), (furG + 0.18f).coerceAtMost(1f), (furB + 0.18f).coerceAtMost(1f), 1f, spec = 0.14f, shininess = 12f)

        // Realistic Feline Head (Crisp Cat Eyes, Nose, Whiskers & Forehead Tabby Stripes)
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, x, 17.6f, z)
        Matrix.rotateM(modelMatrix, 0, -deg, 0f, 1f, 0f)
        Matrix.scaleM(modelMatrix, 0, 1.06f, 0.95f, 1.08f)
        submitMesh(shader, geo.humanHeadMesh, tex.catFace, furR, furG, furB, 1f, spec = 0.14f, shininess = 12f)

        // Cream Feline Muzzle Snout
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, x, 17.3f, z)
        Matrix.rotateM(modelMatrix, 0, -deg, 0f, 1f, 0f)
        submitMesh(shader, geo.catMuzzleMesh, tex.catFur, (furR + 0.35f).coerceAtMost(1f), (furG + 0.35f).coerceAtMost(1f), (furB + 0.35f).coerceAtMost(1f), 1f, spec = 0.12f, shininess = 10f)

        // Pointed 3D Cat Ears + Pink Inner Ears
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, x, 18.0f, z)
        Matrix.rotateM(modelMatrix, 0, -deg, 0f, 1f, 0f)
        submitMesh(shader, geo.catEarOuterMesh, tex.catFur, furR * 0.92f, furG * 0.92f, furB * 0.92f, 1f, spec = 0.14f, shininess = 12f)
        submitMesh(shader, geo.catEarInnerMesh, 0, 0.96f, 0.64f, 0.72f, 1f, spec = 0.10f, shininess = 10f)

        // Police Visor Cap (perched cleanly between the ears) & Sunglasses
        if (isPoliceCap) {
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, x, 18.8f, z)
            Matrix.rotateM(modelMatrix, 0, -deg, 0f, 1f, 0f)
            Matrix.scaleM(modelMatrix, 0, 0.92f, 0.85f, 0.76f)
            submitMesh(shader, geo.humanPoliceCapMesh, tex.fabricWeave, 0.10f, 0.18f, 0.38f, 1f, spec = 0.35f, shininess = 28f)
        }
        if (hasSunglasses) {
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, x, 17.8f, z)
            Matrix.rotateM(modelMatrix, 0, -deg, 0f, 1f, 0f)
            submitMesh(shader, geo.humanSunglassesMesh, 0, 0.06f, 0.07f, 0.10f, 1f, spec = 0.95f, shininess = 80f)
        }

        // Tapered Trousers + Sneakers with White Soles (Left & Right) — stops completely when swingDeg == 0f
        for (side in listOf(-1f, 1f)) {
            val legAngle = swingDeg * side
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, x, 7.6f, z)
            Matrix.rotateM(modelMatrix, 0, -deg, 0f, 1f, 0f)
            Matrix.translateM(modelMatrix, 0, 0f, 0f, side * 1.95f)
            Matrix.rotateM(modelMatrix, 0, legAngle, 0f, 0f, 1f)
            submitMesh(shader, geo.humanLegMesh, tex.fabricWeave, pantsR, pantsG, pantsB, 1f, spec = 0.10f, shininess = 10f)

            // Sneaker upper + crisp cream/white sole attached to lower leg
            Matrix.translateM(modelMatrix, 0, 0f, -7.0f, 0f)
            submitMesh(shader, geo.humanShoeMesh, 0, 0.24f, 0.20f, 0.18f, 1f, spec = 0.36f, shininess = 28f)
            submitMesh(shader, geo.humanShoeSoleMesh, 0, 0.94f, 0.92f, 0.88f, 1f, spec = 0.25f, shininess = 18f)
        }

        // Left Arm (Jacket Sleeve + Furry Paw Forearm)
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, x, 14.3f, z)
        Matrix.rotateM(modelMatrix, 0, -deg, 0f, 1f, 0f)
        Matrix.translateM(modelMatrix, 0, 0f, 0f, -4.7f)
        Matrix.rotateM(modelMatrix, 0, -swingDeg, 0f, 0f, 1f)
        submitMesh(shader, geo.humanUpperArmMesh, tex.fabricWeave, coatR * 0.94f, coatG * 0.94f, coatB * 0.94f, 1f, spec = 0.18f, shininess = 14f)
        Matrix.translateM(modelMatrix, 0, 0f, -3.4f, 0f)
        submitMesh(shader, geo.humanForearmMesh, tex.catFur, furR, furG, furB, 1f, spec = 0.14f, shininess = 12f)

        // Right Arm (+ Weapon or Claw Punch)
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, x, 14.3f, z)
        Matrix.rotateM(modelMatrix, 0, -deg, 0f, 1f, 0f)
        Matrix.translateM(modelMatrix, 0, 0f, 0f, 4.7f)
        when {
            hasWeapon -> Matrix.rotateM(modelMatrix, 0, 82f, 0f, 0f, 1f)
            isPunching -> Matrix.rotateM(modelMatrix, 0, 86f, 0f, 0f, 1f)
            else -> Matrix.rotateM(modelMatrix, 0, swingDeg, 0f, 0f, 1f)
        }
        submitMesh(shader, geo.humanUpperArmMesh, tex.fabricWeave, coatR * 0.94f, coatG * 0.94f, coatB * 0.94f, 1f, spec = 0.18f, shininess = 14f)
        Matrix.translateM(modelMatrix, 0, 0f, -3.4f, 0f)
        submitMesh(shader, geo.humanForearmMesh, tex.catFur, furR, furG, furB, 1f, spec = 0.14f, shininess = 12f)

        if (hasWeapon) {
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, x, 13.0f, z)
            Matrix.rotateM(modelMatrix, 0, -deg, 0f, 1f, 0f)
            Matrix.translateM(modelMatrix, 0, 5.2f, 0f, 4.4f)
            when (weaponType) {
                WeaponType.PISTOL -> {
                    submitMesh(shader, geo.weaponPistolMesh, 0, 0.22f, 0.24f, 0.28f, 1f, spec = 0.80f, shininess = 58f)
                }
                WeaponType.SMG -> {
                    submitMesh(shader, geo.weaponSmgMesh, 0, 0.14f, 0.16f, 0.22f, 1f, spec = 0.84f, shininess = 62f)
                }
                WeaponType.SHOTGUN -> {
                    submitMesh(shader, geo.weaponShotgunMesh, 0, 0.36f, 0.24f, 0.16f, 1f, spec = 0.65f, shininess = 42f)
                }
                WeaponType.PLASMA -> {
                    submitMesh(shader, geo.weaponPlasmaMesh, 0, 0.18f, 0.22f, 0.30f, 1f, spec = 0.88f, shininess = 72f)
                    submitMesh(shader, geo.weaponPlasmaCoreMesh, 0, 0.08f, 0.92f, 1.0f, 1f, spec = 0.95f, shininess = 90f, emissive = 0.92f)
                }
                WeaponType.FISTS -> Unit
            }
        }
    }

    private fun drawBeacons3D(shader: LitShader, geo: CityGeometry, world: GameWorldState) {
        val pulse = 0.22f + 0.10f * sin(world.clockPulse * 5f)
        drawBeaconPillar(shader, geo, CityMap.SAFE_HOUSE_X, CityMap.SAFE_HOUSE_Y + 90f, 24f, 42f, 0f, 0.94f, 1f, pulse)
        for (pad in CityMap.payNSprayPads) {
            drawBeaconPillar(shader, geo, pad.x, pad.y, 32f, 52f, 0.18f, 0.90f, 0.55f, pulse)
        }
        val active = world.activeMission
        if (active == null) {
            for (m in CityMap.missions) {
                drawBeaconPillar(shader, geo, m.markerX, m.markerY, 20f, 110f, 1f, 0.78f, 0.24f, pulse + 0.08f)
            }
        } else if (active.template.type != MissionObjectiveType.ELIMINATE_SYNDICATE &&
            active.template.type != MissionObjectiveType.SURVIVE_HEAT
        ) {
            drawBeaconPillar(shader, geo, active.targetX, active.targetY, 26f, 130f, 1f, 0.18f, 0.58f, pulse + 0.12f)
        }
    }

    private fun drawBeaconPillar(
        shader: LitShader,
        geo: CityGeometry,
        x: Float,
        z: Float,
        radius: Float,
        height: Float,
        r: Float,
        g: Float,
        b: Float,
        alpha: Float,
    ) {
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, x, 0.4f, z)
        Matrix.scaleM(modelMatrix, 0, radius, height, radius)
        submitMesh(shader, geo.beaconCylinderMesh, 0, r, g, b, alpha, spec = 0f, shininess = 4f, emissive = 0.92f)
    }

    private fun drawIdentityMesh(
        shader: LitShader,
        mesh: GpuMesh,
        textureId: Int,
        r: Float,
        g: Float,
        b: Float,
        a: Float,
        spec: Float,
        shininess: Float,
        emissive: Float = 0f,
    ) {
        Matrix.setIdentityM(modelMatrix, 0)
        submitMesh(shader, mesh, textureId, r, g, b, a, spec, shininess, emissive)
    }

    private fun submitMesh(
        shader: LitShader,
        mesh: GpuMesh,
        textureId: Int,
        r: Float,
        g: Float,
        b: Float,
        a: Float,
        spec: Float,
        shininess: Float,
        emissive: Float = 0f,
    ) {
        Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0)
        GLES20.glUniformMatrix4fv(shader.uMVP, 1, false, mvpMatrix, 0)
        GLES20.glUniformMatrix4fv(shader.uModel, 1, false, modelMatrix, 0)
        GLES20.glUniform4f(shader.uTintColor, r, g, b, a)
        GLES20.glUniform1f(shader.uSpecularStrength, spec)
        GLES20.glUniform1f(shader.uShininess, shininess)
        GLES20.glUniform3f(shader.uEmissive, r * emissive, g * emissive, b * emissive)

        if (textureId != 0) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
            GLES20.glUniform1i(shader.uTexture, 0)
            GLES20.glUniform1f(shader.uUseTexture, 1f)
        } else {
            GLES20.glUniform1f(shader.uUseTexture, 0f)
        }

        mesh.bindAndDraw(shader)
    }

    private var tintR = 1f
    private var tintG = 1f
    private var tintB = 1f

    private fun setHexTint(hex: Long, scale: Float) {
        tintR = (((hex shr 16) and 0xFF) / 255f) * scale
        tintG = (((hex shr 8) and 0xFF) / 255f) * scale
        tintB = ((hex and 0xFF) / 255f) * scale
    }

    fun release() {
        textures?.release()
        geometry?.release()
        textures = null
        geometry = null
    }
}
