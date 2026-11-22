package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SportsMma
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.CareerProfile
import com.example.data.WeaponType
import com.example.game.GameWorldState
import com.example.game.MinimapRadar
import com.example.game.render3d.CameraController
import com.example.game.render3d.City3DView
import kotlinx.coroutines.delay
import com.example.ui.theme.CashGreen
import com.example.ui.theme.DangerRed
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.PoliceBlue
import com.example.ui.theme.ViceGold
import com.example.ui.theme.ViceSurface
import com.example.ui.theme.ViceVoid
import kotlin.math.hypot
import kotlin.math.sin

@Composable
fun GamePlayScreen(
    world: GameWorldState,
    career: CareerProfile,
    cameraController: CameraController,
    isPaused: Boolean,
    isMuted: Boolean,
    onToggleMute: () -> Unit,
    onJoystickMove: (Float, Float) -> Unit,
    onFiringChange: (Boolean) -> Unit,
    onNitroChange: (Boolean) -> Unit,
    onHijackClick: () -> Unit,
    onSwitchWeapon: () -> Unit,
    onSelectOrBuyWeapon: (WeaponType) -> Boolean,
    onPauseClick: () -> Unit,
    onResumeClick: () -> Unit,
    onSummonCarClick: () -> Unit,
    onRespawnClick: () -> Unit,
    onExitToHub: () -> Unit,
) {
    var showWeaponSelector by remember { mutableStateOf(false) }
    var weaponToastText by remember { mutableStateOf<String?>(null) }
    var lastObservedWeapon by remember { mutableStateOf(world.equippedWeapon) }

    LaunchedEffect(world.equippedWeapon) {
        if (world.equippedWeapon != lastObservedWeapon) {
            lastObservedWeapon = world.equippedWeapon
            weaponToastText = "Senjata: ${world.equippedWeapon.displayName}"
        }
    }

    LaunchedEffect(weaponToastText) {
        if (weaponToastText != null) {
            delay(1800L)
            weaponToastText = null
        }
    }

    BackHandler {
        when {
            showWeaponSelector -> showWeaponSelector = false
            isPaused -> onResumeClick()
            else -> onPauseClick()
        }
    }

    var cameraModeLabel by remember { mutableStateOf(cameraController.mode.label) }
    val activeVehicle = remember(world.inVehicleId, world.vehicles) {
        world.inVehicleId?.let { vid -> world.vehicles.firstOrNull { it.id == vid } }
    }

    Box(modifier = Modifier.fillMaxSize().background(ViceVoid)) {
        // 60fps Hardware-Accelerated 3D OpenGL ES City Viewport
        City3DView(
            world = world,
            cameraController = cameraController,
            modifier = Modifier.fillMaxSize(),
        )

        // Landscape Top HUD Overlay
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            // Top-Left: Radar side-by-side with HP / Armor / Wanted Stars (keeps bottom joystick clear!)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top,
            ) {
                MinimapRadar(
                    world = world,
                    contentDesc = stringResource(R.string.cd_radar_minimap),
                    radarSize = 94.dp,
                    cameraYawRad = cameraController.activeYawRad,
                )
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    HudMeterBar(
                        fraction = (world.playerHp / world.playerMaxHp).coerceIn(0f, 1f),
                        fillColor = if (world.playerHp < 30f) DangerRed else CashGreen,
                        label = "HP ${world.playerHp.toInt()}",
                        icon = Icons.Filled.Favorite,
                    )
                    if (world.playerMaxArmor > 0f || world.playerArmor > 0f) {
                        val denom = maxOf(world.playerMaxArmor, 100f)
                        HudMeterBar(
                            fraction = (world.playerArmor / denom).coerceIn(0f, 1f),
                            fillColor = ElectricCyan,
                            label = "ARMOR ${world.playerArmor.toInt()}",
                            icon = Icons.Filled.Shield,
                        )
                    }
                    if (world.wantedLevel > 0) {
                        val flashBlue = sin(world.clockPulse * 14f) > 0f
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(ViceVoid.copy(alpha = 0.84f))
                                .padding(horizontal = 6.dp, vertical = 4.dp),
                        ) {
                            for (star in 1..5) {
                                val active = star <= world.wantedLevel
                                Icon(
                                    imageVector = Icons.Filled.Star,
                                    contentDescription = "Wanted level ${world.wantedLevel}",
                                    tint = when {
                                        !active -> Color.White.copy(alpha = 0.18f)
                                        flashBlue -> PoliceBlue
                                        else -> DangerRed
                                    },
                                    modifier = Modifier.size(15.dp),
                                )
                            }
                        }
                    }
                    if (world.bustedProgress > 0.05f) {
                        HudMeterBar(
                            fraction = world.bustedProgress.coerceIn(0f, 1f),
                            fillColor = PoliceBlue,
                            label = "TERKEPUNG!",
                        )
                    }
                }
            }

            // Top-Center: Active Mission / District Banner
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                world.activeMission?.let { mission ->
                    val targetDistM = (hypot(mission.targetX - world.playerX, mission.targetY - world.playerY) / 4f).toInt()
                    Surface(
                        color = ViceVoid.copy(alpha = 0.88f),
                        border = BorderStroke(1.dp, ViceGold),
                        shape = RoundedCornerShape(10.dp),
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = mission.template.title.uppercase(),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = ViceGold,
                                )
                                if (mission.template.type != com.example.game.MissionObjectiveType.ELIMINATE_SYNDICATE &&
                                    mission.template.type != com.example.game.MissionObjectiveType.SURVIVE_HEAT
                                ) {
                                    Text(
                                        text = "• ${targetDistM}m",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ElectricCyan,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                            Text(
                                text = mission.objectiveHint,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White,
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                            )
                        }
                    }
                }
                if (world.districtBannerTimerSec > 0f) {
                    Surface(
                        color = ViceVoid.copy(alpha = 0.8f),
                        border = BorderStroke(1.dp, Color(world.currentDistrict.accentHex)),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Text(
                            text = world.currentDistrict.title.uppercase(),
                            style = MaterialTheme.typography.labelLarge,
                            color = Color(world.currentDistrict.accentHex),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                    }
                }
            }

            // Top-Right: Compact Speedometer, 3D Camera Toggle, Cash, Weapon Switcher, Sound & Pause
            Row(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (activeVehicle != null) {
                    val kmh = (kotlin.math.abs(activeVehicle.speed) * 0.28f).toInt()
                    Surface(
                        color = ViceVoid.copy(alpha = 0.24f),
                        border = BorderStroke(1.dp, ViceGold.copy(alpha = 0.60f)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(34.dp),
                    ) {
                        Box(
                            modifier = Modifier.padding(horizontal = 7.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "$kmh KM/H",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = ViceGold,
                            )
                        }
                    }
                }

                Surface(
                    onClick = {
                        cameraModeLabel = cameraController.toggleMode().label
                    },
                    color = ViceVoid.copy(alpha = 0.18f),
                    border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.55f)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .height(34.dp)
                        .testTag("camera_mode_button"),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Videocam,
                            contentDescription = stringResource(R.string.cd_camera_mode),
                            tint = ElectricCyan.copy(alpha = 0.90f),
                            modifier = Modifier.size(14.dp),
                        )
                        Text(
                            text = cameraModeLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.92f),
                        )
                    }
                }

                Surface(
                    color = ViceVoid.copy(alpha = 0.24f),
                    border = BorderStroke(1.dp, CashGreen.copy(alpha = 0.58f)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(34.dp),
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 7.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "$${career.cash}",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = CashGreen,
                        )
                    }
                }

                Surface(
                    onClick = { showWeaponSelector = !showWeaponSelector },
                    color = ViceVoid.copy(alpha = 0.18f),
                    border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.58f)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .height(34.dp)
                        .testTag("weapon_switch_button"),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            imageVector = weaponIcon(world.equippedWeapon),
                            contentDescription = stringResource(R.string.cd_weapon_switch),
                            tint = ElectricCyan.copy(alpha = 0.90f),
                            modifier = Modifier.size(14.dp),
                        )
                        Text(
                            text = world.equippedWeapon.displayName,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.92f),
                            maxLines = 1,
                        )
                    }
                }

                IconButton(
                    onClick = onToggleMute,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(ViceVoid.copy(alpha = 0.18f))
                        .border(
                            1.dp,
                            if (isMuted) DangerRed.copy(alpha = 0.58f) else ElectricCyan.copy(alpha = 0.58f),
                            RoundedCornerShape(8.dp),
                        )
                        .testTag("sound_toggle_button"),
                ) {
                    Icon(
                        imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = stringResource(R.string.cd_sound_toggle),
                        tint = if (isMuted) DangerRed else ElectricCyan.copy(alpha = 0.90f),
                        modifier = Modifier.size(17.dp),
                    )
                }

                IconButton(
                    onClick = onPauseClick,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(ViceVoid.copy(alpha = 0.18f))
                        .border(1.dp, NeonMagenta.copy(alpha = 0.58f), RoundedCornerShape(8.dp))
                        .testTag("pause_menu_button"),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Pause,
                        contentDescription = stringResource(R.string.cd_pause_menu),
                        tint = Color.White.copy(alpha = 0.92f),
                        modifier = Modifier.size(17.dp),
                    )
                }
            }
        }

        // Dynamic Non-Terminal Banner (Mission Passed, Pay 'N' Spray, Welcome)
        val banner = world.banner
        AnimatedVisibility(
            visible = banner != null && !banner.isTerminalOverlay,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 48.dp),
        ) {
            if (banner != null) {
                Surface(
                    color = ViceVoid.copy(alpha = 0.92f),
                    border = BorderStroke(2.dp, Color(banner.accentHex)),
                    shape = RoundedCornerShape(18.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = banner.headline,
                            style = MaterialTheme.typography.headlineLarge,
                            color = Color(banner.accentHex),
                            textAlign = TextAlign.Center,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = banner.subline,
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }

        // Brief Weapon Switch Confirmation Pill
        AnimatedVisibility(
            visible = weaponToastText != null && !showWeaponSelector,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(top = 46.dp, end = 14.dp),
        ) {
            weaponToastText?.let { toast ->
                Surface(
                    color = ViceVoid.copy(alpha = 0.9f),
                    border = BorderStroke(1.dp, ElectricCyan),
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Text(
                        text = toast,
                        style = MaterialTheme.typography.labelLarge,
                        color = ElectricCyan,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }
            }
        }

        // Landscape Bottom Controls (Left Analog Joystick + Right Ergonomic Action Cluster)
        if (!world.isDeadOrBusted && !isPaused && !showWeaponSelector) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                VirtualJoystick(
                    onMove = onJoystickMove,
                    modifier = Modifier.testTag("virtual_joystick"),
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    // Quick-Swap Weapon Button right by thumb
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Surface(
                            onClick = onSwitchWeapon,
                            shape = CircleShape,
                            color = ViceVoid.copy(alpha = 0.16f),
                            border = BorderStroke(1.5.dp, ViceGold.copy(alpha = 0.62f)),
                            modifier = Modifier
                                .size(50.dp)
                                .testTag("weapon_quick_swap_button"),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.SwapHoriz,
                                    contentDescription = stringResource(R.string.cd_weapon_switch),
                                    tint = ViceGold.copy(alpha = 0.88f),
                                    modifier = Modifier.size(24.dp),
                                )
                            }
                        }
                        ActionMicroLabel("GANTI", ViceGold)
                    }

                    val canHijack = world.inVehicleId != null || world.nearestVehicleId != null
                    val hijackAccent = when {
                        world.inVehicleId != null -> ViceGold
                        world.nearestVehicleId != null -> ElectricCyan
                        else -> Color.White.copy(alpha = 0.35f)
                    }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Surface(
                            onClick = onHijackClick,
                            enabled = canHijack,
                            shape = CircleShape,
                            color = ViceVoid.copy(alpha = 0.16f),
                            border = BorderStroke(1.5.dp, hijackAccent.copy(alpha = if (canHijack) 0.62f else 0.30f)),
                            modifier = Modifier
                                .size(54.dp)
                                .testTag("hijack_button"),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.DirectionsCar,
                                    contentDescription = stringResource(R.string.cd_hijack_button),
                                    tint = hijackAccent.copy(alpha = if (canHijack) 0.88f else 0.40f),
                                    modifier = Modifier.size(26.dp),
                                )
                            }
                        }
                        ActionMicroLabel(if (world.inVehicleId != null) "KELUAR" else "MOBIL", hijackAccent)
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        HoldActionButton(
                            accent = ElectricCyan,
                            icon = Icons.Filled.Bolt,
                            contentDesc = stringResource(R.string.cd_nitro_button),
                            sizeDp = 54,
                            onHoldChange = onNitroChange,
                            modifier = Modifier.testTag("nitro_button"),
                        )
                        ActionMicroLabel(if (world.inVehicleId != null) "NITRO" else "LARI", ElectricCyan)
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        HoldActionButton(
                            accent = NeonMagenta,
                            icon = weaponIcon(world.equippedWeapon),
                            contentDesc = stringResource(R.string.cd_fire_button),
                            sizeDp = 74,
                            onHoldChange = onFiringChange,
                            modifier = Modifier.testTag("fire_button"),
                        )
                        ActionMicroLabel(
                            if (world.equippedWeapon.isMelee) "CAKAR" else "TEMBAK",
                            NeonMagenta,
                        )
                    }
                }
            }
        }

        // In-Game Interactive Weapon Selector Overlay
        if (showWeaponSelector && !world.isDeadOrBusted && !isPaused) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ViceVoid.copy(alpha = 0.78f))
                    .windowInsetsPadding(WindowInsets.safeDrawing),
                contentAlignment = Alignment.Center,
            ) {
                Card(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth()
                        .widthIn(max = 760.dp),
                    colors = CardDefaults.cardColors(containerColor = ViceSurface),
                    border = BorderStroke(1.5.dp, ElectricCyan),
                    shape = RoundedCornerShape(22.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column {
                                Text(
                                    text = "GANTI SENJATA",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = ElectricCyan,
                                )
                                Text(
                                    text = "Uang saat ini: $${career.cash} • Pilih atau beli langsung senjata di lapangan",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.75f),
                                )
                            }
                            IconButton(
                                onClick = { showWeaponSelector = false },
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("close_weapon_selector_button"),
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Tutup pemilih senjata",
                                    tint = Color.White,
                                )
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            WeaponType.entries.forEach { weapon ->
                                val owned = career.unlockedWeapons.contains(weapon.id)
                                val equipped = world.equippedWeapon == weapon
                                val canBuy = !owned && career.cash >= weapon.price
                                val accent = when {
                                    equipped -> CashGreen
                                    owned -> ElectricCyan
                                    canBuy -> ViceGold
                                    else -> Color.White.copy(alpha = 0.35f)
                                }
                                Surface(
                                    onClick = {
                                        if (owned || canBuy) {
                                            val ok = onSelectOrBuyWeapon(weapon)
                                            if (ok) {
                                                showWeaponSelector = false
                                            }
                                        } else {
                                            weaponToastText = "Butuh $${weapon.price} untuk ${weapon.displayName}"
                                        }
                                    },
                                    color = ViceVoid.copy(alpha = 0.85f),
                                    border = BorderStroke(if (equipped) 2.dp else 1.dp, accent),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier
                                        .width(138.dp)
                                        .testTag("select_weapon_${weapon.id.lowercase()}"),
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(6.dp),
                                    ) {
                                        Icon(
                                            imageVector = if (owned || canBuy) weaponIcon(weapon) else Icons.Filled.Lock,
                                            contentDescription = weapon.displayName,
                                            tint = accent,
                                            modifier = Modifier.size(30.dp),
                                        )
                                        Text(
                                            text = weapon.displayName,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = Color.White,
                                            textAlign = TextAlign.Center,
                                            maxLines = 1,
                                        )
                                        val totalDmg = weapon.damage * weapon.pellets.coerceAtLeast(1)
                                        val dmgFrac = (totalDmg / 120f).coerceIn(0.18f, 1f)
                                        val ratePerSec = 1000f / weapon.fireIntervalMs.coerceAtLeast(80L).toFloat()
                                        val rateFrac = (ratePerSec / 9.2f).coerceIn(0.18f, 1f)
                                        Column(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalArrangement = Arrangement.spacedBy(3.dp),
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                            ) {
                                                Text("DMG", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
                                                Text(
                                                    "${weapon.damage.toInt()}${if (weapon.pellets > 1) "x${weapon.pellets}" else ""}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color.White,
                                                )
                                            }
                                            LinearProgressIndicator(
                                                progress = { dmgFrac },
                                                color = accent,
                                                trackColor = Color.White.copy(alpha = 0.14f),
                                                modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                                            )
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                            ) {
                                                Text("LAJU", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
                                                Text(
                                                    if (weapon.isMelee) "CEPAT" else "${ratePerSec.toInt()}/s",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color.White,
                                                )
                                            }
                                            LinearProgressIndicator(
                                                progress = { rateFrac },
                                                color = ElectricCyan,
                                                trackColor = Color.White.copy(alpha = 0.14f),
                                                modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                                            )
                                        }
                                        Surface(
                                            color = accent.copy(alpha = 0.18f),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.fillMaxWidth(),
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(vertical = 6.dp, horizontal = 6.dp),
                                                horizontalArrangement = Arrangement.Center,
                                                verticalAlignment = Alignment.CenterVertically,
                                            ) {
                                                if (equipped) {
                                                    Icon(
                                                        imageVector = Icons.Filled.CheckCircle,
                                                        contentDescription = null,
                                                        tint = CashGreen,
                                                        modifier = Modifier.size(14.dp),
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                }
                                                Text(
                                                    text = when {
                                                        equipped -> "DIPAKAI"
                                                        owned -> "PAKAI"
                                                        else -> "$${weapon.price}"
                                                    },
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = accent,
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Terminal WASTED / BUSTED Overlay
        if (world.isDeadOrBusted && banner != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(banner.accentHex).copy(alpha = 0.25f))
                    .background(ViceVoid.copy(alpha = 0.72f))
                    .windowInsetsPadding(WindowInsets.safeDrawing),
                contentAlignment = Alignment.Center,
            ) {
                Card(
                    modifier = Modifier
                        .padding(16.dp)
                        .widthIn(max = 460.dp),
                    colors = CardDefaults.cardColors(containerColor = ViceSurface),
                    border = BorderStroke(2.dp, Color(banner.accentHex)),
                    shape = RoundedCornerShape(22.dp),
                ) {
                    Column(
                        modifier = Modifier
                            .padding(20.dp)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(
                            text = banner.headline,
                            style = MaterialTheme.typography.displayLarge,
                            color = Color(banner.accentHex),
                        )
                        Text(
                            text = banner.subline,
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                        )
                        Button(
                            onClick = onRespawnClick,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(banner.accentHex)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("respawn_button"),
                        ) {
                            Text(
                                text = "LANJUT DARI SAFE HOUSE",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
        }

        // In-Game Phone Pause Menu
        if (isPaused) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ViceVoid.copy(alpha = 0.82f))
                    .windowInsetsPadding(WindowInsets.safeDrawing),
                contentAlignment = Alignment.Center,
            ) {
                Card(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth()
                        .widthIn(max = 480.dp),
                    colors = CardDefaults.cardColors(containerColor = ViceSurface),
                    border = BorderStroke(1.5.dp, ElectricCyan),
                    shape = RoundedCornerShape(22.dp),
                ) {
                    Column(
                        modifier = Modifier
                            .padding(18.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(
                            text = "TELEPON GTA NEON CITY",
                            style = MaterialTheme.typography.headlineMedium,
                            color = ElectricCyan,
                        )
                        Text(
                            text = "Distrik: ${world.currentDistrict.title} • Skor Sesi: ${world.sessionScore}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Button(
                            onClick = onResumeClick,
                            colors = ButtonDefaults.buttonColors(containerColor = NeonMagenta),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("resume_game_button"),
                        ) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("LANJUTKAN AKSI", style = MaterialTheme.typography.titleMedium)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            OutlinedButton(
                                onClick = onSummonCarClick,
                                border = BorderStroke(1.dp, ElectricCyan),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("summon_car_button"),
                            ) {
                                Icon(Icons.Filled.DirectionsCar, contentDescription = null, tint = ElectricCyan)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("PANGGIL MOBIL", color = ElectricCyan, maxLines = 1)
                            }
                            OutlinedButton(
                                onClick = onExitToHub,
                                border = BorderStroke(1.dp, ViceGold),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("exit_to_hub_button"),
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, tint = ViceGold)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("MENU AWAL", color = ViceGold, maxLines = 1)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionMicroLabel(
    text: String,
    color: Color,
) {
    Surface(
        color = ViceVoid.copy(alpha = 0.24f),
        shape = RoundedCornerShape(4.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color.copy(alpha = 0.90f),
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
        )
    }
}

@Composable
private fun HudMeterBar(
    fraction: Float,
    fillColor: Color,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
) {
    Column(
        modifier = Modifier
            .width(110.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(ViceVoid.copy(alpha = 0.86f))
            .border(1.dp, fillColor.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = fillColor,
                    modifier = Modifier.size(11.dp),
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
        }
        LinearProgressIndicator(
            progress = { fraction },
            color = fillColor,
            trackColor = Color.White.copy(alpha = 0.16f),
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
        )
    }
}

@Composable
private fun VirtualJoystick(
    onMove: (Float, Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    var knobOffset by remember { mutableStateOf(Offset.Zero) }
    val outerRadiusPx = 105f

    Box(
        modifier = modifier
            .size(124.dp)
            .semantics { contentDescription = "Analog gerak dan kemudi" }
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val center = Offset(size.width / 2f, size.height / 2f)
                    fun updateFromPosition(pos: Offset) {
                        val dx = pos.x - center.x
                        val dy = pos.y - center.y
                        val dist = hypot(dx, dy)
                        val clamped = if (dist > outerRadiusPx) {
                            Offset(dx / dist * outerRadiusPx, dy / dist * outerRadiusPx)
                        } else {
                            Offset(dx, dy)
                        }
                        knobOffset = clamped
                        onMove(clamped.x / outerRadiusPx, clamped.y / outerRadiusPx)
                    }
                    updateFromPosition(down.position)
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) break
                        updateFromPosition(change.position)
                    }
                    knobOffset = Offset.Zero
                    onMove(0f, 0f)
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val outerR = size.minDimension / 2f - 4f
            drawCircle(
                color = ViceVoid.copy(alpha = 0.14f),
                radius = outerR,
                center = center,
            )
            drawCircle(
                color = ElectricCyan.copy(alpha = 0.45f),
                radius = outerR,
                center = center,
                style = Stroke(width = 2.5f),
            )
            drawCircle(
                color = ElectricCyan.copy(alpha = 0.16f),
                radius = outerR * 0.62f,
                center = center,
                style = Stroke(width = 1.5f),
            )
            // Subtle cardinal axis ticks
            val tickLen = 8.dp.toPx()
            drawLine(ElectricCyan.copy(alpha = 0.40f), Offset(center.x, center.y - outerR + 3f), Offset(center.x, center.y - outerR + tickLen), strokeWidth = 2f)
            drawLine(ElectricCyan.copy(alpha = 0.40f), Offset(center.x, center.y + outerR - 3f), Offset(center.x, center.y + outerR - tickLen), strokeWidth = 2f)
            drawLine(ElectricCyan.copy(alpha = 0.40f), Offset(center.x - outerR + 3f, center.y), Offset(center.x - outerR + tickLen, center.y), strokeWidth = 2f)
            drawLine(ElectricCyan.copy(alpha = 0.40f), Offset(center.x + outerR - 3f, center.y), Offset(center.x + outerR - tickLen, center.y), strokeWidth = 2f)

            drawCircle(
                color = NeonMagenta.copy(alpha = 0.34f),
                radius = 23.dp.toPx(),
                center = center + knobOffset,
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.62f),
                radius = 23.dp.toPx(),
                center = center + knobOffset,
                style = Stroke(width = 2.2f),
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.30f),
                radius = 9.dp.toPx(),
                center = center + knobOffset,
            )
        }
    }
}

@Composable
private fun HoldActionButton(
    accent: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDesc: String,
    sizeDp: Int,
    onHoldChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var pressed by remember { mutableStateOf(false) }
    Box(
        modifier = modifier
            .size(sizeDp.dp)
            .clip(CircleShape)
            .background(
                if (pressed) accent.copy(alpha = 0.38f) else ViceVoid.copy(alpha = 0.16f)
            )
            .border(
                BorderStroke(1.8.dp, accent.copy(alpha = if (pressed) 0.92f else 0.62f)),
                CircleShape,
            )
            .semantics { contentDescription = contentDesc }
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    pressed = true
                    onHoldChange(true)
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) break
                    }
                    pressed = false
                    onHoldChange(false)
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDesc,
            tint = if (pressed) Color.White else accent.copy(alpha = 0.88f),
            modifier = Modifier.size((sizeDp * 0.46f).dp),
        )
    }
}

private fun weaponIcon(weapon: WeaponType): androidx.compose.ui.graphics.vector.ImageVector = when (weapon) {
    WeaponType.FISTS -> Icons.Filled.SportsMma
    WeaponType.PLASMA -> Icons.Filled.Bolt
    else -> Icons.Filled.GpsFixed
}
