package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.audio.MenuMusicPlayer
import com.example.audio.SoundEffectsEngine
import com.example.data.CareerProfile
import com.example.data.CareerRepository
import com.example.ui.screens.CityLoadingScreen
import com.example.ui.screens.GamePlayScreen
import com.example.ui.screens.StartMenuScreen
import com.example.ui.theme.NeonViceTheme
import com.example.viewmodel.NeonViceViewModel
import com.example.viewmodel.UiState

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NeonViceTheme {
                NeonViceRoot()
            }
        }
    }
}

@Composable
private fun NeonViceRoot() {
    val viewModel: NeonViceViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                NeonViceViewModel(
                    repository = CareerRepository(app),
                    soundEngine = SoundEffectsEngine(),
                    menuMusicPlayer = MenuMusicPlayer(app),
                )
            }
        },
    )

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> viewModel.onAppBackgroundStateChanged(true)
                Lifecycle.Event.ON_PAUSE -> viewModel.onAppBackgroundStateChanged(false)
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val careerState by viewModel.careerState.collectAsStateWithLifecycle()
    val isLoadingCity by viewModel.isLoadingCity.collectAsStateWithLifecycle()
    val loadingProgress by viewModel.loadingProgress.collectAsStateWithLifecycle()
    val loadingStageText by viewModel.loadingStageText.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val isPaused by viewModel.isPaused.collectAsStateWithLifecycle()
    val worldState by viewModel.worldState.collectAsStateWithLifecycle()
    val isMuted by viewModel.isMuted.collectAsStateWithLifecycle()

    when {
        isPlaying -> {
            val activeCareer = (careerState as? UiState.Success)?.data ?: CareerProfile()
            GamePlayScreen(
                world = worldState,
                career = activeCareer,
                cameraController = viewModel.cameraController,
                isPaused = isPaused,
                isMuted = isMuted,
                onToggleMute = viewModel::toggleMute,
                onJoystickMove = viewModel::updateJoystick,
                onFiringChange = viewModel::setFiring,
                onNitroChange = viewModel::setNitroPressed,
                onHijackClick = viewModel::triggerHijack,
                onSwitchWeapon = viewModel::cycleEquippedWeapon,
                onSelectOrBuyWeapon = viewModel::selectOrBuyWeaponInGame,
                onPauseClick = viewModel::pauseGame,
                onResumeClick = viewModel::resumeGame,
                onSummonCarClick = viewModel::summonPersonalCar,
                onRespawnClick = viewModel::respawnPlayer,
                onExitToHub = viewModel::exitToHub,
            )
        }
        isLoadingCity -> {
            CityLoadingScreen(
                progress = loadingProgress,
                stageText = loadingStageText,
                onCancel = viewModel::cancelLoading,
            )
        }
        else -> {
            StartMenuScreen(
                onPlayClick = { viewModel.enterCity(null) },
            )
        }
    }
}
