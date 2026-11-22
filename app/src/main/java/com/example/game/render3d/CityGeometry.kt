package com.example.game.render3d

import com.example.game.CityMap
import com.example.game.District
import com.example.game.VehicleKind
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class VehicleModelSet(
    val chassis: GpuMesh,
    val glass: GpuMesh,
    val trim: GpuMesh,
    val accent: GpuMesh?,
    val headlights: GpuMesh,
    val taillights: GpuMesh,
    val leftLightbar: GpuMesh?,
    val rightLightbar: GpuMesh?,
    val roofSign: GpuMesh?,
    val wheelRadius: Float,
    val frontWheelX: Float,
    val rearWheelX: Float,
    val wheelZ: Float,
) {
    fun release() {
        chassis.release()
        glass.release()
        trim.release()
        accent?.release()
        headlights.release()
        taillights.release()
        leftLightbar?.release()
        rightLightbar?.release()
        roofSign?.release()
    }
}

class CityGeometry {
    val oceanMesh: GpuMesh
    val beachMesh: GpuMesh
    val roadMesh: GpuMesh
    val intersectionMesh: GpuMesh
    val sidewalkMesh: GpuMesh
    val sidewalkCurbMesh: GpuMesh
    val storefrontsMesh: GpuMesh
    val storeAwningsMesh: GpuMesh
    val buildingsGlassMesh: GpuMesh
    val buildingsConcreteMesh: GpuMesh
    val buildingsArtDecoMesh: GpuMesh
    val rooftopsMesh: GpuMesh
    val buildingNeonCyanMesh: GpuMesh
    val buildingNeonMagentaMesh: GpuMesh
    val buildingNeonGoldMesh: GpuMesh
    val lampPolesMesh: GpuMesh
    val lampBulbsMesh: GpuMesh
    val streetLightPoolsMesh: GpuMesh
    val streetFurnitureMetalMesh: GpuMesh
    val streetFurnitureWoodMesh: GpuMesh
    val hydrantRedMesh: GpuMesh
    val trafficGreenGlowMesh: GpuMesh
    val trafficRedGlowMesh: GpuMesh
    val palmTrunksMesh: GpuMesh
    val palmFrondsMesh: GpuMesh
    val skyDomeMesh: GpuMesh

    val wheelMesh: GpuMesh
    val shadowQuadMesh: GpuMesh
    val unitBoxMesh: GpuMesh
    val unitSphereMesh: GpuMesh
    val beaconCylinderMesh: GpuMesh

    // Realistic 3D Pickup Objects
    val cashStackMesh: GpuMesh
    val medkitMesh: GpuMesh
    val armorVestMesh: GpuMesh
    val briefcaseMesh: GpuMesh

    // Realistic Articulated 3D Anatomy & Layered Tailored Outfit
    val humanTorsoMesh: GpuMesh
    val humanInnerShirtMesh: GpuMesh
    val humanBeltMesh: GpuMesh
    val humanHeadMesh: GpuMesh
    val humanHairMesh: GpuMesh
    val humanPoliceCapMesh: GpuMesh
    val humanSunglassesMesh: GpuMesh
    val humanUpperArmMesh: GpuMesh
    val humanForearmMesh: GpuMesh
    val humanLegMesh: GpuMesh
    val humanShoeMesh: GpuMesh
    val humanShoeSoleMesh: GpuMesh

    // Distinct Detailed 3D Weapon Models
    val weaponPistolMesh: GpuMesh
    val weaponSmgMesh: GpuMesh
    val weaponShotgunMesh: GpuMesh
    val weaponPlasmaMesh: GpuMesh
    val weaponPlasmaCoreMesh: GpuMesh

    // Realistic 3D Feline Anatomy (Ears, Inner Ears, Muzzle, Tail, Tail Tip, Collar)
    val catEarOuterMesh: GpuMesh
    val catEarInnerMesh: GpuMesh
    val catMuzzleMesh: GpuMesh
    val catTailMesh: GpuMesh
    val catTailTipMesh: GpuMesh
    val catCollarMesh: GpuMesh

    val vehicleModels: Map<VehicleKind, VehicleModelSet>

    init {
        // 1. Ocean & Coastal Seawall + Promenade Guardrail
        val oceanBuilder = MeshBuilder(1024)
        oceanBuilder.addQuad(
            -1600f, -2.8f, 4400f, 0f, 18f,
            18f, -2.8f, 4400f, 6f, 18f,
            18f, -2.8f, -1000f, 6f, 0f,
            -1600f, -2.8f, -1000f, 0f, 0f,
            0f, 1f, 0f,
        )
        oceanMesh = oceanBuilder.build()

        val beachBuilder = MeshBuilder(1024)
        beachBuilder.addBox(20f, -1.2f, CityMap.WORLD_SIZE / 2f, 42f, 2.2f, CityMap.WORLD_SIZE + 800f, uvScaleX = 2f, uvScaleY = 30f)
        beachMesh = beachBuilder.build()

        // 2. Roads & Intersections
        val roadBuilder = MeshBuilder(16384)
        val interBuilder = MeshBuilder(8192)
        val halfRoad = CityMap.ROAD_WIDTH / 2f

        roadBuilder.addQuad(
            0f, -0.3f, CityMap.WORLD_SIZE, 0f, 1f,
            CityMap.WORLD_SIZE, -0.3f, CityMap.WORLD_SIZE, 1f, 1f,
            CityMap.WORLD_SIZE, -0.3f, 0f, 1f, 0f,
            0f, -0.3f, 0f, 0f, 0f,
            0f, 1f, 0f,
        )

        val centers = CityMap.roadCenters
        for (cx in centers) {
            for (cz in centers) {
                interBuilder.addQuad(
                    cx - halfRoad, 0.05f, cz + halfRoad, 0f, 1f,
                    cx + halfRoad, 0.05f, cz + halfRoad, 1f, 1f,
                    cx + halfRoad, 0.05f, cz - halfRoad, 1f, 0f,
                    cx - halfRoad, 0.05f, cz - halfRoad, 0f, 0f,
                    0f, 1f, 0f,
                )
            }
        }

        for (i in centers.indices) {
            val rc = centers[i]
            for (j in 0 until centers.size - 1) {
                val z0 = centers[j] + halfRoad
                val z1 = centers[j + 1] - halfRoad
                val segLen = z1 - z0
                val repeatV = segLen / 180f
                roadBuilder.addQuad(
                    rc - halfRoad, 0.04f, z1, 0f, repeatV,
                    rc + halfRoad, 0.04f, z1, 1f, repeatV,
                    rc + halfRoad, 0.04f, z0, 1f, 0f,
                    rc - halfRoad, 0.04f, z0, 0f, 0f,
                    0f, 1f, 0f,
                )
            }
            for (j in 0 until centers.size - 1) {
                val x0 = centers[j] + halfRoad
                val x1 = centers[j + 1] - halfRoad
                val segLen = x1 - x0
                val repeatU = segLen / 180f
                roadBuilder.addQuad(
                    x0, 0.04f, rc + halfRoad, 1f, 0f,
                    x1, 0.04f, rc + halfRoad, 1f, repeatU,
                    x1, 0.04f, rc - halfRoad, 0f, repeatU,
                    x0, 0.04f, rc - halfRoad, 0f, 0f,
                    0f, 1f, 0f,
                )
            }
        }
        roadMesh = roadBuilder.build()
        intersectionMesh = interBuilder.build()

        // 3. Raised Concrete City Block Sidewalks + Distinct Granite Curb Borders
        val sidewalkBuilder = MeshBuilder(16384)
        val curbBuilder = MeshBuilder(16384)
        for (gx in 0 until CityMap.GRID_COUNT) {
            for (gy in 0 until CityMap.GRID_COUNT) {
                val left = CityMap.ROAD_WIDTH + gx * CityMap.CELL_STEP
                val top = CityMap.ROAD_WIDTH + gy * CityMap.CELL_STEP
                val cx = left + CityMap.BLOCK_SIZE / 2f
                val cz = top + CityMap.BLOCK_SIZE / 2f
                sidewalkBuilder.addBox(
                    cx = cx,
                    cy = 0.85f,
                    cz = cz,
                    sx = CityMap.BLOCK_SIZE - 4f,
                    sy = 1.7f,
                    sz = CityMap.BLOCK_SIZE - 4f,
                    uvScaleX = 6f,
                    uvScaleY = 0.1f,
                )
                // Raised light-concrete curbstone frame around each block
                val curbThickness = 4.0f
                val curbH = 2.0f
                val halfB = CityMap.BLOCK_SIZE / 2f
                curbBuilder.addBox(cx, curbH / 2f, top + curbThickness / 2f, CityMap.BLOCK_SIZE, curbH, curbThickness)
                curbBuilder.addBox(cx, curbH / 2f, top + CityMap.BLOCK_SIZE - curbThickness / 2f, CityMap.BLOCK_SIZE, curbH, curbThickness)
                curbBuilder.addBox(left + curbThickness / 2f, curbH / 2f, cz, curbThickness, curbH, CityMap.BLOCK_SIZE)
                curbBuilder.addBox(left + CityMap.BLOCK_SIZE - curbThickness / 2f, curbH / 2f, cz, curbThickness, curbH, CityMap.BLOCK_SIZE)

                // Corner yellow tactile curb warning pads
                curbBuilder.addBox(left + 7f, 1.85f, top + 7f, 10f, 0.4f, 10f)
                curbBuilder.addBox(left + CityMap.BLOCK_SIZE - 7f, 1.85f, top + 7f, 10f, 0.4f, 10f)
                curbBuilder.addBox(left + 7f, 1.85f, top + CityMap.BLOCK_SIZE - 7f, 10f, 0.4f, 10f)
                curbBuilder.addBox(left + CityMap.BLOCK_SIZE - 7f, 1.85f, top + CityMap.BLOCK_SIZE - 7f, 10f, 0.4f, 10f)
                // Keep unused variable warning away
                if (halfB < 0f) break
            }
        }
        sidewalkMesh = sidewalkBuilder.build()
        sidewalkCurbMesh = curbBuilder.build()

        // 4. Realistic 3D Skyscrapers, Storefronts, Awnings, Rooftops & Emissive Neon Architecture
        val storefrontBuilder = MeshBuilder(16384)
        val awningBuilder = MeshBuilder(8192)
        val glassBuilder = MeshBuilder(32768)
        val concreteBuilder = MeshBuilder(32768)
        val decoBuilder = MeshBuilder(32768)
        val roofBuilder = MeshBuilder(16384)
        val neonCyanBuilder = MeshBuilder(8192)
        val neonMagentaBuilder = MeshBuilder(8192)
        val neonGoldBuilder = MeshBuilder(8192)

        for ((idx, b) in CityMap.buildings.withIndex()) {
            val r = b.rect
            val w = r.width
            val d = r.height
            val cx = r.left + w / 2f
            val cz = r.top + d / 2f
            val baseHeight = when {
                b.label == "CASINO" -> 195f
                b.label == "NEON CLUB" -> 132f
                b.label == "SAFE HOUSE" -> 76f
                b.label == "GARASI" -> 58f
                b.district == District.DOWNTOWN_NEON -> 124f + (b.heightOffset - 14f) * 8.5f
                b.district == District.VICE_PLAZA -> 108f + (b.heightOffset - 14f) * 7.2f
                b.district == District.OCEAN_DRIVE -> 84f + (b.heightOffset - 14f) * 5.0f
                else -> 70f + (b.heightOffset - 14f) * 4.2f
            }

            val facadeBuilder = when (b.district) {
                District.DOWNTOWN_NEON -> if (idx % 3 == 0) concreteBuilder else glassBuilder
                District.VICE_PLAZA -> if (idx % 2 == 0) decoBuilder else glassBuilder
                District.OCEAN_DRIVE -> if (idx % 2 == 0) decoBuilder else concreteBuilder
                District.INDUSTRIAL_DOCKS -> concreteBuilder
            }

            val lobbyH = 19f
            val uvLobbyX = (w / 52f).coerceAtLeast(1f)
            storefrontBuilder.addBox(
                cx = cx,
                cy = 1.7f + lobbyH / 2f,
                cz = cz,
                sx = w - 1f,
                sy = lobbyH,
                sz = d - 1f,
                uvScaleX = uvLobbyX,
                uvScaleY = 1f,
                includeTop = false,
            )

            // Crisp Canopy / Cornice ledge + angled storefront awnings
            roofBuilder.addBox(
                cx = cx,
                cy = 1.7f + lobbyH + 1.0f,
                cz = cz,
                sx = w + 4.4f,
                sy = 2.0f,
                sz = d + 4.4f,
                uvScaleX = 2f,
                uvScaleY = 0.2f,
            )
            // Angled awnings along front/back street sides
            awningBuilder.addSlopedBox(
                minX = cx - w * 0.42f,
                maxX = cx + w * 0.42f,
                minY = 14.5f,
                maxY = 17.5f,
                halfZBottom = d * 0.5f + 5.2f,
                halfZTop = d * 0.5f + 0.5f,
                frontSlopeX = 0f,
                rearSlopeX = 0f,
            )

            // District-colored neon signage strip wrapping canopy edge
            val neonBuilder = when {
                b.label == "SAFE HOUSE" -> neonCyanBuilder
                b.label == "CASINO" || b.district == District.VICE_PLAZA -> neonGoldBuilder
                b.district == District.DOWNTOWN_NEON -> if (idx % 2 == 0) neonMagentaBuilder else neonCyanBuilder
                b.district == District.OCEAN_DRIVE -> neonCyanBuilder
                else -> neonMagentaBuilder
            }
            neonBuilder.addBox(cx, 1.7f + lobbyH + 1.1f, cz + d * 0.5f + 2.5f, w * 0.84f, 1.4f, 0.8f)
            neonBuilder.addBox(cx, 1.7f + lobbyH + 1.1f, cz - d * 0.5f - 2.5f, w * 0.84f, 1.4f, 0.8f)
            neonBuilder.addBox(cx + w * 0.5f + 2.5f, 1.7f + lobbyH + 1.1f, cz, 0.8f, 1.4f, d * 0.84f)
            neonBuilder.addBox(cx - w * 0.5f - 2.5f, 1.7f + lobbyH + 1.1f, cz, 0.8f, 1.4f, d * 0.84f)

            val upperH = (baseHeight - lobbyH).coerceAtLeast(36f)
            val uvX = (w / 72f).coerceAtLeast(1f)
            val uvY = (upperH / 68f).coerceAtLeast(1f)
            facadeBuilder.addBox(
                cx = cx,
                cy = 1.7f + lobbyH + upperH / 2f,
                cz = cz,
                sx = w,
                sy = upperH,
                sz = d,
                uvScaleX = uvX,
                uvScaleY = uvY,
                includeTop = false,
            )

            // Vertical architectural corner neon accent strips on signature towers
            if (baseHeight > 110f || b.label != null) {
                neonBuilder.addBox(cx + w * 0.5f + 0.6f, 1.7f + lobbyH + upperH * 0.5f, cz + d * 0.5f + 0.6f, 1.4f, upperH * 0.92f, 1.4f)
                neonBuilder.addBox(cx - w * 0.5f - 0.6f, 1.7f + lobbyH + upperH * 0.5f, cz - d * 0.5f - 0.6f, 1.4f, upperH * 0.92f, 1.4f)
            }

            val roofY = 1.7f + baseHeight
            roofBuilder.addBox(
                cx = cx,
                cy = roofY + 1.6f,
                cz = cz,
                sx = w + 3.2f,
                sy = 3.2f,
                sz = d + 3.2f,
                uvScaleX = 2f,
                uvScaleY = 0.2f,
            )
            // Rooftop glowing neon crown trim
            neonBuilder.addBox(cx, roofY + 3.4f, cz + d * 0.5f + 1.2f, w + 2.2f, 1.2f, 1.0f)
            neonBuilder.addBox(cx, roofY + 3.4f, cz - d * 0.5f - 1.2f, w + 2.2f, 1.2f, 1.0f)
            neonBuilder.addBox(cx + w * 0.5f + 1.2f, roofY + 3.4f, cz, 1.0f, 1.2f, d + 2.2f)
            neonBuilder.addBox(cx - w * 0.5f - 1.2f, roofY + 3.4f, cz, 1.0f, 1.2f, d + 2.2f)

            if (baseHeight > 125f && w > 80f && d > 80f) {
                val tierH = 40f
                val tierW = w * 0.64f
                val tierD = d * 0.64f
                facadeBuilder.addBox(
                    cx = cx,
                    cy = roofY + 3.2f + tierH / 2f,
                    cz = cz,
                    sx = tierW,
                    sy = tierH,
                    sz = tierD,
                    uvScaleX = 1.5f,
                    uvScaleY = 0.8f,
                    includeTop = false,
                )
                roofBuilder.addBox(
                    cx = cx,
                    cy = roofY + 3.2f + tierH + 1.5f,
                    cz = cz,
                    sx = tierW + 2.5f,
                    sy = 3f,
                    sz = tierD + 2.5f,
                )
                // Twin HVAC boxes + communications spire with glowing beacon tip
                roofBuilder.addBox(cx - tierW * 0.18f, roofY + 3.2f + tierH + 7f, cz, 18f, 8f, 14f)
                roofBuilder.addBox(cx + tierW * 0.14f, roofY + 3.2f + tierH + 5.5f, cz - tierD * 0.18f, 11f, 5f, 11f)
                roofBuilder.addVerticalCylinder(cx + tierW * 0.2f, roofY + 3.2f + tierH + 3f, roofY + 3.2f + tierH + 44f, cz, 1.6f, 0.45f, 6)
                neonMagentaBuilder.addBox(cx + tierW * 0.2f, roofY + 3.2f + tierH + 45f, cz, 2.2f, 2.8f, 2.2f)
            } else {
                // Rooftop HVAC & water tank housing
                roofBuilder.addBox(cx + w * 0.16f, roofY + 7f, cz - d * 0.14f, 20f, 8f, 16f)
                roofBuilder.addVerticalCylinder(cx - w * 0.20f, roofY + 3.2f, roofY + 15f, cz + d * 0.16f, 6.5f, 6.2f, 8)
            }
        }
        storefrontsMesh = storefrontBuilder.build()
        storeAwningsMesh = awningBuilder.build()
        buildingsGlassMesh = glassBuilder.build()
        buildingsConcreteMesh = concreteBuilder.build()
        buildingsArtDecoMesh = decoBuilder.build()
        rooftopsMesh = roofBuilder.build()
        buildingNeonCyanMesh = neonCyanBuilder.build()
        buildingNeonMagentaMesh = neonMagentaBuilder.build()
        buildingNeonGoldMesh = neonGoldBuilder.build()

        // 5. 3D Streetlights & Warm Ground Pools + Sidewalk Furniture (Benches, Trash Bins, Hydrants, Traffic Lights)
        val poleBuilder = MeshBuilder(16384)
        val bulbBuilder = MeshBuilder(8192)
        val poolBuilder = MeshBuilder(8192)
        val metalFurnBuilder = MeshBuilder(16384)
        val woodFurnBuilder = MeshBuilder(8192)
        val hydrantBuilder = MeshBuilder(8192)
        val trafficGreenBuilder = MeshBuilder(4096)
        val trafficRedBuilder = MeshBuilder(4096)

        for (i in 0 until CityMap.GRID_COUNT) {
            val blockMid = CityMap.ROAD_WIDTH + i * CityMap.CELL_STEP + CityMap.BLOCK_SIZE / 2f
            for (rc in CityMap.roadCenters) {
                val offsets = listOf(-halfRoad - 4f to 1f, halfRoad + 4f to -1f)
                for ((sideOffset, dir) in offsets) {
                    val px = (rc + sideOffset).coerceIn(28f, CityMap.WORLD_SIZE - 28f)
                    val pz = blockMid
                    poleBuilder.addVerticalCylinder(px, 1.6f, 38f, pz, 1.5f, 0.9f, 6)
                    poleBuilder.addBox(px + dir * 7f, 37.5f, pz, 14f, 1.2f, 1.4f)
                    bulbBuilder.addBox(px + dir * 13f, 36.4f, pz, 5.4f, 1.6f, 3.4f)

                    // Ground illumination pool under streetlamp
                    val gx = px + dir * 12f
                    poolBuilder.addQuad(
                        gx - 38f, 0.18f, pz + 38f, 0f, 1f,
                        gx + 38f, 0.18f, pz + 38f, 1f, 1f,
                        gx + 38f, 0.18f, pz - 38f, 1f, 0f,
                        gx - 38f, 0.18f, pz - 38f, 0f, 0f,
                        0f, 1f, 0f,
                    )
                }

                // Sidewalk Bench, Trash Receptacle & Fire Hydrant
                val bx = (rc + halfRoad + 14f).coerceIn(32f, CityMap.WORLD_SIZE - 32f)
                val bz = blockMid - 55f
                if (bx < CityMap.WORLD_SIZE - 45f) {
                    // Bench legs & wooden slats
                    metalFurnBuilder.addBox(bx, 3.2f, bz - 8f, 7f, 3f, 1.6f)
                    metalFurnBuilder.addBox(bx, 3.2f, bz + 8f, 7f, 3f, 1.6f)
                    woodFurnBuilder.addBox(bx, 4.9f, bz, 7.5f, 1.1f, 20f)
                    woodFurnBuilder.addBox(bx + 3.2f, 7.6f, bz, 1.1f, 4.5f, 20f)

                    // Cylindrical metallic trash bin beside bench
                    metalFurnBuilder.addVerticalCylinder(bx + 0.5f, 1.7f, 8.8f, bz + 16f, 2.4f, 2.6f, 8)

                    // Fire hydrant near block corner
                    val hx = bx - 8f
                    val hz = blockMid - 155f
                    hydrantBuilder.addVerticalCylinder(hx, 1.7f, 7.8f, hz, 2.2f, 2.0f, 8)
                    hydrantBuilder.addVerticalCylinder(hx, 7.8f, 9.5f, hz, 2.6f, 1.2f, 8)
                    hydrantBuilder.addBox(hx, 5.8f, hz, 5.8f, 1.6f, 1.6f)
                }
            }
        }

        // Traffic Signal Mast Arms at interior intersections
        for (ix in 1 until centers.size - 1) {
            for (iz in 1 until centers.size - 1) {
                val cx = centers[ix]
                val cz = centers[iz]
                val px = cx + halfRoad + 6f
                val pz = cz + halfRoad + 6f
                // Mast pole & cantilever arm
                metalFurnBuilder.addVerticalCylinder(px, 1.6f, 36f, pz, 1.8f, 1.2f, 7)
                metalFurnBuilder.addBox(px - 16f, 35f, pz, 32f, 1.4f, 1.4f)
                // Dual Signal housing boxes
                metalFurnBuilder.addBox(px - 22f, 32.5f, pz, 3.8f, 7.8f, 3.8f)
                trafficGreenBuilder.addBox(px - 22f, 30.2f, pz, 4.0f, 1.9f, 1.9f)
                trafficRedBuilder.addBox(px - 22f, 34.4f, pz, 4.0f, 1.9f, 1.9f)
            }
        }

        lampPolesMesh = poleBuilder.build()
        lampBulbsMesh = bulbBuilder.build()
        streetLightPoolsMesh = poolBuilder.build()
        streetFurnitureMetalMesh = metalFurnBuilder.build()
        streetFurnitureWoodMesh = woodFurnBuilder.build()
        hydrantRedMesh = hydrantBuilder.build()
        trafficGreenGlowMesh = trafficGreenBuilder.build()
        trafficRedGlowMesh = trafficRedBuilder.build()

        // 6. Realistic 3D Palm Trees (textured bark + coconuts + serrated fronds)
        val trunkBuilder = MeshBuilder(16384)
        val frondBuilder = MeshBuilder(16384)
        for ((tx, ty) in CityMap.palmPositions) {
            trunkBuilder.addVerticalCylinder(tx, 1.6f, 16f, ty, 2.9f, 2.2f, 8, uvScaleY = 1.5f)
            trunkBuilder.addVerticalCylinder(tx + 1.5f, 15.5f, 30f, ty + 1.0f, 2.2f, 1.6f, 8, uvScaleY = 1.5f)
            trunkBuilder.addVerticalCylinder(tx + 3.0f, 29.5f, 43f, ty + 2.0f, 1.6f, 1.2f, 8, uvScaleY = 1.5f)

            val topX = tx + 3.0f
            val topY = 43f
            val topZ = ty + 2.0f
            // Small coconut crown cluster beneath fronds
            trunkBuilder.addBox(topX - 1.2f, topY - 1.8f, topZ + 1.0f, 2.4f, 2.2f, 2.4f)
            trunkBuilder.addBox(topX + 1.3f, topY - 2.0f, topZ - 1.0f, 2.2f, 2.2f, 2.2f)

            for (f in 0 until 8) {
                val ang = (f * 2.0 * PI / 8.0).toFloat()
                val dx = cos(ang)
                val dz = sin(ang)
                val px = -dz
                val pz = dx
                val midX = topX + dx * 14f
                val midY = topY + 5.2f
                val midZ = topZ + dz * 14f
                val tipX = topX + dx * 26f
                val tipY = topY - 7.5f
                val tipZ = topZ + dz * 26f

                frondBuilder.addQuad(
                    topX - px * 4.5f, topY, topZ - pz * 4.5f, 0f, 0f,
                    topX + px * 4.5f, topY, topZ + pz * 4.5f, 1f, 0f,
                    midX + px * 7.5f, midY, midZ + pz * 7.5f, 1f, 0.5f,
                    midX - px * 7.5f, midY, midZ - pz * 7.5f, 0f, 0.5f,
                    0f, 1f, 0f,
                )
                frondBuilder.addQuad(
                    midX - px * 7.5f, midY, midZ - pz * 7.5f, 0f, 0.5f,
                    midX + px * 7.5f, midY, midZ + pz * 7.5f, 1f, 0.5f,
                    tipX + px * 4.2f, tipY, tipZ + pz * 4.2f, 1f, 1f,
                    tipX - px * 4.2f, tipY, tipZ - pz * 4.2f, 0f, 1f,
                    dx * 0.3f, 0.9f, dz * 0.3f,
                )
            }
        }
        palmTrunksMesh = trunkBuilder.build()
        palmFrondsMesh = frondBuilder.build()

        // 7. Sky Dome & Reusable Primitives
        skyDomeMesh = MeshBuilder(4096).apply { addSphere(1.0f, stacks = 12, slices = 20) }.build()
        wheelMesh = MeshBuilder(1024).apply { addWheelCylinder(radius = 5.4f, width = 3.9f, segments = 18) }.build()
        shadowQuadMesh = MeshBuilder(128).apply {
            addQuad(-0.5f, 0f, 0.5f, 0f, 1f, 0.5f, 0f, 0.5f, 1f, 1f, 0.5f, 0f, -0.5f, 1f, 0f, -0.5f, 0f, -0.5f, 0f, 0f, 0f, 1f, 0f)
        }.build()
        unitBoxMesh = MeshBuilder(256).apply { addBox(0f, 0f, 0f, 1f, 1f, 1f, includeBottom = true) }.build()
        unitSphereMesh = MeshBuilder(1024).apply { addSphere(1f, stacks = 8, slices = 12) }.build()
        beaconCylinderMesh = MeshBuilder(512).apply { addVerticalCylinder(0f, 0f, 1f, 0f, 1f, 0.85f, segments = 18) }.build()

        // 8. Realistic 3D Pickups
        cashStackMesh = MeshBuilder(256).apply { addBox(0f, 0f, 0f, 8.8f, 3.2f, 5.2f, includeBottom = true) }.build()
        medkitMesh = MeshBuilder(256).apply {
            addBox(0f, 0f, 0f, 8.2f, 6.2f, 3.6f, includeBottom = true)
            addBox(0f, 3.6f, 0f, 3.4f, 1.1f, 1.0f)
        }.build()
        armorVestMesh = MeshBuilder(256).apply {
            addSlopedBox(-2.2f, 2.2f, -4.0f, 4.2f, 4.2f, 3.6f, 0.5f, 0.5f)
            addBox(0f, 4.6f, -2.4f, 2.0f, 1.5f, 1.6f)
            addBox(0f, 4.6f, 2.4f, 2.0f, 1.5f, 1.6f)
        }.build()
        briefcaseMesh = MeshBuilder(256).apply {
            addBox(0f, 0f, 0f, 9.2f, 6.6f, 3.2f, includeBottom = true)
            addBox(0f, 3.9f, 0f, 3.6f, 1.2f, 0.9f)
        }.build()

        // 9. Realistic Sculpted Articulated 3D Anatomy & Tailored Cream Outfit Layers
        humanTorsoMesh = MeshBuilder(512).apply {
            // Tailored open jacket chest + shoulder lapels
            addSlopedBox(-2.45f, 2.55f, -3.55f, 3.85f, 3.65f, 4.35f, 0.35f, 0.3f)
            // Left & Right Jacket Lapel Collars
            addBox(1.85f, 2.95f, -1.75f, 1.0f, 2.6f, 1.4f)
            addBox(1.85f, 2.95f, 1.75f, 1.0f, 2.6f, 1.4f)
        }.build()
        humanInnerShirtMesh = MeshBuilder(256).apply {
            // Crisp dark/cream undershirt chest panel visible between jacket lapels
            addBox(2.25f, 0.45f, 0f, 0.7f, 5.8f, 2.1f, includeBottom = true)
        }.build()
        humanBeltMesh = MeshBuilder(256).apply {
            addBox(0f, 0f, 0f, 5.1f, 1.25f, 7.5f, includeBottom = true)
            // Metallic belt buckle
            addBox(2.65f, 0f, 0f, 0.45f, 1.0f, 1.4f)
        }.build()
        humanHeadMesh = MeshBuilder(1024).apply {
            addSphere(radius = 2.45f, radiusY = 2.85f, stacks = 12, slices = 16)
        }.build()
        humanHairMesh = MeshBuilder(512).apply {
            addSlopedBox(-2.7f, 1.2f, 0.5f, 3.1f, 2.45f, 2.1f, 0.9f, 0.5f)
            addBox(-1.4f, -0.3f, 0f, 2.6f, 2.8f, 4.6f)
        }.build()
        humanPoliceCapMesh = MeshBuilder(256).apply {
            addVerticalCylinder(0f, 0.8f, 3.1f, 0f, 2.65f, 2.95f, segments = 12)
            addBox(2.2f, 0.9f, 0f, 2.8f, 0.5f, 4.4f)
        }.build()
        humanSunglassesMesh = MeshBuilder(128).apply {
            addBox(2.35f, 0.35f, -0.95f, 0.6f, 0.95f, 1.45f)
            addBox(2.35f, 0.35f, 0.95f, 0.6f, 0.95f, 1.45f)
            addBox(2.35f, 0.55f, 0f, 0.5f, 0.3f, 0.8f)
        }.build()
        humanUpperArmMesh = MeshBuilder(512).apply {
            addTaperedLimb(radiusTop = 1.28f, radiusBottom = 1.08f, length = 3.6f, segments = 10)
        }.build()
        humanForearmMesh = MeshBuilder(512).apply {
            addTaperedLimb(radiusTop = 1.02f, radiusBottom = 0.86f, length = 3.5f, segments = 10)
        }.build()
        humanLegMesh = MeshBuilder(512).apply {
            addTaperedLimb(radiusTop = 1.58f, radiusBottom = 1.16f, length = 7.2f, segments = 10)
        }.build()
        humanShoeMesh = MeshBuilder(256).apply {
            addBox(0.8f, 0.25f, 0f, 3.8f, 1.2f, 2.3f, includeBottom = true)
        }.build()
        humanShoeSoleMesh = MeshBuilder(256).apply {
            addBox(0.8f, -0.5f, 0f, 3.95f, 0.45f, 2.4f, includeBottom = true)
        }.build()

        // Distinct 3D Weapon Meshes
        weaponPistolMesh = MeshBuilder(256).apply {
            addBox(1.8f, 0.45f, 0f, 4.4f, 1.05f, 0.85f, includeBottom = true) // Slide
            addBox(0.1f, -0.8f, 0f, 1.3f, 1.85f, 0.8f, includeBottom = true) // Grip
        }.build()
        weaponSmgMesh = MeshBuilder(512).apply {
            addBox(2.2f, 0.5f, 0f, 6.2f, 1.3f, 1.05f, includeBottom = true) // Receiver
            addBox(5.8f, 0.55f, 0f, 2.4f, 0.6f, 0.65f, includeBottom = true) // Barrel shroud
            addBox(-1.4f, 0.4f, 0f, 2.0f, 1.0f, 0.75f, includeBottom = true) // Compact rear stock
            addBox(0.4f, -0.85f, 0f, 1.3f, 2.0f, 0.9f, includeBottom = true) // Pistol grip
            addBox(3.4f, -1.35f, 0f, 1.0f, 2.7f, 0.75f, includeBottom = true) // Extended curved magazine
        }.build()
        weaponShotgunMesh = MeshBuilder(512).apply {
            addBox(3.6f, 0.65f, 0f, 9.6f, 0.9f, 0.95f, includeBottom = true) // Long barrel + receiver
            addBox(4.8f, 0.05f, 0f, 5.2f, 0.75f, 1.05f, includeBottom = true) // Pump forend
            addBox(-1.6f, 0.0f, 0f, 3.4f, 1.4f, 0.85f, includeBottom = true) // Buttstock
        }.build()
        weaponPlasmaMesh = MeshBuilder(512).apply {
            addBox(3.0f, 0.55f, 0f, 9.2f, 1.55f, 1.35f, includeBottom = true) // Heavy sci-fi chassis
            addBox(8.2f, 0.85f, 0f, 2.8f, 0.55f, 0.75f, includeBottom = true) // Upper rail
            addBox(8.2f, 0.15f, 0f, 2.8f, 0.55f, 0.75f, includeBottom = true) // Lower rail
            addBox(-1.4f, 0.25f, 0f, 2.6f, 1.4f, 1.05f, includeBottom = true) // Stock
            addBox(0.6f, -1.0f, 0f, 1.4f, 2.0f, 0.95f, includeBottom = true) // Grip
        }.build()
        weaponPlasmaCoreMesh = MeshBuilder(256).apply {
            addBox(4.2f, 0.58f, 0f, 4.4f, 0.75f, 1.48f, includeBottom = true) // Glowing cyan energy coil
        }.build()

        // 9b. Sculpted 3D Cat Ears, Muzzle, Tail, Tail Tip & Collar
        catEarOuterMesh = MeshBuilder(512).apply {
            for (side in listOf(-1f, 1f)) {
                val bz = side * 1.45f
                val tipZ = side * 1.95f
                val tipY = 3.85f
                val baseY = 1.45f
                addQuad(
                    0.5f, baseY, bz - 0.85f, 0f, 1f,
                    0.5f, baseY, bz + 0.85f, 1f, 1f,
                    0.15f, tipY, tipZ + 0.14f, 0.6f, 0f,
                    0.15f, tipY, tipZ - 0.14f, 0.4f, 0f,
                    0.96f, 0.25f, 0f,
                )
                addQuad(
                    -1.25f, baseY, bz + 0.85f, 0f, 1f,
                    -1.25f, baseY, bz - 0.85f, 1f, 1f,
                    0.15f, tipY, tipZ - 0.14f, 0.6f, 0f,
                    0.15f, tipY, tipZ + 0.14f, 0.4f, 0f,
                    -0.92f, 0.35f, 0f,
                )
                addQuad(
                    0.5f, baseY, bz + side * 0.85f, 0f, 1f,
                    -1.25f, baseY, bz + side * 0.85f, 1f, 1f,
                    0.15f, tipY, tipZ, 0.5f, 0f,
                    0.15f, tipY, tipZ, 0.5f, 0f,
                    0f, 0.3f, side,
                )
                addQuad(
                    -1.25f, baseY, bz - side * 0.85f, 0f, 1f,
                    0.5f, baseY, bz - side * 0.85f, 1f, 1f,
                    0.15f, tipY, tipZ, 0.5f, 0f,
                    0.15f, tipY, tipZ, 0.5f, 0f,
                    0f, 0.3f, -side,
                )
            }
        }.build()

        catEarInnerMesh = MeshBuilder(128).apply {
            for (side in listOf(-1f, 1f)) {
                val bz = side * 1.45f
                val tipZ = side * 1.85f
                addQuad(
                    0.62f, 1.65f, bz - 0.52f, 0f, 1f,
                    0.62f, 1.65f, bz + 0.52f, 1f, 1f,
                    0.26f, 3.35f, tipZ + 0.08f, 0.5f, 0f,
                    0.26f, 3.35f, tipZ - 0.08f, 0.5f, 0f,
                    1f, 0.2f, 0f,
                )
            }
        }.build()

        catMuzzleMesh = MeshBuilder(256).apply {
            addSlopedBox(1.85f, 3.18f, -1.15f, 0.05f, 1.15f, 0.85f, 0.4f, 0.1f)
        }.build()

        catTailMesh = MeshBuilder(512).apply {
            addBox(-2.2f, 0.2f, 0f, 4.4f, 1.05f, 1.05f, includeBottom = true)
            addBox(-4.8f, 1.4f, 0f, 3.2f, 0.95f, 0.95f, includeBottom = true)
            addBox(-6.3f, 3.3f, 0f, 1.8f, 3.1f, 0.88f, includeBottom = true)
        }.build()

        catTailTipMesh = MeshBuilder(256).apply {
            addBox(-5.6f, 5.2f, 0f, 1.7f, 1.5f, 0.9f, includeBottom = true)
        }.build()

        catCollarMesh = MeshBuilder(256).apply {
            // Neck collar band + front gold tag
            addVerticalCylinder(0f, -0.35f, 0.45f, 0f, 2.0f, 1.85f, segments = 12)
            addBox(2.05f, -0.45f, 0f, 0.5f, 0.9f, 0.9f, includeBottom = true)
        }.build()

        // 10. Distinct Realistic 3D Vehicle Models
        vehicleModels = VehicleKind.entries.associateWith { buildVehicleModel(it) }
    }

    private fun buildVehicleModel(kind: VehicleKind): VehicleModelSet {
        val halfL = kind.length * 0.5f
        val halfW = kind.width * 0.5f
        val isSwat = kind == VehicleKind.SWAT
        val isSuper = kind == VehicleKind.SUPERCAR
        val isMuscle = kind == VehicleKind.MUSCLE
        val isTaxi = kind == VehicleKind.TAXI
        val isPolice = kind == VehicleKind.POLICE

        val chassisHeight = when {
            isSwat -> 13.0f
            isSuper -> 6.8f
            else -> 8.4f
        }
        val chassisBottom = 3.2f
        val chassisTop = chassisBottom + chassisHeight

        val chassis = MeshBuilder(1024).apply {
            addSlopedBox(
                minX = -halfL,
                maxX = halfL,
                minY = chassisBottom,
                maxY = chassisTop,
                halfZBottom = halfW,
                halfZTop = halfW * 0.94f,
                frontSlopeX = if (isSuper) 9f else if (isSwat) 2.5f else 5f,
                rearSlopeX = if (isSwat) 1.5f else 3f,
            )
            // Side rear-view mirrors + wheel arch fender flares
            addBox(halfL * 0.28f, chassisTop + 1.4f, -halfW - 1.2f, 2.0f, 1.5f, 2.6f)
            addBox(halfL * 0.28f, chassisTop + 1.4f, halfW + 1.2f, 2.0f, 1.5f, 2.6f)

            if (isMuscle) {
                // Supercharger hood blower intake
                addBox(halfL * 0.35f, chassisTop + 1.4f, 0f, 12f, 2.2f, 8f)
            }
            if (isSuper) {
                // Dual-strut aerodynamic carbon rear wing
                addBox(-halfL + 4f, chassisTop + 3.2f, -halfW * 0.55f, 2f, 4f, 1.5f)
                addBox(-halfL + 4f, chassisTop + 3.2f, halfW * 0.55f, 2f, 4f, 1.5f)
                addBox(-halfL + 3f, chassisTop + 5.2f, 0f, 6.8f, 1.2f, kind.width * 0.98f)
            }
        }.build()

        val cabinMinX = when {
            isSwat -> -halfL + 3f
            isSuper -> -halfL * 0.55f
            else -> -halfL * 0.62f
        }
        val cabinMaxX = when {
            isSwat -> halfL - 5f
            isSuper -> halfL * 0.38f
            else -> halfL * 0.36f
        }
        val cabinTop = chassisTop + when {
            isSwat -> 9.5f
            isSuper -> 5.6f
            else -> 6.8f
        }

        val glass = MeshBuilder(1024).apply {
            addSlopedBox(
                minX = cabinMinX,
                maxX = cabinMaxX,
                minY = chassisTop - 0.2f,
                maxY = cabinTop,
                halfZBottom = halfW * 0.86f,
                halfZTop = halfW * 0.72f,
                frontSlopeX = if (isSwat) 3.5f else 8.5f,
                rearSlopeX = if (isSwat) 1.5f else 7.0f,
            )
        }.build()

        val trim = MeshBuilder(1024).apply {
            // Front & rear bumpers
            addBox(halfL + 0.9f, chassisBottom + 1.8f, 0f, 2.3f, 3.0f, kind.width * 0.98f)
            addBox(-halfL - 0.9f, chassisBottom + 1.8f, 0f, 2.3f, 3.0f, kind.width * 0.98f)
            // Front splitter / SWAT push bar
            if (isPolice || isSwat) {
                addBox(halfL + 2.4f, chassisBottom + 3.2f, 0f, 1.4f, 4.2f, kind.width * 0.56f)
            }
            // Front radiator grille + rear dual chrome exhausts + license plate
            addBox(halfL + 0.35f, chassisBottom + 4.4f, 0f, 1.3f, 2.8f, kind.width * 0.52f)
            addBox(-halfL - 1.3f, chassisBottom + 1.2f, -halfW * 0.45f, 2.6f, 1.35f, 1.5f)
            addBox(-halfL - 1.3f, chassisBottom + 1.2f, halfW * 0.45f, 2.6f, 1.35f, 1.5f)
            addBox(-halfL - 0.2f, chassisBottom + 4.2f, 0f, 0.8f, 1.9f, 5.2f)
            // Side lower rocker skirts
            addBox(0f, chassisBottom + 0.6f, -halfW - 0.1f, kind.length * 0.48f, 1.6f, 1.1f)
            addBox(0f, chassisBottom + 0.6f, halfW + 0.1f, kind.length * 0.48f, 1.6f, 1.1f)
            // Metallic roof cap
            addBox(
                cx = (cabinMinX + cabinMaxX) * 0.5f - 0.6f,
                cy = cabinTop + 0.35f,
                cz = 0f,
                sx = (cabinMaxX - cabinMinX) * 0.64f,
                sy = 0.8f,
                sz = halfW * 1.38f,
            )
        }.build()

        // Distinct Livery Accent (Police white doors, Muscle/Supercar dual racing stripes, Taxi door band)
        val accent = when {
            isPolice -> MeshBuilder(256).apply {
                addBox(0f, chassisBottom + chassisHeight * 0.52f, -halfW + 0.25f, kind.length * 0.44f, chassisHeight * 0.86f, 0.8f)
                addBox(0f, chassisBottom + chassisHeight * 0.52f, halfW - 0.25f, kind.length * 0.44f, chassisHeight * 0.86f, 0.8f)
                addBox((cabinMinX + cabinMaxX) * 0.5f - 0.6f, cabinTop + 0.85f, 0f, (cabinMaxX - cabinMinX) * 0.58f, 0.55f, halfW * 1.32f)
            }.build()
            isMuscle || isSuper -> MeshBuilder(256).apply {
                addBox(halfL * 0.1f, chassisTop + 0.25f, -2.1f, kind.length * 0.88f, 0.4f, 1.8f)
                addBox(halfL * 0.1f, chassisTop + 0.25f, 2.1f, kind.length * 0.88f, 0.4f, 1.8f)
            }.build()
            isTaxi -> MeshBuilder(256).apply {
                addBox(0f, chassisBottom + chassisHeight * 0.48f, -halfW + 0.2f, kind.length * 0.72f, 2.2f, 0.7f)
                addBox(0f, chassisBottom + chassisHeight * 0.48f, halfW - 0.2f, kind.length * 0.72f, 2.2f, 0.7f)
            }.build()
            else -> null
        }

        val headlights = MeshBuilder(256).apply {
            addBox(halfL - 0.3f, chassisBottom + chassisHeight * 0.55f, -halfW * 0.64f, 1.8f, 2.5f, 5.0f)
            addBox(halfL - 0.3f, chassisBottom + chassisHeight * 0.55f, halfW * 0.64f, 1.8f, 2.5f, 5.0f)
        }.build()

        val taillights = MeshBuilder(256).apply {
            addBox(-halfL + 0.3f, chassisBottom + chassisHeight * 0.62f, -halfW * 0.64f, 1.8f, 2.3f, 5.4f)
            addBox(-halfL + 0.3f, chassisBottom + chassisHeight * 0.62f, halfW * 0.64f, 1.8f, 2.3f, 5.4f)
            addBox(-halfL + 0.2f, chassisBottom + chassisHeight * 0.62f, 0f, 1.4f, 0.9f, kind.width * 0.52f)
        }.build()

        val leftBar = if (isPolice || isSwat) {
            MeshBuilder(128).apply { addBox(-1f, cabinTop + 1.6f, -halfW * 0.28f, 3.8f, 2.2f, halfW * 0.5f) }.build()
        } else null
        val rightBar = if (isPolice || isSwat) {
            MeshBuilder(128).apply { addBox(-1f, cabinTop + 1.6f, halfW * 0.28f, 3.8f, 2.2f, halfW * 0.5f) }.build()
        } else null
        val taxiSign = if (isTaxi) {
            MeshBuilder(128).apply { addBox(0f, cabinTop + 1.7f, 0f, 4.5f, 2.4f, 9.0f) }.build()
        } else null

        return VehicleModelSet(
            chassis = chassis,
            glass = glass,
            trim = trim,
            accent = accent,
            headlights = headlights,
            taillights = taillights,
            leftLightbar = leftBar,
            rightLightbar = rightBar,
            roofSign = taxiSign,
            wheelRadius = 5.4f,
            frontWheelX = halfL * 0.62f,
            rearWheelX = -halfL * 0.62f,
            wheelZ = halfW - 1.2f,
        )
    }

    fun release() {
        oceanMesh.release()
        beachMesh.release()
        roadMesh.release()
        intersectionMesh.release()
        sidewalkMesh.release()
        sidewalkCurbMesh.release()
        storefrontsMesh.release()
        storeAwningsMesh.release()
        buildingsGlassMesh.release()
        buildingsConcreteMesh.release()
        buildingsArtDecoMesh.release()
        rooftopsMesh.release()
        buildingNeonCyanMesh.release()
        buildingNeonMagentaMesh.release()
        buildingNeonGoldMesh.release()
        lampPolesMesh.release()
        lampBulbsMesh.release()
        streetLightPoolsMesh.release()
        streetFurnitureMetalMesh.release()
        streetFurnitureWoodMesh.release()
        hydrantRedMesh.release()
        trafficGreenGlowMesh.release()
        trafficRedGlowMesh.release()
        palmTrunksMesh.release()
        palmFrondsMesh.release()
        skyDomeMesh.release()
        wheelMesh.release()
        shadowQuadMesh.release()
        unitBoxMesh.release()
        unitSphereMesh.release()
        beaconCylinderMesh.release()
        cashStackMesh.release()
        medkitMesh.release()
        armorVestMesh.release()
        briefcaseMesh.release()
        humanTorsoMesh.release()
        humanInnerShirtMesh.release()
        humanBeltMesh.release()
        humanHeadMesh.release()
        humanHairMesh.release()
        humanPoliceCapMesh.release()
        humanSunglassesMesh.release()
        humanUpperArmMesh.release()
        humanForearmMesh.release()
        humanLegMesh.release()
        humanShoeMesh.release()
        humanShoeSoleMesh.release()
        weaponPistolMesh.release()
        weaponSmgMesh.release()
        weaponShotgunMesh.release()
        weaponPlasmaMesh.release()
        weaponPlasmaCoreMesh.release()
        catEarOuterMesh.release()
        catEarInnerMesh.release()
        catMuzzleMesh.release()
        catTailMesh.release()
        catTailTipMesh.release()
        catCollarMesh.release()
        vehicleModels.values.forEach { it.release() }
    }
}
