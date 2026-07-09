package com.example.ui

import android.graphics.Paint
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.interpreter.DrawCommand
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    viewModel: ProjectViewModel,
    onBack: () -> Unit
) {
    val project by viewModel.currentProject.collectAsState()
    var selectedTab by remember { mutableStateOf(0) }
    val tabTitles = listOf("Mã nguồn", "Visual", "Mô phỏng", "Kho Asset", "Trợ lý AI")
    val coroutineScope = rememberCoroutineScope()

    if (project == null) return

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = project!!.name,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = WhiteCream,
                            maxLines = 1
                        )
                        Text(
                            text = "Kịch bản Python & Giao diện game",
                            fontSize = 11.sp,
                            color = GrayMuted
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("back_button")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = WhiteCream
                        )
                    }
                },
                actions = {
                    // Quick Action: Run Button
                    IconButton(
                        onClick = {
                            selectedTab = 2 // Switch to Run Tab
                            viewModel.startGame()
                        },
                        modifier = Modifier.testTag("quick_run_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Run Code",
                            tint = Color(0xFF50FA7B)
                        )
                    }

                    // Quick Action: Save Button
                    IconButton(
                        onClick = { viewModel.saveVisualEntities() }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = "Save Project",
                            tint = TealCosmic
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SpaceBlack,
                    titleContentColor = WhiteCream
                )
            )
        },
        containerColor = SpaceBlack,
        modifier = Modifier.navigationBarsPadding()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Custom Tab Row for phone screen
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = HighDensityNav,
                contentColor = TealCosmic,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = TealCosmic
                    )
                },
                modifier = Modifier.testTag("tab_bar")
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                fontSize = 13.sp,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == index) TealCosmic else GrayMuted
                            )
                        },
                        modifier = Modifier.testTag("tab_item_$index")
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(SpaceBlack)
            ) {
                when (selectedTab) {
                    0 -> CodeEditorTab(viewModel)
                    1 -> VisualDesignerTab(viewModel)
                    2 -> SimulationTab(viewModel)
                    3 -> AssetLibraryTab(viewModel)
                    4 -> AiAssistantTab(viewModel)
                }
            }
        }
    }
}

// 1. CODE EDITOR TAB
@Composable
fun CodeEditorTab(viewModel: ProjectViewModel) {
    val code by viewModel.editorCode
    val textScrollState = rememberScrollState()
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(SpaceBlack)
        ) {
            // Line numbers column
            val lines = code.split("\n")
            val linesCount = lines.size.coerceAtLeast(1)
            val lineNumbersText = (1..linesCount).joinToString("\n")

            Column(
                modifier = Modifier
                    .width(42.dp)
                    .fillMaxHeight()
                    .background(GraphiteDeep)
                    .padding(vertical = 12.dp, horizontal = 4.dp),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = lineNumbersText,
                    color = GrayMuted.copy(alpha = 0.5f),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    style = TextStyle(textAlign = androidx.compose.ui.text.style.TextAlign.End)
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .verticalScroll(textScrollState)
                    .padding(vertical = 12.dp, horizontal = 8.dp)
            ) {
                BasicTextField(
                    value = code,
                    onValueChange = { viewModel.updateCurrentCode(it) },
                    visualTransformation = PythonSyntaxHighlighter(),
                    textStyle = TextStyle(
                        color = WhiteCream,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    ),
                    cursorBrush = Brush.verticalGradient(listOf(TealCosmic, TealCosmic)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("code_editor_input")
                )
            }
        }

        // Ergonomic Keyboard Assistant Toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(GraphiteDeep)
                .padding(vertical = 6.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val helpers = listOf("def ", "if ", ":", "=", "draw_rect", "draw_circle", "global ", "+", "on_update")
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
            ) {
                androidx.compose.foundation.lazy.LazyRow(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items(helpers) { item ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(SpaceBlack)
                                .clickable {
                                    val currentText = viewModel.editorCode.value
                                    viewModel.updateCurrentCode(currentText + item)
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = item.trim(),
                                color = TealCosmic,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

// 2. VISUAL DESIGNER TAB
@Composable
fun VisualDesignerTab(viewModel: ProjectViewModel) {
    val entities = viewModel.visualEntities
    val selected by viewModel.selectedEntity
    var showColorPicker by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Drag-and-Drop Canvas frame
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(2.dp, GraphiteDeep, RoundedCornerShape(12.dp))
                .background(SpaceBlack)
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        // Deselect if tap empty space
                        viewModel.selectedEntity.value = null
                    }
                }
                .testTag("designer_canvas")
        ) {
            entities.forEach { entity ->
                // Visual entity draggable box
                Box(
                    modifier = Modifier
                        .offset(x = entity.x.dp / 2f, y = entity.y.dp / 2f) // scale drawing down 50% for design window
                        .size(
                            width = if (entity.type == "rect") (entity.w / 2f).coerceAtLeast(10f).dp else (entity.r).dp,
                            height = if (entity.type == "rect") (entity.h / 2f).coerceAtLeast(10f).dp else (entity.r).dp
                        )
                        .clip(if (entity.type == "circle") CircleShape else RoundedCornerShape(4.dp))
                        .background(
                            parseColor(entity.color).copy(
                                alpha = if (selected?.id == entity.id) 0.9f else 0.5f
                            )
                        )
                        .border(
                            width = if (selected?.id == entity.id) 2.dp else 0.dp,
                            color = TealCosmic,
                            shape = if (entity.type == "circle") CircleShape else RoundedCornerShape(4.dp)
                        )
                        .clickable { viewModel.selectedEntity.value = entity }
                        .pointerInput(entity.id) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                entity.x += dragAmount.x * 2f // Scale coordinate back to standard 400x600 size
                                entity.y += dragAmount.y * 2f
                                entity.x = entity.x.coerceIn(0f, 400f)
                                entity.y = entity.y.coerceIn(0f, 600f)
                                viewModel.saveVisualEntities()
                            }
                        }
                ) {
                    if (entity.type == "text") {
                        Text(
                            text = entity.text.takeIf { it.isNotEmpty() } ?: entity.name,
                            color = Color.White,
                            fontSize = 8.sp,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }
            }

            Text(
                text = "Kéo thả để di chuyển thực thể (Khung thiết kế 400x600)",
                color = GrayMuted.copy(alpha = 0.5f),
                fontSize = 10.sp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(8.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Tool Add Entities Actions Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { viewModel.addVisualEntity("rect") },
                colors = ButtonDefaults.buttonColors(containerColor = GraphiteDeep),
                modifier = Modifier
                    .weight(1f)
                    .testTag("add_rect_btn")
            ) {
                Icon(imageVector = Icons.Default.Square, contentDescription = "Add Rect", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("H.Chữ nhật", fontSize = 11.sp)
            }

            Button(
                onClick = { viewModel.addVisualEntity("circle") },
                colors = ButtonDefaults.buttonColors(containerColor = GraphiteDeep),
                modifier = Modifier
                    .weight(1f)
                    .testTag("add_circle_btn")
            ) {
                Icon(imageVector = Icons.Default.Circle, contentDescription = "Add Circle", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("H.Tròn", fontSize = 11.sp)
            }

            Button(
                onClick = { viewModel.addVisualEntity("text") },
                colors = ButtonDefaults.buttonColors(containerColor = GraphiteDeep),
                modifier = Modifier
                    .weight(1f)
                    .testTag("add_text_btn")
            ) {
                Icon(imageVector = Icons.Default.TextFields, contentDescription = "Add Text", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Nhãn chữ", fontSize = 11.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Entity Properties Panel
        if (selected != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = GraphiteDeep),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .testTag("properties_panel")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Thuộc tính: ${selected!!.name}",
                            fontWeight = FontWeight.Bold,
                            color = TealCosmic,
                            fontSize = 14.sp
                        )

                        IconButton(onClick = { viewModel.deleteVisualEntity(selected!!) }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete entity",
                                tint = Color.Red
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Name edit field
                    var nameText by remember(selected!!.id) { mutableStateOf(selected!!.name) }
                    var colorText by remember(selected!!.id) { mutableStateOf(selected!!.color) }
                    var xVal by remember(selected!!.id) { mutableStateOf(selected!!.x.toString()) }
                    var yVal by remember(selected!!.id) { mutableStateOf(selected!!.y.toString()) }
                    var textVal by remember(selected!!.id) { mutableStateOf(selected!!.text) }

                    OutlinedTextField(
                        value = nameText,
                        onValueChange = {
                            nameText = it
                            viewModel.updateEntityProperties(selected!!, it, colorText, selected!!.x, selected!!.y, selected!!.w, selected!!.h, selected!!.r, textVal)
                        },
                        label = { Text("Tên biến kịch bản") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = xVal,
                            onValueChange = {
                                xVal = it
                                val f = it.toFloatOrNull() ?: selected!!.x
                                viewModel.updateEntityProperties(selected!!, nameText, colorText, f, selected!!.y, selected!!.w, selected!!.h, selected!!.r, textVal)
                            },
                            label = { Text("Tọa độ X") },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )

                        OutlinedTextField(
                            value = yVal,
                            onValueChange = {
                                yVal = it
                                val f = it.toFloatOrNull() ?: selected!!.y
                                viewModel.updateEntityProperties(selected!!, nameText, colorText, selected!!.x, f, selected!!.w, selected!!.h, selected!!.r, textVal)
                            },
                            label = { Text("Tọa độ Y") },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (selected!!.type == "text") {
                        OutlinedTextField(
                            value = textVal,
                            onValueChange = {
                                textVal = it
                                viewModel.updateEntityProperties(selected!!, nameText, colorText, selected!!.x, selected!!.y, text = it)
                            },
                            label = { Text("Nội dung nhãn") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )
                    } else if (selected!!.type == "rect") {
                        var wVal by remember(selected!!.id) { mutableStateOf(selected!!.w.toString()) }
                        var hVal by remember(selected!!.id) { mutableStateOf(selected!!.h.toString()) }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = wVal,
                                onValueChange = {
                                    wVal = it
                                    val f = it.toFloatOrNull() ?: selected!!.w
                                    viewModel.updateEntityProperties(selected!!, nameText, colorText, selected!!.x, selected!!.y, w = f, h = selected!!.h, r = selected!!.r, textVal)
                                },
                                label = { Text("Rộng (W)") },
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )

                            OutlinedTextField(
                                value = hVal,
                                onValueChange = {
                                    hVal = it
                                    val f = it.toFloatOrNull() ?: selected!!.h
                                    viewModel.updateEntityProperties(selected!!, nameText, colorText, selected!!.x, selected!!.y, w = selected!!.w, h = f, r = selected!!.r, textVal)
                                },
                                label = { Text("Cao (H)") },
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )
                        }
                    } else {
                        var rVal by remember(selected!!.id) { mutableStateOf(selected!!.r.toString()) }

                        OutlinedTextField(
                            value = rVal,
                            onValueChange = {
                                rVal = it
                                val f = it.toFloatOrNull() ?: selected!!.r
                                viewModel.updateEntityProperties(selected!!, nameText, colorText, selected!!.x, selected!!.y, w = selected!!.w, h = selected!!.h, r = f, textVal)
                            },
                            label = { Text("Bán kính R") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Color picker buttons Row
                    Text(text = "Chọn màu sắc thực thể", fontSize = 12.sp, color = GrayMuted)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val colorsList = listOf("RED", "GREEN", "BLUE", "YELLOW", "CYAN", "WHITE")
                        colorsList.forEach { colorName ->
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(parseColor(colorName))
                                    .border(
                                        width = if (colorText == colorName) 2.dp else 0.dp,
                                        color = TealCosmic,
                                        shape = CircleShape
                                    )
                                    .clickable {
                                        colorText = colorName
                                        viewModel.updateEntityProperties(
                                            selected!!,
                                            nameText,
                                            colorName,
                                            selected!!.x,
                                            selected!!.y,
                                            selected!!.w,
                                            selected!!.h,
                                            selected!!.r,
                                            textVal
                                        )
                                    }
                            )
                        }
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "Chạm chọn đối tượng để biên tập thuộc tính", color = GrayMuted, fontSize = 12.sp)
            }
        }
    }
}

// 3. SIMULATION / RUN TAB
@Composable
fun SimulationTab(viewModel: ProjectViewModel) {
    val isRunning by viewModel.isRunning
    val errors by viewModel.runtimeError
    val logs = viewModel.consoleLogs
    val logsListState = rememberLazyListState()

    // Autoscroll terminal logs
    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            logsListState.animateScrollToItem(logs.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Canvas Console Engine Frame (400x600 logic, scaled visually to container aspect ratio)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(2.dp, GraphiteDeep, RoundedCornerShape(12.dp))
                .background(Color.Black)
                .pointerInput(isRunning) {
                    detectTapGestures { offset ->
                        // Scale gesture coordinates to match internal game logic window size (400x600)
                        val scaledX = (offset.x / size.width) * 400f
                        val scaledY = (offset.y / size.height) * 600f
                        viewModel.handleCanvasTouch(scaledX, scaledY)
                    }
                }
                .pointerInput(isRunning) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val scaledX = (change.position.x / size.width) * 400f
                        val scaledY = (change.position.y / size.height) * 600f
                        viewModel.handleCanvasTouch(scaledX, scaledY)
                    }
                }
                .testTag("game_simulation_canvas")
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Draw transient commands (game frame)
                viewModel.interpreter.transientDrawings.forEach { cmd ->
                    drawPythonCommand(cmd)
                }
                // Draw persistent commands (drawing frame)
                viewModel.interpreter.persistentDrawings.forEach { cmd ->
                    drawPythonCommand(cmd)
                }
            }

            // Engine controllers overlays
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!isRunning) {
                    IconButton(
                        onClick = { viewModel.startGame() },
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                            .testTag("run_simulation_btn")
                    ) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Run", tint = Color.Green)
                    }
                } else {
                    IconButton(
                        onClick = { viewModel.stopGame() },
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                            .testTag("stop_simulation_btn")
                    ) {
                        Icon(imageVector = Icons.Default.Stop, contentDescription = "Stop", tint = Color.Red)
                    }
                }
            }
        }

        // Error message warning banner
        if (errors != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = AiSuggestionBg),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, AiSuggestionBorder, RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "AI Auto Fix",
                                tint = TealCosmic,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "AI DEBUGGER",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TealCosmic,
                                letterSpacing = 1.sp
                            )
                        }
                        Text(
                            text = "Phát hiện lỗi kịch bản",
                            fontSize = 10.sp,
                            color = GrayMuted
                        )
                    }

                    Text(
                        text = errors!!,
                        color = WhiteCream,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 16.sp
                    )

                    Button(
                        onClick = { viewModel.fixCodeWithAI() },
                        colors = ButtonDefaults.buttonColors(containerColor = TealCosmic, contentColor = SpacePurpleDark),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(vertical = 8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("ai_error_fix_btn")
                    ) {
                        Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = "Fix", modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Áp dụng sửa lỗi tự động", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Terminal Console Log frame
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(GraphiteDeep)
                .padding(12.dp)
        ) {
            Text(
                text = "BẢNG ĐIỀU KHIỂN TERMINAL LOG",
                fontSize = 10.sp,
                color = GrayMuted,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Divider(color = GraphiteDeep)

            Spacer(modifier = Modifier.height(8.dp))

            if (logs.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "Chưa có log đầu ra. Nhấn chạy kịch bản.", color = GrayMuted, fontSize = 11.sp)
                }
            } else {
                LazyColumn(
                    state = logsListState,
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(logs) { logMsg ->
                        Text(
                            text = logMsg,
                            color = if (logMsg.startsWith("CRASH")) Color.Red else Color.White,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }
    }
}

// DrawScope helper extension to render commands
private fun DrawScope.drawPythonCommand(cmd: DrawCommand) {
    when (cmd) {
        is DrawCommand.Rect -> {
            // Scale rendering parameters from internal 400x600 size to actual visual layout frame size
            val drawX = (cmd.x / 400f) * size.width
            val drawY = (cmd.y / 600f) * size.height
            val drawW = (cmd.w / 400f) * size.width
            val drawH = (cmd.h / 600f) * size.height
            drawRect(
                color = parseColor(cmd.color),
                topLeft = Offset(drawX, drawY),
                size = Size(drawW, drawH)
            )
        }
        is DrawCommand.Circle -> {
            val drawX = (cmd.x / 400f) * size.width
            val drawY = (cmd.y / 600f) * size.height
            val drawR = (cmd.r / 400f) * size.width
            drawCircle(
                color = parseColor(cmd.color),
                center = Offset(drawX, drawY),
                radius = drawR
            )
        }
        is DrawCommand.Text -> {
            val drawX = (cmd.x / 400f) * size.width
            val drawY = (cmd.y / 600f) * size.height
            drawContext.canvas.nativeCanvas.drawText(
                cmd.text,
                drawX,
                drawY,
                Paint().apply {
                    color = android.graphics.Color.parseColor(parseHexColor(cmd.color))
                    textSize = cmd.size * (size.width / 400f)
                    isAntiAlias = true
                    typeface = android.graphics.Typeface.MONOSPACE
                }
            )
        }
    }
}

// 4. AI ASSISTANT TAB
@Composable
fun AiAssistantTab(viewModel: ProjectViewModel) {
    val aiResult by viewModel.aiResponse
    val loading by viewModel.aiLoading
    var chatPrompt by remember { mutableStateOf("") }
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // AI Advice response panel
        Card(
            colors = CardDefaults.cardColors(containerColor = GraphiteDeep),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(scrollState)
                .testTag("ai_response_panel")
        ) {
            Box(modifier = Modifier.padding(16.dp)) {
                if (loading) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = TealCosmic)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("AI đang phân tích và sửa mã nguồn...", color = GrayMuted, fontSize = 12.sp)
                    }
                } else if (aiResult.isEmpty()) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "AI",
                            tint = TealCosmic,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Trợ lý Trí tuệ Nhân tạo AI Copilot",
                            color = WhiteCream,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Hãy nhập yêu cầu của bạn bên dưới (ví dụ: 'Thêm chuyển động nhảy', 'Tạo game tránh thiên thạch') hoặc nhấp nút 'Sửa lỗi AI' để tự động sửa lỗi code hiện tại.",
                            color = GrayMuted,
                            fontSize = 12.sp,
                            style = TextStyle(textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        )
                    }
                } else {
                    Text(
                        text = aiResult,
                        color = WhiteCream,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        // Fast Action AI Fix Button
        Button(
            onClick = { viewModel.fixCodeWithAI() },
            colors = ButtonDefaults.buttonColors(containerColor = PurpleAmethyst, contentColor = SpaceBlack),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("ai_fix_code_now_btn")
        ) {
            Icon(imageVector = Icons.Default.Build, contentDescription = "Fix", modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Sửa lỗi mã nguồn tự động với AI", fontWeight = FontWeight.Bold)
        }

        // Interactive Prompt input row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = chatPrompt,
                onValueChange = { chatPrompt = it },
                placeholder = { Text("Yêu cầu AI viết game hoặc sửa tính năng...") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = TealCosmic,
                    unfocusedBorderColor = GraphiteDeep
                ),
                maxLines = 2,
                modifier = Modifier
                    .weight(1f)
                    .testTag("ai_chat_prompt_input")
            )

            IconButton(
                onClick = {
                    if (chatPrompt.trim().isNotEmpty()) {
                        viewModel.askAI(chatPrompt)
                        chatPrompt = ""
                    }
                },
                modifier = Modifier
                    .size(48.dp)
                    .background(TealCosmic, RoundedCornerShape(12.dp))
                    .testTag("ai_send_prompt_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Send",
                    tint = SpaceBlack
                )
            }
        }
    }
}

// Helper methods for Color and Hex decoding
fun parseColor(colorName: String): Color {
    return when (colorName.uppercase()) {
        "RED" -> Color(0xFFFF5555)
        "GREEN" -> Color(0xFF50FA7B)
        "BLUE" -> Color(0xFF8BE9FD)
        "YELLOW" -> Color(0xFFF1FA8C)
        "CYAN" -> Color(0xFF8BE9FD)
        "MAGENTA" -> Color(0xFFFF79C6)
        "WHITE" -> Color(0xFFF8F8F2)
        "GRAY" -> Color(0xFF6272A4)
        "BLACK" -> Color(0xFF282A36)
        else -> {
            if (colorName.startsWith("#")) {
                try {
                    Color(android.graphics.Color.parseColor(colorName))
                } catch (e: Exception) {
                    Color.White
                }
            } else {
                Color.White
            }
        }
    }
}

fun parseHexColor(colorName: String): String {
    return when (colorName.uppercase()) {
        "RED" -> "#FF5555"
        "GREEN" -> "#50FA7B"
        "BLUE" -> "#8BE9FD"
        "YELLOW" -> "#F1FA8C"
        "CYAN" -> "#8BE9FD"
        "MAGENTA" -> "#FFFF79C6"
        "WHITE" -> "#F8F8F2"
        "GRAY" -> "#6272A4"
        "BLACK" -> "#282A36"
        else -> {
            if (colorName.startsWith("#")) {
                colorName
            } else {
                "#FFFFFF"
            }
        }
    }
}
