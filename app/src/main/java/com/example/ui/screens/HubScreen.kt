package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.CareerProfile
import com.example.data.MissionRun
import com.example.data.RunOutcome
import com.example.data.VehicleCatalog
import com.example.data.WeaponType
import com.example.game.CityMap
import com.example.game.MissionTemplate
import com.example.ui.theme.CashGreen
import com.example.ui.theme.DangerRed
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.OutlineSubtle
import com.example.ui.theme.PoliceBlue
import com.example.ui.theme.ViceGold
import com.example.ui.theme.ViceSurface
import com.example.ui.theme.ViceSurfaceElevated
import com.example.ui.theme.ViceVoid
import com.example.viewmodel.HubTab
import com.example.viewmodel.UiState

@Composable
fun HubScreen(
    careerState: UiState<CareerProfile>,
    recentRunsState: UiState<List<MissionRun>>,
    selectedTab: HubTab,
    statusToast: String?,
    onClearToast: () -> Unit,
    onSelectTab: (HubTab) -> Unit,
    onEnterCity: (MissionTemplate?) -> Unit,
    onBuyOrEquipWeapon: (WeaponType) -> Unit,
    onBuyOrEquipVehicle: (VehicleCatalog) -> Unit,
    onUpgradeArmor: () -> Unit,
    onUpgradeNitro: () -> Unit,
    onUpdateCallsign: (String) -> Unit,
    onResetCareer: () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(statusToast) {
        if (statusToast != null) {
            snackbarHostState.showSnackbar(statusToast)
            onClearToast()
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize().background(ViceVoid)) {
        val isWide = maxWidth >= 600.dp

        if (isWide) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing),
            ) {
                NavigationRail(
                    containerColor = ViceSurface,
                    modifier = Modifier.fillMaxHeight(),
                ) {
                    Spacer(modifier = Modifier.height(12.dp))
                    HubTab.entries.forEach { tab ->
                        NavigationRailItem(
                            selected = selectedTab == tab,
                            onClick = { onSelectTab(tab) },
                            icon = { Icon(tabIcon(tab), contentDescription = tabLabel(tab)) },
                            label = { Text(tabLabel(tab)) },
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = ViceVoid,
                                selectedTextColor = ElectricCyan,
                                indicatorColor = ElectricCyan,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                            modifier = Modifier.testTag("nav_${tab.name.lowercase()}"),
                        )
                    }
                }
                Scaffold(
                    containerColor = ViceVoid,
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    contentWindowInsets = WindowInsets(0, 0, 0, 0),
                    modifier = Modifier.weight(1f),
                ) { innerPadding ->
                    HubBody(
                        innerPadding = innerPadding,
                        isWide = true,
                        careerState = careerState,
                        recentRunsState = recentRunsState,
                        selectedTab = selectedTab,
                        onEnterCity = onEnterCity,
                        onSelectTab = onSelectTab,
                        onBuyOrEquipWeapon = onBuyOrEquipWeapon,
                        onBuyOrEquipVehicle = onBuyOrEquipVehicle,
                        onUpgradeArmor = onUpgradeArmor,
                        onUpgradeNitro = onUpgradeNitro,
                        onUpdateCallsign = onUpdateCallsign,
                        onResetCareer = onResetCareer,
                    )
                }
            }
        } else {
            Scaffold(
                containerColor = ViceVoid,
                snackbarHost = { SnackbarHost(snackbarHostState) },
                bottomBar = {
                    NavigationBar(
                        containerColor = ViceSurface,
                        windowInsets = WindowInsets.navigationBars,
                    ) {
                        HubTab.entries.forEach { tab ->
                            NavigationBarItem(
                                selected = selectedTab == tab,
                                onClick = { onSelectTab(tab) },
                                icon = { Icon(tabIcon(tab), contentDescription = tabLabel(tab)) },
                                label = { Text(tabLabel(tab)) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = ViceVoid,
                                    selectedTextColor = ElectricCyan,
                                    indicatorColor = ElectricCyan,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                ),
                                modifier = Modifier.testTag("nav_${tab.name.lowercase()}"),
                            )
                        }
                    }
                },
            ) { innerPadding ->
                HubBody(
                    innerPadding = innerPadding,
                    isWide = false,
                    careerState = careerState,
                    recentRunsState = recentRunsState,
                    selectedTab = selectedTab,
                    onEnterCity = onEnterCity,
                    onSelectTab = onSelectTab,
                    onBuyOrEquipWeapon = onBuyOrEquipWeapon,
                    onBuyOrEquipVehicle = onBuyOrEquipVehicle,
                    onUpgradeArmor = onUpgradeArmor,
                    onUpgradeNitro = onUpgradeNitro,
                    onUpdateCallsign = onUpdateCallsign,
                    onResetCareer = onResetCareer,
                )
            }
        }
    }
}

@Composable
private fun HubBody(
    innerPadding: PaddingValues,
    isWide: Boolean,
    careerState: UiState<CareerProfile>,
    recentRunsState: UiState<List<MissionRun>>,
    selectedTab: HubTab,
    onEnterCity: (MissionTemplate?) -> Unit,
    onSelectTab: (HubTab) -> Unit,
    onBuyOrEquipWeapon: (WeaponType) -> Unit,
    onBuyOrEquipVehicle: (VehicleCatalog) -> Unit,
    onUpgradeArmor: () -> Unit,
    onUpgradeNitro: () -> Unit,
    onUpdateCallsign: (String) -> Unit,
    onResetCareer: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
        contentAlignment = Alignment.TopCenter,
    ) {
        when (careerState) {
            is UiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = ElectricCyan)
                }
            }
            is UiState.Error -> {
                Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = ViceSurface),
                        border = BorderStroke(1.dp, DangerRed),
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Text("Data Karier Bermasalah", style = MaterialTheme.typography.titleLarge, color = DangerRed)
                            Text(careerState.message, style = MaterialTheme.typography.bodyMedium)
                            OutlinedButton(onClick = onResetCareer) {
                                Text("Reset Data Lokal")
                            }
                        }
                    }
                }
            }
            is UiState.Success -> {
                val career = careerState.data
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(if (selectedTab == HubTab.CITY) Modifier else Modifier.widthIn(max = 1040.dp)),
                ) {
                    when (selectedTab) {
                        HubTab.CITY -> CityOverviewTab(
                            career = career,
                            isWide = isWide,
                            onEnterCity = { onEnterCity(null) },
                            onSelectTab = onSelectTab,
                        )
                        HubTab.MISSIONS -> MissionsTab(
                            isWide = isWide,
                            onStartMission = { onEnterCity(it) },
                        )
                        HubTab.BLACK_MARKET -> BlackMarketTab(
                            career = career,
                            isWide = isWide,
                            onBuyOrEquipWeapon = onBuyOrEquipWeapon,
                            onBuyOrEquipVehicle = onBuyOrEquipVehicle,
                            onUpgradeArmor = onUpgradeArmor,
                            onUpgradeNitro = onUpgradeNitro,
                        )
                        HubTab.CAREER -> CareerTab(
                            career = career,
                            recentRunsState = recentRunsState,
                            onUpdateCallsign = onUpdateCallsign,
                            onResetCareer = onResetCareer,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CityOverviewTab(
    career: CareerProfile,
    isWide: Boolean,
    onEnterCity: () -> Unit,
    onSelectTab: (HubTab) -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Simple, sleek atmospheric Vice sunset-skyline backdrop
        StartMenuBackdrop(modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 980.dp)
                .align(Alignment.Center)
                .then(if (isWide) Modifier else Modifier.verticalScroll(rememberScrollState()))
                .padding(horizontal = if (isWide) 28.dp else 18.dp, vertical = if (isWide) 18.dp else 16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            // Top Compact Status Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        color = ViceVoid.copy(alpha = 0.58f),
                        border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.55f)),
                        shape = RoundedCornerShape(50),
                    ) {
                        Text(
                            text = stringResource(R.string.tagline_subtitle),
                            style = MaterialTheme.typography.labelMedium,
                            color = ElectricCyan,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        )
                    }
                    Surface(
                        color = NeonMagenta.copy(alpha = 0.22f),
                        border = BorderStroke(1.dp, NeonMagenta.copy(alpha = 0.75f)),
                        shape = RoundedCornerShape(50),
                    ) {
                        Text(
                            text = "VERSI KUCING",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CompactMenuStat("UANG", "$${career.cash}", CashGreen)
                    CompactMenuStat("RESPECT", "${career.respect}", ViceGold)
                    CompactMenuStat("MISI", "${career.missionsCompleted}", NeonMagenta)
                }
            }

            // Center Hero Title & Clean CTA
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = if (isWide) 8.dp else 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "GTA NEON CITY",
                        style = MaterialTheme.typography.displayLarge,
                        color = Color.White,
                    )
                    Box(
                        modifier = Modifier
                            .width(120.dp)
                            .height(4.dp)
                            .background(
                                Brush.horizontalGradient(listOf(NeonMagenta, ElectricCyan)),
                                shape = RoundedCornerShape(2.dp),
                            )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Kucing Agen ${career.callsign}  •  ${WeaponType.fromId(career.equippedWeapon).displayName}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.85f),
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Button(
                        onClick = onEnterCity,
                        colors = ButtonDefaults.buttonColors(containerColor = NeonMagenta, contentColor = Color.White),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .height(56.dp)
                            .widthIn(min = 210.dp)
                            .testTag("enter_city_button"),
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(26.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.btn_enter_city),
                            style = MaterialTheme.typography.titleLarge,
                        )
                    }

                    if (isWide) {
                        OutlinedButton(
                            onClick = { onSelectTab(HubTab.MISSIONS) },
                            border = BorderStroke(1.dp, ViceGold.copy(alpha = 0.75f)),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.height(52.dp),
                        ) {
                            Icon(Icons.Filled.Flag, contentDescription = null, tint = ViceGold, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("PILIH MISI", color = ViceGold, fontWeight = FontWeight.SemiBold)
                        }
                        OutlinedButton(
                            onClick = { onSelectTab(HubTab.BLACK_MARKET) },
                            border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.75f)),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.height(52.dp),
                        ) {
                            Icon(Icons.Filled.ShoppingCart, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("ARSENAL", color = ElectricCyan, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // Bottom Minimal Control Strip (clean & uncluttered)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ControlHintChip(Icons.Filled.DirectionsCar, "Bajak Mobil", Modifier.weight(1f))
                ControlHintChip(Icons.Filled.GpsFixed, "Tembak", Modifier.weight(1f))
                ControlHintChip(Icons.Filled.SwapHoriz, "Ganti Senjata", Modifier.weight(1f))
                if (isWide) {
                    ControlHintChip(Icons.Filled.Bolt, "Nitro / Sprint", Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun StartMenuBackdrop(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Clean atmospheric Vice twilight gradient
        drawRect(
            brush = Brush.verticalGradient(
                colorStops = arrayOf(
                    0.00f to Color(0xFF070914),
                    0.42f to Color(0xFF12112B),
                    0.74f to Color(0xFF28143D),
                    1.00f to Color(0xFF471A47),
                )
            )
        )

        // Subtle warm twilight sun/moon orb near the right horizon
        val orbCenter = Offset(w * 0.76f, h * 0.52f)
        val orbRadius = minOf(w, h) * 0.36f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0x66FF5599),
                    Color(0x28FF8A4C),
                    Color.Transparent,
                ),
                center = orbCenter,
                radius = orbRadius * 1.45f,
            ),
            radius = orbRadius * 1.45f,
            center = orbCenter,
        )
        drawCircle(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0x88FFCC70), Color(0x55FF3B8D)),
                startY = orbCenter.y - orbRadius * 0.45f,
                endY = orbCenter.y + orbRadius * 0.45f,
            ),
            radius = orbRadius * 0.44f,
            center = orbCenter,
        )

        // Minimalist distant city skyline silhouettes along the bottom edge
        val horizonY = h * 0.78f
        val skylineColor = Color(0xFF0B0C1A)
        val towers = listOf(
            0.00f to 0.14f, 0.06f to 0.22f, 0.12f to 0.16f, 0.19f to 0.28f,
            0.26f to 0.18f, 0.34f to 0.24f, 0.42f to 0.15f, 0.50f to 0.26f,
            0.57f to 0.20f, 0.65f to 0.31f, 0.72f to 0.19f, 0.80f to 0.27f,
            0.88f to 0.21f, 0.94f to 0.16f,
        )
        towers.forEachIndexed { index, (xFrac, hFrac) ->
            val bw = w * 0.068f
            val bh = h * hFrac
            drawRect(
                color = skylineColor,
                topLeft = Offset(w * xFrac, horizonY - bh * 0.65f),
                size = Size(bw, h - (horizonY - bh * 0.65f)),
            )
            // Subtle single vertical neon window strip on select towers
            if (index % 3 == 1) {
                drawRect(
                    color = if (index % 2 == 0) Color(0x3300F0FF) else Color(0x33FF2E93),
                    topLeft = Offset(w * xFrac + bw * 0.42f, horizonY - bh * 0.52f),
                    size = Size(bw * 0.12f, bh * 0.36f),
                )
            }
        }

        // Clean palm silhouette on bottom right
        val palmBase = Offset(w * 0.90f, h)
        val palmTop = Offset(w * 0.86f, h * 0.62f)
        drawLine(
            color = Color(0xFF070812),
            start = palmBase,
            end = palmTop,
            strokeWidth = 8f,
        )
        val frondAngles = listOf(-145f, -110f, -65f, -25f, 15f)
        frondAngles.forEach { deg ->
            val rad = Math.toRadians(deg.toDouble())
            val end = Offset(
                palmTop.x + kotlin.math.cos(rad).toFloat() * minOf(w, h) * 0.14f,
                palmTop.y + kotlin.math.sin(rad).toFloat() * minOf(w, h) * 0.11f + 16f,
            )
            val frondPath = Path().apply {
                moveTo(palmTop.x, palmTop.y)
                quadraticTo(
                    (palmTop.x + end.x) * 0.5f,
                    palmTop.y - 18f,
                    end.x,
                    end.y,
                )
            }
            drawPath(
                path = frondPath,
                color = Color(0xFF070812),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 6f),
            )
        }

        // Crisp subtle bottom ground shadow & neon horizon hairline
        drawRect(
            color = Color(0xFF070812),
            topLeft = Offset(0f, horizonY + h * 0.04f),
            size = Size(w, h),
        )
        drawLine(
            brush = Brush.horizontalGradient(
                listOf(Color.Transparent, Color(0x6600F0FF), Color(0x66FF2E93), Color.Transparent)
            ),
            start = Offset(0f, horizonY + h * 0.04f),
            end = Offset(w, horizonY + h * 0.04f),
            strokeWidth = 2f,
        )
    }
}

@Composable
private fun CompactMenuStat(label: String, value: String, accent: Color) {
    Surface(
        color = ViceVoid.copy(alpha = 0.64f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.68f))
            Text(value, style = MaterialTheme.typography.titleMedium, color = accent, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ControlHintChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = ViceVoid.copy(alpha = 0.58f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)),
        shape = RoundedCornerShape(10.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.85f), maxLines = 1)
        }
    }
}

@Composable
private fun MissionsTab(
    isWide: Boolean,
    onStartMission: (MissionTemplate) -> Unit,
) {
    val rows = if (isWide) CityMap.missions.chunked(2) else CityMap.missions.map { listOf(it) }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("KONTRAK & MISI JALANAN", style = MaterialTheme.typography.headlineMedium, color = ViceGold)
            Text(
                text = "Pilih misi untuk langsung diterjunkan ke kota dengan penanda GPS aktif.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        items(rows) { rowMissions ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                rowMissions.forEach { mission ->
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = ViceSurface),
                        border = BorderStroke(1.dp, ViceGold.copy(alpha = 0.45f)),
                        shape = RoundedCornerShape(18.dp),
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = mission.title.uppercase(),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = Color.White,
                                )
                                Surface(
                                    color = CashGreen.copy(alpha = 0.16f),
                                    border = BorderStroke(1.dp, CashGreen),
                                    shape = RoundedCornerShape(8.dp),
                                ) {
                                    Text(
                                        text = "+$${mission.rewardCash}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = CashGreen,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    )
                                }
                            }
                            Text(
                                text = mission.brief,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Button(
                                onClick = { onStartMission(mission) },
                                colors = ButtonDefaults.buttonColors(containerColor = ViceGold, contentColor = ViceVoid),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("start_mission_${mission.id}"),
                            ) {
                                Icon(Icons.Filled.Flag, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("MULAI MISI", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                if (isWide && rowMissions.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun BlackMarketTab(
    career: CareerProfile,
    isWide: Boolean,
    onBuyOrEquipWeapon: (WeaponType) -> Unit,
    onBuyOrEquipVehicle: (VehicleCatalog) -> Unit,
    onUpgradeArmor: () -> Unit,
    onUpgradeNitro: () -> Unit,
) {
    val weaponRows = if (isWide) WeaponType.entries.chunked(2) else WeaponType.entries.map { listOf(it) }
    val vehicleRows = if (isWide) VehicleCatalog.entries.chunked(2) else VehicleCatalog.entries.map { listOf(it) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("PASAR GELAP & GARASI", style = MaterialTheme.typography.headlineMedium, color = NeonMagenta)
                Surface(
                    color = CashGreen.copy(alpha = 0.16f),
                    border = BorderStroke(1.dp, CashGreen),
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Text(
                        text = "$${career.cash}",
                        style = MaterialTheme.typography.titleLarge,
                        color = CashGreen,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }
            }
        }

        item { Text("ARSENAL SENJATA", style = MaterialTheme.typography.titleLarge, color = ElectricCyan) }
        items(weaponRows) { rowWeapons ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                rowWeapons.forEach { weapon ->
                    val owned = career.unlockedWeapons.contains(weapon.id)
                    val equipped = career.equippedWeapon == weapon.id
                    ShopCard(
                        title = weapon.displayName,
                        subtitle = weapon.description,
                        price = weapon.price,
                        owned = owned,
                        equipped = equipped,
                        accent = NeonMagenta,
                        testTag = "weapon_card_${weapon.id}",
                        modifier = Modifier.weight(1f),
                        onAction = { onBuyOrEquipWeapon(weapon) },
                    )
                }
                if (isWide && rowWeapons.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
        }

        item {
            Spacer(modifier = Modifier.height(4.dp))
            Text("GARASI KENDARAAN PRIBADI", style = MaterialTheme.typography.titleLarge, color = ElectricCyan)
        }
        items(vehicleRows) { rowVehicles ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                rowVehicles.forEach { vehicle ->
                    val owned = career.ownedVehicles.contains(vehicle.id)
                    val equipped = career.equippedVehicle == vehicle.id
                    ShopCard(
                        title = vehicle.displayName,
                        subtitle = "${vehicle.subtitle} (Top Speed ${vehicle.maxSpeed.toInt()})",
                        price = vehicle.price,
                        owned = owned,
                        equipped = equipped,
                        accent = ElectricCyan,
                        testTag = "vehicle_card_${vehicle.id}",
                        modifier = Modifier.weight(1f),
                        onAction = { onBuyOrEquipVehicle(vehicle) },
                    )
                }
                if (isWide && rowVehicles.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
        }

        item {
            Spacer(modifier = Modifier.height(4.dp))
            Text("UPGRADE TAKTIS", style = MaterialTheme.typography.titleLarge, color = ViceGold)
        }
        item {
            val armorCost = (career.armorLevel + 1L) * 700L
            val nitroCost = (career.nitroLevel + 1L) * 650L
            if (isWide) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    UpgradeCard(
                        title = "Rompi Anti Peluru (Lv ${career.armorLevel}/3)",
                        subtitle = "+35 Armor per level saat turun ke kota.",
                        isMaxed = career.armorLevel >= 3L,
                        cost = armorCost,
                        icon = Icons.Filled.Shield,
                        accent = ElectricCyan,
                        testTag = "upgrade_armor_button",
                        modifier = Modifier.weight(1f),
                        onUpgrade = onUpgradeArmor,
                    )
                    UpgradeCard(
                        title = "Turbo Nitro Boost (Lv ${career.nitroLevel}/3)",
                        subtitle = "Kecepatan puncak nitro +14% per level.",
                        isMaxed = career.nitroLevel >= 3L,
                        cost = nitroCost,
                        icon = Icons.Filled.Bolt,
                        accent = ViceGold,
                        testTag = "upgrade_nitro_button",
                        modifier = Modifier.weight(1f),
                        onUpgrade = onUpgradeNitro,
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    UpgradeCard(
                        title = "Rompi Anti Peluru (Lv ${career.armorLevel}/3)",
                        subtitle = "+35 Armor per level saat turun ke kota.",
                        isMaxed = career.armorLevel >= 3L,
                        cost = armorCost,
                        icon = Icons.Filled.Shield,
                        accent = ElectricCyan,
                        testTag = "upgrade_armor_button",
                        onUpgrade = onUpgradeArmor,
                    )
                    UpgradeCard(
                        title = "Turbo Nitro Boost (Lv ${career.nitroLevel}/3)",
                        subtitle = "Kecepatan puncak nitro +14% per level.",
                        isMaxed = career.nitroLevel >= 3L,
                        cost = nitroCost,
                        icon = Icons.Filled.Bolt,
                        accent = ViceGold,
                        testTag = "upgrade_nitro_button",
                        onUpgrade = onUpgradeNitro,
                    )
                }
            }
        }
    }
}

@Composable
private fun CareerTab(
    career: CareerProfile,
    recentRunsState: UiState<List<MissionRun>>,
    onUpdateCallsign: (String) -> Unit,
    onResetCareer: () -> Unit,
) {
    var callsignInput by remember(career.callsign) { mutableStateOf(career.callsign) }
    var showResetConfirm by remember { mutableStateOf(false) }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text("Reset Progres Karier?") },
            text = { Text("Semua uang, senjata, kendaraan, dan catatan misi akan dikembalikan ke awal.") },
            confirmButton = {
                Button(
                    onClick = {
                        showResetConfirm = false
                        onResetCareer()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                ) {
                    Text("Reset")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) {
                    Text("Batal")
                }
            },
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Text("PROFIL KARIER", style = MaterialTheme.typography.headlineMedium, color = ElectricCyan)
        }
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = ViceSurface),
                border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.45f)),
                shape = RoundedCornerShape(18.dp),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text("Nama Jalanan (Callsign)", style = MaterialTheme.typography.titleMedium, color = Color.White)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlinedTextField(
                            value = callsignInput,
                            onValueChange = { if (it.length <= 30) callsignInput = it },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                        Button(
                            onClick = { onUpdateCallsign(callsignInput) },
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = ViceVoid),
                            modifier = Modifier
                                .height(52.dp)
                                .testTag("save_callsign_button"),
                        ) {
                            Text("Simpan", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatPill("SKOR TERTINGGI", "${career.highScore}", ViceGold, Modifier.weight(1f))
                StatPill("MOBIL DIBAJAK", "${career.vehiclesStolen}", ElectricCyan, Modifier.weight(1f))
                StatPill("PEAK WANTED", "${career.maxWantedSurvived}★", DangerRed, Modifier.weight(1f))
            }
        }

        item {
            Text("CATATAN KRIMINAL TERBARU", style = MaterialTheme.typography.titleLarge, color = ViceGold)
        }

        when (recentRunsState) {
            is UiState.Loading -> item { CircularProgressIndicator(color = ElectricCyan) }
            is UiState.Error -> item { Text(recentRunsState.message, color = DangerRed) }
            is UiState.Success -> {
                if (recentRunsState.data.isEmpty()) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = ViceSurfaceElevated),
                            shape = RoundedCornerShape(14.dp),
                        ) {
                            Text(
                                text = "Belum ada catatan misi. Turun ke kota dan selesaikan kontrak pertama Anda!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(16.dp),
                            )
                        }
                    }
                } else {
                    items(recentRunsState.data, key = { it.runId }) { run ->
                        val outcome = RunOutcome.fromRaw(run.outcome)
                        val badgeColor = when (outcome) {
                            RunOutcome.PASSED, RunOutcome.HEIST -> CashGreen
                            RunOutcome.WASTED -> DangerRed
                            RunOutcome.BUSTED -> PoliceBlue
                        }
                        Card(
                            colors = CardDefaults.cardColors(containerColor = ViceSurface),
                            border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.45f)),
                            shape = RoundedCornerShape(14.dp),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(run.title, style = MaterialTheme.typography.titleMedium, color = Color.White)
                                    Text(
                                        text = "${outcome.name} • Peak ${run.wantedPeak}★",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = badgeColor,
                                    )
                                }
                                Text(
                                    text = if (run.cashEarned >= 0) "+$${run.cashEarned}" else "-$${-run.cashEarned}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (run.cashEarned >= 0) CashGreen else DangerRed,
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            OutlinedButton(
                onClick = { showResetConfirm = true },
                border = BorderStroke(1.dp, DangerRed),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("reset_career_button"),
            ) {
                Icon(Icons.Filled.RestartAlt, contentDescription = null, tint = DangerRed)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.reset_career), color = DangerRed)
            }
        }
    }
}

@Composable
private fun StatPill(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = ViceSurface),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(14.dp),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleLarge, color = accent)
        }
    }
}

@Composable
private fun ShopCard(
    title: String,
    subtitle: String,
    price: Long,
    owned: Boolean,
    equipped: Boolean,
    accent: Color,
    testTag: String,
    modifier: Modifier = Modifier,
    onAction: () -> Unit,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = ViceSurface),
        border = BorderStroke(1.dp, if (equipped) accent else OutlineSubtle),
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f).padding(end = 10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = Color.White)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Button(
                onClick = onAction,
                enabled = !equipped,
                colors = ButtonDefaults.buttonColors(
                    containerColor = when {
                        equipped -> CashGreen.copy(alpha = 0.2f)
                        owned -> ElectricCyan
                        else -> accent
                    },
                    contentColor = if (equipped) CashGreen else ViceVoid,
                ),
                modifier = Modifier
                    .height(48.dp)
                    .testTag(testTag),
            ) {
                if (equipped) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("DIPAKAI")
                } else if (owned) {
                    Text("PAKAI", fontWeight = FontWeight.Bold)
                } else {
                    Text("$$price", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun UpgradeCard(
    title: String,
    subtitle: String,
    isMaxed: Boolean,
    cost: Long,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: Color,
    testTag: String,
    modifier: Modifier = Modifier,
    onUpgrade: () -> Unit,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = ViceSurface),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier.weight(1f).padding(end = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(icon, contentDescription = title, tint = accent, modifier = Modifier.size(26.dp))
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(title, style = MaterialTheme.typography.titleMedium, color = Color.White)
                    Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Button(
                onClick = onUpgrade,
                enabled = !isMaxed,
                colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = ViceVoid),
                modifier = Modifier
                    .height(48.dp)
                    .testTag(testTag),
            ) {
                Text(if (isMaxed) "MAX" else "$$cost", fontWeight = FontWeight.Bold)
            }
        }
    }
}

private fun tabIcon(tab: HubTab) = when (tab) {
    HubTab.CITY -> Icons.Filled.Explore
    HubTab.MISSIONS -> Icons.Filled.Flag
    HubTab.BLACK_MARKET -> Icons.Filled.ShoppingCart
    HubTab.CAREER -> Icons.Filled.Person
}

@Composable
private fun tabLabel(tab: HubTab): String = when (tab) {
    HubTab.CITY -> stringResource(R.string.nav_play)
    HubTab.MISSIONS -> stringResource(R.string.nav_missions)
    HubTab.BLACK_MARKET -> stringResource(R.string.nav_shop)
    HubTab.CAREER -> stringResource(R.string.nav_profile)
}
