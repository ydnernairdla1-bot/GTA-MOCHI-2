package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.MenuMusicPlayer
import com.example.audio.SoundEffectsEngine
import com.example.data.CareerProfile
import com.example.data.CareerRepository
import com.example.data.MissionRun
import com.example.data.VehicleCatalog
import com.example.data.WeaponType
import com.example.game.CareerEvent
import com.example.game.GameEngine
import com.example.game.GameWorldState
import com.example.game.MissionTemplate
import com.example.game.PlayerInput
import com.example.game.SoundEvent
import com.example.game.render3d.CameraController
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.max

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class HubTab {
    CITY,
    MISSIONS,
    BLACK_MARKET,
    CAREER,
}

class NeonViceViewModel(
    private val repository: CareerRepository,
    private val soundEngine: SoundEffectsEngine? = null,
    private val menuMusicPlayer: MenuMusicPlayer? = null,
) : ViewModel() {

    private val engine = GameEngine()

    val careerState: StateFlow<UiState<CareerProfile>> = repository.observeCareer()
        .map<CareerProfile?, UiState<CareerProfile>> { profile ->
            UiState.Success(profile ?: CareerProfile())
        }
        .catch { error ->
            if (error is CancellationException) throw error
            emit(UiState.Error(error.message ?: "Gagal memuat data karier lokal"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading,
        )

    val recentRunsState: StateFlow<UiState<List<MissionRun>>> = repository.observeRecentRuns()
        .map<List<MissionRun>, UiState<List<MissionRun>>> { UiState.Success(it) }
        .catch { error ->
            if (error is CancellationException) throw error
            emit(UiState.Error(error.message ?: "Gagal memuat riwayat misi"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading,
        )

    private val _selectedTab = MutableStateFlow(HubTab.CITY)
    val selectedTab: StateFlow<HubTab> = _selectedTab.asStateFlow()

    private val _isLoadingCity = MutableStateFlow(false)
    val isLoadingCity: StateFlow<Boolean> = _isLoadingCity.asStateFlow()

    private val _loadingProgress = MutableStateFlow(0f)
    val loadingProgress: StateFlow<Float> = _loadingProgress.asStateFlow()

    private val _loadingStageText = MutableStateFlow("Menyiapkan GTA Neon City...")
    val loadingStageText: StateFlow<String> = _loadingStageText.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()

    private val _worldState = MutableStateFlow(engine.createInitialState(CareerProfile()))
    val worldState: StateFlow<GameWorldState> = _worldState.asStateFlow()

    private val _statusToast = MutableStateFlow<String?>(null)
    val statusToast: StateFlow<String?> = _statusToast.asStateFlow()

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    val cameraController = CameraController()

    private var rawStickX = 0f
    private var rawStickY = 0f
    private var currentInput = PlayerInput()
    private var hijackCounter = 0
    private var gameLoopJob: Job? = null
    private var loadingJob: Job? = null
    private var hasInitializedWorld = false
    private var pendingCashDelta = 0L
    private var pendingRespectDelta = 0L
    private var pendingMissionsDelta = 0L
    private var pendingVehiclesDelta = 0L
    private var pendingHighScore = 0L
    private var pendingMaxWanted = 0
    private var lastSyncMs = 0L

    init {
        menuMusicPlayer?.setShouldPlay(true)
        viewModelScope.launch {
            repository.ensureProfileExists()
        }
        viewModelScope.launch {
            careerState.collect { state ->
                if (state is UiState.Success && !hasInitializedWorld) {
                    hasInitializedWorld = true
                    _worldState.value = engine.createInitialState(state.data)
                }
            }
        }
    }

    fun selectTab(tab: HubTab) {
        _selectedTab.value = tab
    }

    fun clearStatusToast() {
        _statusToast.value = null
    }

    fun enterCity(missionToStart: MissionTemplate? = null) {
        if (_isLoadingCity.value) return
        val career = currentCareerOrDefault()
        if (!hasInitializedWorld) {
            _worldState.value = engine.createInitialState(career)
            hasInitializedWorld = true
        }
        if (missionToStart != null) {
            _worldState.value = engine.startMission(_worldState.value, missionToStart)
        }
        soundEngine?.playUiClick()
        _isPlaying.value = false
        _isPaused.value = false
        _loadingProgress.value = 0f
        _loadingStageText.value = "Memuat peta jalan & distrik Neon City..."
        _isLoadingCity.value = true

        loadingJob?.cancel()
        loadingJob = viewModelScope.launch {
            val steps = 56
            for (step in 1..steps) {
                val p = step / steps.toFloat()
                _loadingProgress.value = p
                _loadingStageText.value = when {
                    p < 0.28f -> "Memuat peta jalan & distrik Neon City..."
                    p < 0.58f -> "Menyiapkan kendaraan & lalu lintas kota..."
                    p < 0.85f -> "Memanggil agen kucing Mochi ke jalanan..."
                    else -> "Mengaktifkan radar, misi & audio 3D..."
                }
                delay(35L)
            }
            _isLoadingCity.value = false
            _isPaused.value = false
            _isPlaying.value = true
            menuMusicPlayer?.setShouldPlay(false)
            soundEngine?.setSessionActive(true)
            if (missionToStart != null) {
                soundEngine?.processWorldState(_worldState.value, isNitroActive = false)
            }
            startGameLoop()
        }
    }

    fun cancelLoading() {
        loadingJob?.cancel()
        loadingJob = null
        _isLoadingCity.value = false
        _loadingProgress.value = 0f
        menuMusicPlayer?.setShouldPlay(true)
    }

    fun pauseGame() {
        _isPaused.value = true
        soundEngine?.setSessionActive(false)
    }

    fun resumeGame() {
        _isPaused.value = false
        if (_isPlaying.value) {
            soundEngine?.setSessionActive(true)
        }
    }

    fun exitToHub() {
        flushPendingCareerSync()
        loadingJob?.cancel()
        loadingJob = null
        _isLoadingCity.value = false
        _isPaused.value = false
        _isPlaying.value = false
        soundEngine?.setSessionActive(false)
        menuMusicPlayer?.setShouldPlay(true)
        gameLoopJob?.cancel()
        gameLoopJob = null
    }

    fun toggleMute() {
        val next = !_isMuted.value
        _isMuted.value = next
        soundEngine?.setMuted(next)
        menuMusicPlayer?.setMuted(next)
        if (!next) {
            soundEngine?.playUiClick()
        }
    }

    fun onAppBackgroundStateChanged(isForeground: Boolean) {
        menuMusicPlayer?.setForeground(isForeground)
        if (isForeground && _isPlaying.value && !_isPaused.value) {
            soundEngine?.setSessionActive(true)
        } else if (!isForeground) {
            soundEngine?.setSessionActive(false)
        }
    }

    fun updateJoystick(moveX: Float, moveY: Float) {
        rawStickX = moveX
        rawStickY = moveY
        val (wx, wy) = cameraController.screenStickToWorld(moveX, moveY)
        currentInput = currentInput.copy(moveX = wx, moveY = wy)
    }

    fun setFiring(firing: Boolean) {
        currentInput = currentInput.copy(isFiring = firing)
    }

    fun setNitroPressed(pressed: Boolean) {
        currentInput = currentInput.copy(isNitroPressed = pressed)
    }

    fun triggerHijack() {
        hijackCounter++
        currentInput = currentInput.copy(hijackTriggerCount = hijackCounter)
    }

    fun cycleEquippedWeapon() {
        val career = currentCareerOrDefault()
        val unlocked = WeaponType.entries
            .filter { career.unlockedWeapons.contains(it.id) }
            .ifEmpty { listOf(WeaponType.FISTS, WeaponType.PISTOL) }
        val currentIdx = unlocked.indexOfFirst { it == _worldState.value.equippedWeapon }
        val next = unlocked[(currentIdx + 1) % unlocked.size]
        _worldState.value = _worldState.value.copy(equippedWeapon = next)
        soundEngine?.triggerEvent(SoundEvent.WEAPON_SWITCH)
        viewModelScope.launch {
            repository.saveCareerProgress(
                cash = career.cash,
                respect = career.respect,
                highScore = career.highScore,
                missionsCompleted = career.missionsCompleted,
                vehiclesStolen = career.vehiclesStolen,
                maxWantedSurvived = career.maxWantedSurvived,
                unlockedWeapons = career.unlockedWeapons,
                equippedWeapon = next.id,
                ownedVehicles = career.ownedVehicles,
                equippedVehicle = career.equippedVehicle,
                armorLevel = career.armorLevel,
                nitroLevel = career.nitroLevel,
            )
        }
    }

    fun selectOrBuyWeaponInGame(weapon: WeaponType): Boolean {
        val career = currentCareerOrDefault()
        val alreadyUnlocked = career.unlockedWeapons.contains(weapon.id)
        if (!alreadyUnlocked && career.cash < weapon.price) {
            return false
        }
        val newCash = if (alreadyUnlocked) career.cash else career.cash - weapon.price
        val newWeapons = if (alreadyUnlocked) career.unlockedWeapons else (career.unlockedWeapons + weapon.id).distinct()
        _worldState.value = _worldState.value.copy(equippedWeapon = weapon)
        if (!alreadyUnlocked) {
            soundEngine?.triggerEvent(SoundEvent.PICKUP_CASH)
        }
        soundEngine?.triggerEvent(SoundEvent.WEAPON_SWITCH)
        viewModelScope.launch {
            repository.saveCareerProgress(
                cash = newCash,
                respect = career.respect,
                highScore = career.highScore,
                missionsCompleted = career.missionsCompleted,
                vehiclesStolen = career.vehiclesStolen,
                maxWantedSurvived = career.maxWantedSurvived,
                unlockedWeapons = newWeapons,
                equippedWeapon = weapon.id,
                ownedVehicles = career.ownedVehicles,
                equippedVehicle = career.equippedVehicle,
                armorLevel = career.armorLevel,
                nitroLevel = career.nitroLevel,
            )
        }
        return true
    }

    fun summonPersonalCar() {
        val career = currentCareerOrDefault()
        _worldState.value = engine.summonPersonalVehicle(_worldState.value, career)
        _isPaused.value = false
        if (_isPlaying.value) {
            soundEngine?.setSessionActive(true)
            soundEngine?.playUiClick()
        }
    }

    fun respawnPlayer() {
        val career = currentCareerOrDefault()
        _worldState.value = engine.respawnAfterHospitalOrPrecinct(_worldState.value, career)
    }

    fun buyOrEquipWeapon(weapon: WeaponType) {
        val career = currentCareerOrDefault()
        val alreadyUnlocked = career.unlockedWeapons.contains(weapon.id)
        if (!alreadyUnlocked && career.cash < weapon.price) {
            _statusToast.value = "Uang tidak cukup ($${weapon.price} dibutuhkan)"
            return
        }
        val newCash = if (alreadyUnlocked) career.cash else career.cash - weapon.price
        val newWeapons = if (alreadyUnlocked) career.unlockedWeapons else (career.unlockedWeapons + weapon.id).distinct()
        _worldState.value = _worldState.value.copy(equippedWeapon = weapon)

        viewModelScope.launch {
            repository.saveCareerProgress(
                cash = newCash,
                respect = career.respect,
                highScore = career.highScore,
                missionsCompleted = career.missionsCompleted,
                vehiclesStolen = career.vehiclesStolen,
                maxWantedSurvived = career.maxWantedSurvived,
                unlockedWeapons = newWeapons,
                equippedWeapon = weapon.id,
                ownedVehicles = career.ownedVehicles,
                equippedVehicle = career.equippedVehicle,
                armorLevel = career.armorLevel,
                nitroLevel = career.nitroLevel,
            )
            _statusToast.value = if (alreadyUnlocked) "${weapon.displayName} dipakai" else "${weapon.displayName} dibeli & dipakai!"
        }
    }

    fun buyOrEquipVehicle(vehicle: VehicleCatalog) {
        val career = currentCareerOrDefault()
        val owned = career.ownedVehicles.contains(vehicle.id)
        if (!owned && career.cash < vehicle.price) {
            _statusToast.value = "Uang tidak cukup ($${vehicle.price} dibutuhkan)"
            return
        }
        val newCash = if (owned) career.cash else career.cash - vehicle.price
        val newVehicles = if (owned) career.ownedVehicles else (career.ownedVehicles + vehicle.id).distinct()

        viewModelScope.launch {
            repository.saveCareerProgress(
                cash = newCash,
                respect = career.respect,
                highScore = career.highScore,
                missionsCompleted = career.missionsCompleted,
                vehiclesStolen = career.vehiclesStolen,
                maxWantedSurvived = career.maxWantedSurvived,
                unlockedWeapons = career.unlockedWeapons,
                equippedWeapon = career.equippedWeapon,
                ownedVehicles = newVehicles,
                equippedVehicle = vehicle.id,
                armorLevel = career.armorLevel,
                nitroLevel = career.nitroLevel,
            )
            _statusToast.value = if (owned) "${vehicle.displayName} dipilih" else "${vehicle.displayName} masuk garasi!"
        }
    }

    fun upgradeArmor() {
        val career = currentCareerOrDefault()
        if (career.armorLevel >= 3L) return
        val cost = (career.armorLevel + 1L) * 700L
        if (career.cash < cost) {
            _statusToast.value = "Uang tidak cukup ($${cost} dibutuhkan)"
            return
        }
        val nextLevel = career.armorLevel + 1L
        viewModelScope.launch {
            repository.saveCareerProgress(
                cash = career.cash - cost,
                respect = career.respect,
                highScore = career.highScore,
                missionsCompleted = career.missionsCompleted,
                vehiclesStolen = career.vehiclesStolen,
                maxWantedSurvived = career.maxWantedSurvived,
                unlockedWeapons = career.unlockedWeapons,
                equippedWeapon = career.equippedWeapon,
                ownedVehicles = career.ownedVehicles,
                equippedVehicle = career.equippedVehicle,
                armorLevel = nextLevel,
                nitroLevel = career.nitroLevel,
            )
            _worldState.value = _worldState.value.copy(
                playerMaxArmor = nextLevel * 35f,
                playerArmor = nextLevel * 35f,
            )
            _statusToast.value = "Rompi Anti Peluru Level $nextLevel aktif!"
        }
    }

    fun upgradeNitro() {
        val career = currentCareerOrDefault()
        if (career.nitroLevel >= 3L) return
        val cost = (career.nitroLevel + 1L) * 650L
        if (career.cash < cost) {
            _statusToast.value = "Uang tidak cukup ($${cost} dibutuhkan)"
            return
        }
        val nextLevel = career.nitroLevel + 1L
        viewModelScope.launch {
            repository.saveCareerProgress(
                cash = career.cash - cost,
                respect = career.respect,
                highScore = career.highScore,
                missionsCompleted = career.missionsCompleted,
                vehiclesStolen = career.vehiclesStolen,
                maxWantedSurvived = career.maxWantedSurvived,
                unlockedWeapons = career.unlockedWeapons,
                equippedWeapon = career.equippedWeapon,
                ownedVehicles = career.ownedVehicles,
                equippedVehicle = career.equippedVehicle,
                armorLevel = career.armorLevel,
                nitroLevel = nextLevel,
            )
            _statusToast.value = "Turbo Nitro Level $nextLevel terpasang!"
        }
    }

    fun updateCallsign(newCallsign: String) {
        val trimmed = newCallsign.trim()
        if (trimmed.length !in 2..30) {
            _statusToast.value = "Nama jalanan harus 2-30 karakter"
            return
        }
        viewModelScope.launch {
            repository.updateCallsign(trimmed)
            _statusToast.value = "Callsign diperbarui!"
        }
    }

    fun resetCareer() {
        viewModelScope.launch {
            repository.resetCareer()
            _worldState.value = engine.createInitialState(CareerProfile())
            _statusToast.value = "Progres karier dimulai ulang."
        }
    }

    private fun startGameLoop() {
        if (gameLoopJob?.isActive == true) return
        gameLoopJob = viewModelScope.launch {
            var lastTimeNs = System.nanoTime()
            while (isActive && _isPlaying.value) {
                val nowNs = System.nanoTime()
                val dt = ((nowNs - lastTimeNs) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
                lastTimeNs = nowNs

                if (!_isPaused.value) {
                    val career = currentCareerOrDefault()
                    val (wx, wy) = cameraController.screenStickToWorld(rawStickX, rawStickY)
                    val tickInput = currentInput.copy(moveX = wx, moveY = wy)
                    val (nextWorld, event) = engine.step(_worldState.value, dt, tickInput, career)
                    _worldState.value = nextWorld
                    soundEngine?.processWorldState(nextWorld, tickInput.isNitroPressed)
                    if (event != null) {
                        handleCareerEvent(event)
                    }
                }
                delay(16L)
            }
        }
    }

    private fun handleCareerEvent(event: CareerEvent) {
        pendingCashDelta += event.cashDelta
        pendingRespectDelta += event.respectDelta
        pendingMissionsDelta += event.missionsCompletedDelta
        pendingVehiclesDelta += event.vehiclesStolenDelta
        pendingHighScore = max(pendingHighScore, event.scoreUpdate)
        pendingMaxWanted = max(pendingMaxWanted, event.maxWantedSeen)

        val nowMs = System.currentTimeMillis()
        val isSignificant = event.runLogOutcome != null || event.missionsCompletedDelta > 0L
        if (isSignificant || nowMs - lastSyncMs >= 1500L) {
            flushPendingCareerSync()
            lastSyncMs = nowMs
        }

        if (event.runLogOutcome != null && event.runLogTitle != null) {
            val title = event.runLogTitle
            val outcome = event.runLogOutcome
            val cashEarned = event.runLogCash
            val wanted = event.runLogWanted.toLong()
            viewModelScope.launch {
                repository.recordMissionRun(
                    title = title,
                    outcome = outcome,
                    cashEarned = cashEarned,
                    wantedPeak = wanted,
                )
            }
        }
    }

    private fun flushPendingCareerSync() {
        if (pendingCashDelta == 0L &&
            pendingRespectDelta == 0L &&
            pendingMissionsDelta == 0L &&
            pendingVehiclesDelta == 0L &&
            pendingHighScore == 0L &&
            pendingMaxWanted == 0
        ) return

        val career = currentCareerOrDefault()
        val nextCash = max(0L, career.cash + pendingCashDelta)
        val nextRespect = career.respect + pendingRespectDelta
        val nextHighScore = max(career.highScore, pendingHighScore)
        val nextMissions = career.missionsCompleted + pendingMissionsDelta
        val nextStolen = career.vehiclesStolen + pendingVehiclesDelta
        val nextMaxWanted = max(career.maxWantedSurvived, pendingMaxWanted.toLong())

        pendingCashDelta = 0L
        pendingRespectDelta = 0L
        pendingMissionsDelta = 0L
        pendingVehiclesDelta = 0L
        pendingHighScore = 0L
        pendingMaxWanted = 0

        viewModelScope.launch {
            repository.saveCareerProgress(
                cash = nextCash,
                respect = nextRespect,
                highScore = nextHighScore,
                missionsCompleted = nextMissions,
                vehiclesStolen = nextStolen,
                maxWantedSurvived = nextMaxWanted,
                unlockedWeapons = career.unlockedWeapons,
                equippedWeapon = career.equippedWeapon,
                ownedVehicles = career.ownedVehicles,
                equippedVehicle = career.equippedVehicle,
                armorLevel = career.armorLevel,
                nitroLevel = career.nitroLevel,
            )
        }
    }

    private fun currentCareerOrDefault(): CareerProfile {
        return (careerState.value as? UiState.Success)?.data ?: CareerProfile()
    }

    override fun onCleared() {
        super.onCleared()
        loadingJob?.cancel()
        gameLoopJob?.cancel()
        menuMusicPlayer?.release()
        soundEngine?.release()
    }
}
