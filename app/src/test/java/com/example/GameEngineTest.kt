package com.example

import com.example.data.CareerProfile
import com.example.game.CityMap
import com.example.game.GameEngine
import com.example.game.PlayerInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.hypot

class GameEngineTest {

    @Test
    fun playerCanMoveAndHijackPersonalVehicle() {
        val engine = GameEngine()
        val career = CareerProfile(userId = "test_uid")
        var state = engine.createInitialState(career)

        val personalCar = state.vehicles.first { it.isPersonal }
        val startDist = hypot(personalCar.x - state.playerX, personalCar.y - state.playerY)
        assertTrue("Personal car spawns near player", startDist < 120f)

        // Move toward personal car
        val dx = personalCar.x - state.playerX
        val dy = personalCar.y - state.playerY
        val len = hypot(dx, dy).coerceAtLeast(1f)
        repeat(20) {
            state = engine.step(
                state,
                0.03f,
                PlayerInput(moveX = dx / len, moveY = dy / len),
                career,
            ).first
        }

        // Trigger hijack
        val (inCarState, _) = engine.step(
            state,
            0.016f,
            PlayerInput(hijackTriggerCount = 1),
            career,
        )
        assertEquals(personalCar.id, inCarState.inVehicleId)
        assertTrue(inCarState.soundEvents.contains(com.example.game.SoundEvent.HIJACK_ENTER))

        // Drive forward with nitro
        val (drivenState, _) = engine.step(
            inCarState,
            0.05f,
            PlayerInput(moveX = 1f, moveY = 0f, isNitroPressed = true, hijackTriggerCount = 1),
            career,
        )
        assertTrue("Nitro fuel decreases when boosting", drivenState.nitroFuel < 100f)

        // Exit vehicle & fire weapon
        val (exitedState, _) = engine.step(
            drivenState,
            0.016f,
            PlayerInput(isFiring = true, hijackTriggerCount = 2),
            career,
        )
        assertNull(exitedState.inVehicleId)
        assertTrue(exitedState.soundEvents.contains(com.example.game.SoundEvent.VEHICLE_EXIT))
        assertTrue(exitedState.soundEvents.contains(com.example.game.SoundEvent.WEAPON_PISTOL))
    }

    @Test
    fun syndicateEliminationMissionSpawnsFourTargets() {
        val engine = GameEngine()
        val career = CareerProfile(userId = "test_uid")
        val initial = engine.createInitialState(career)
        val mission = CityMap.missions.first { it.id == "SYNDICATE_SHAKEDOWN" }

        val started = engine.startMission(initial, mission)
        assertNotNull(started.activeMission)
        assertEquals(4, started.peds.count { it.isMissionTarget })
    }

    @Test
    fun cameraControllerMapsStickAndUpdatesFiniteCoordinates() {
        val cam = com.example.game.render3d.CameraController()
        val engine = GameEngine()
        val world = engine.createInitialState(CareerProfile(userId = "test_uid"))

        cam.update(world, 0.016f)
        assertTrue(cam.camX.isFinite() && cam.camY.isFinite() && cam.camZ.isFinite())
        assertTrue(cam.camY > 20f)

        val nextMode = cam.toggleMode()
        assertEquals(com.example.game.render3d.CameraMode.DRONE_3D, nextMode)
        cam.update(world, 0.05f)
        val (wx, wy) = cam.screenStickToWorld(0f, -1f)
        assertTrue("Pushing stick up in North drone mode moves North (-Y)", wy < -0.5f && kotlin.math.abs(wx) < 0.3f)
    }

    @Test
    fun playerStopsWalkingAnimationWhenStationary() {
        val engine = GameEngine()
        val career = CareerProfile(userId = "test_uid")
        var state = engine.createInitialState(career)

        // Initially stationary
        val (idleState, _) = engine.step(state, 0.03f, PlayerInput(), career)
        assertTrue(!idleState.isPlayerMoving)
        assertEquals(0f, idleState.playerWalkPhase, 0.0001f)

        // Move forward -> legs animate
        val (movingState, _) = engine.step(idleState, 0.03f, PlayerInput(moveX = 1f, moveY = 0f), career)
        assertTrue(movingState.isPlayerMoving)
        assertTrue(movingState.playerWalkPhase > 0f)

        // Stop moving -> legs stop immediately
        val (stoppedState, _) = engine.step(movingState, 0.03f, PlayerInput(moveX = 0f, moveY = 0f), career)
        assertTrue(!stoppedState.isPlayerMoving)
        assertEquals(0f, stoppedState.playerWalkPhase, 0.0001f)
    }
}
