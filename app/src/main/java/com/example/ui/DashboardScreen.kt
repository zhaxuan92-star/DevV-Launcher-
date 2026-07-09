package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Project
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DashboardScreen(
    viewModel: ProjectViewModel,
    onProjectSelected: (Project) -> Unit
) {
    val projects by viewModel.allProjects.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }
    var newProjName by remember { mutableStateOf("") }
    var newProjDesc by remember { mutableStateOf("") }
    var isGameMode by remember { mutableStateOf(true) }
    var projectToDelete by remember { mutableStateOf<Project?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SpaceBlack)
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Hero Brand Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "PyDev Studio",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = TealCosmic,
                        fontFamily = FontFamily.SansSerif,
                        modifier = Modifier.testTag("app_title")
                    )
                    Text(
                        text = "IDE Lập Trình Game & AI Di Động",
                        fontSize = 14.sp,
                        color = GrayMuted,
                        fontWeight = FontWeight.Medium
                    )
                }

                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(GraphiteDeep)
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Code,
                        contentDescription = "Code logo",
                        tint = TealCosmic
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Statistics Bar / Quick info
            Card(
                colors = CardDefaults.cardColors(containerColor = GraphiteDeep),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = projects.size.toString(),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = TealCosmic
                        )
                        Text(text = "Dự án", fontSize = 12.sp, color = GrayMuted)
                    }
                    Divider(
                        color = SpaceBlack,
                        modifier = Modifier
                            .height(35.dp)
                            .width(1.dp)
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Python",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = PurpleAmethyst
                        )
                        Text(text = "Ngôn ngữ", fontSize = 12.sp, color = GrayMuted)
                    }
                    Divider(
                        color = SpaceBlack,
                        modifier = Modifier
                            .height(35.dp)
                            .width(1.dp)
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Offline",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF50FA7B)
                        )
                        Text(text = "Hoạt động", fontSize = 12.sp, color = GrayMuted)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "DỰ ÁN CỦA TÔI",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = GrayMuted,
                letterSpacing = 1.5.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (projects.isEmpty()) {
                // Empty state
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = "No project",
                            tint = GrayMuted,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Chưa có dự án nào được tạo",
                            color = WhiteCream,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Nhấp vào dấu + bên dưới để tạo dự án game đầu tiên",
                            color = GrayMuted,
                            fontSize = 12.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(projects, key = { it.id }) { project ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = GraphiteDeep),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onClick = { onProjectSelected(project) },
                                    onLongClick = { projectToDelete = project }
                                )
                                .testTag("project_card_${project.id}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (project.pythonCode.contains("on_update")) Color(0xFF00E5FF).copy(alpha = 0.15f)
                                            else Color(0xFFBD93F9).copy(alpha = 0.15f)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (project.pythonCode.contains("on_update")) Icons.Default.VideogameAsset else Icons.Default.Terminal,
                                        contentDescription = "Project icon",
                                        tint = if (project.pythonCode.contains("on_update")) TealCosmic else PurpleAmethyst
                                    )
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = project.name,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = WhiteCream,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = project.description,
                                        fontSize = 12.sp,
                                        color = GrayMuted,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Cập nhật: " + SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(project.lastModified)),
                                        fontSize = 10.sp,
                                        color = GrayMuted.copy(alpha = 0.7f)
                                    )
                                }

                                IconButton(onClick = { projectToDelete = project }) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete project",
                                        tint = Color.Red.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // FAB to add new project
        FloatingActionButton(
            onClick = { showCreateDialog = true },
            containerColor = TealCosmic,
            contentColor = SpaceBlack,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .testTag("create_project_fab")
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "Create new")
        }

        // Dialog for creation
        if (showCreateDialog) {
            AlertDialog(
                onDismissRequest = { showCreateDialog = false },
                containerColor = GraphiteDeep,
                title = {
                    Text(text = "Dự án mới", color = WhiteCream, fontWeight = FontWeight.Bold)
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = newProjName,
                            onValueChange = { newProjName = it },
                            label = { Text("Tên dự án") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = WhiteCream,
                                unfocusedTextColor = WhiteCream,
                                focusedLabelColor = TealCosmic,
                                unfocusedLabelColor = GrayMuted,
                                focusedBorderColor = TealCosmic,
                                unfocusedBorderColor = SpaceBlack
                            ),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("new_project_name_input")
                        )

                        OutlinedTextField(
                            value = newProjDesc,
                            onValueChange = { newProjDesc = it },
                            label = { Text("Mô tả ngắn") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = WhiteCream,
                                unfocusedTextColor = WhiteCream,
                                focusedLabelColor = TealCosmic,
                                unfocusedLabelColor = GrayMuted,
                                focusedBorderColor = TealCosmic,
                                unfocusedBorderColor = SpaceBlack
                            ),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("new_project_desc_input")
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Chế độ game / Bản vẽ thiết kế", color = WhiteCream, fontSize = 14.sp)
                            Switch(
                                checked = isGameMode,
                                onCheckedChange = { isGameMode = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = SpaceBlack,
                                    checkedTrackColor = TealCosmic
                                )
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (newProjName.trim().isNotEmpty()) {
                                viewModel.createNewProject(newProjName, newProjDesc, isGameMode)
                                newProjName = ""
                                newProjDesc = ""
                                isGameMode = true
                                showCreateDialog = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = TealCosmic, contentColor = SpaceBlack),
                        modifier = Modifier.testTag("submit_project_button")
                    ) {
                        Text("Tạo dự án")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCreateDialog = false }) {
                        Text("Hủy", color = GrayMuted)
                    }
                }
            )
        }

        // Dialog for deletion
        if (projectToDelete != null) {
            AlertDialog(
                onDismissRequest = { projectToDelete = null },
                containerColor = GraphiteDeep,
                title = {
                    Text(text = "Xóa dự án?", color = WhiteCream, fontWeight = FontWeight.Bold)
                },
                text = {
                    Text(
                        text = "Bạn có chắc chắn muốn xóa dự án '${projectToDelete?.name}'? Thao tác này không thể hoàn tác.",
                        color = GrayMuted,
                        fontSize = 14.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            projectToDelete?.let { viewModel.deleteProject(it) }
                            projectToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                        modifier = Modifier.testTag("confirm_delete_button")
                    ) {
                        Text("Xóa", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { projectToDelete = null }) {
                        Text("Bỏ qua", color = GrayMuted)
                    }
                }
            )
        }
    }
}
