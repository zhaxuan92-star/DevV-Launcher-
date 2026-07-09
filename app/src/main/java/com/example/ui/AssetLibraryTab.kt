package com.example.ui

import android.widget.Toast
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

// Asset item model
data class AssetItem(
    val id: String,
    val name: String,
    val category: String, // "sprite" | "background" | "sound" | "ui"
    val description: String,
    val codeSnippet: String,
    val sfxTrigger: String? = null,
    val renderer: @Composable (Modifier) -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssetLibraryTab(viewModel: ProjectViewModel) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var selectedCategory by remember { mutableStateOf("all") }
    var searchQuery by remember { mutableStateOf("") }
    var selectedAsset by remember { mutableStateOf<AssetItem?>(null) }

    // Hardcoded built-in game assets list
    val assets = remember {
        listOf(
            // --- SPRITES ---
            AssetItem(
                id = "sprite_rocket",
                name = "Tên Lửa Vũ Trụ (Space Rocket)",
                category = "sprite",
                description = "Vẽ phi thuyền tên lửa vũ trụ hoạt động với động cơ đẩy lửa phía đuôi, lý tưởng cho game bắn máy bay.",
                codeSnippet = """# --- IMPORTED ASSET: Tên Lửa Vũ Trụ ---
# Biến vị trí của phi thuyền
rocket_x = 180
rocket_y = 300
rocket_w = 20
rocket_h = 40

def draw_rocket(x, y):
    # Đuôi lửa động cơ
    draw_rect(x + 5, y + 40, 10, 8, "YELLOW")
    # Cánh phụ hai bên
    draw_rect(x - 5, y + 30, 5, 15, "CYAN")
    draw_rect(x + 20, y + 30, 5, 15, "CYAN")
    # Thân chính tên lửa
    draw_rect(x, y, 20, 40, "WHITE")
    # Mũi tên lửa màu đỏ
    draw_circle(x + 10, y, 10, "RED")
""",
                renderer = { modifier ->
                    Canvas(modifier = modifier) {
                        val w = size.width
                        val h = size.height
                        // Booster fire
                        drawRect(Color(0xFFFFC107), Offset(w * 0.4f, h * 0.8f), Size(w * 0.2f, h * 0.12f))
                        // Rocket body
                        drawRoundRect(Color.White, Offset(w * 0.35f, h * 0.32f), Size(w * 0.3f, h * 0.48f), CornerRadius(6f, 6f))
                        // Red nose cone
                        drawArc(Color(0xFFF2B8B5), -180f, 180f, true, Offset(w * 0.35f, h * 0.17f), Size(w * 0.3f, h * 0.3f))
                        // Cyan side fins
                        drawRect(Color(0xFF80DEEA), Offset(w * 0.23f, h * 0.65f), Size(w * 0.12f, h * 0.15f))
                        drawRect(Color(0xFF80DEEA), Offset(w * 0.65f, h * 0.65f), Size(w * 0.12f, h * 0.15f))
                        // Window
                        drawCircle(Color(0xFF381E72), w * 0.08f, Offset(w * 0.5f, h * 0.52f))
                    }
                }
            ),
            AssetItem(
                id = "sprite_alien",
                name = "Kẻ Xâm Lược (Alien Invader)",
                category = "sprite",
                description = "Kẻ thù ngoài hành tinh cổ điển với râu, mắt phát sáng và ba xúc tu nhấp nhô nghịch ngợm.",
                codeSnippet = """# --- IMPORTED ASSET: Kẻ Xâm Lược ---
alien_x = 100
alien_y = 150
alien_speed = 2

def draw_alien(x, y):
    # Vẽ râu ăng-ten trái và phải
    draw_rect(x + 8, y - 10, 4, 10, "GREEN")
    draw_rect(x + 28, y - 10, 4, 10, "GREEN")
    draw_circle(x + 10, y - 10, 3, "GREEN")
    draw_circle(x + 30, y - 10, 3, "GREEN")
    # Thân chính quái vật
    draw_rect(x, y, 40, 24, "GREEN")
    # Mắt phát sáng màu trắng tinh
    draw_circle(x + 12, y + 8, 4, "WHITE")
    draw_circle(x + 28, y + 8, 4, "WHITE")
    # Xúc tu di động dưới thân
    draw_rect(x + 4, y + 24, 6, 8, "GREEN")
    draw_rect(x + 17, y + 24, 6, 8, "GREEN")
    draw_rect(x + 30, y + 24, 6, 8, "GREEN")
""",
                renderer = { modifier ->
                    Canvas(modifier = modifier) {
                        val w = size.width
                        val h = size.height
                        // Main body
                        drawRoundRect(Color(0xFF7DDA58), Offset(w * 0.22f, h * 0.32f), Size(w * 0.56f, h * 0.38f), CornerRadius(12f, 12f))
                        // Eyes
                        drawCircle(Color.White, w * 0.06f, Offset(w * 0.38f, h * 0.48f))
                        drawCircle(Color.Black, w * 0.025f, Offset(w * 0.38f, h * 0.48f))
                        drawCircle(Color.White, w * 0.06f, Offset(w * 0.62f, h * 0.48f))
                        drawCircle(Color.Black, w * 0.025f, Offset(w * 0.62f, h * 0.48f))
                        // Antennae
                        drawLine(Color(0xFF7DDA58), Offset(w * 0.32f, h * 0.32f), Offset(w * 0.25f, h * 0.18f), strokeWidth = 5f)
                        drawCircle(Color(0xFF7DDA58), w * 0.04f, Offset(w * 0.25f, h * 0.18f))
                        drawLine(Color(0xFF7DDA58), Offset(w * 0.68f, h * 0.32f), Offset(w * 0.75f, h * 0.18f), strokeWidth = 5f)
                        drawCircle(Color(0xFF7DDA58), w * 0.04f, Offset(w * 0.75f, h * 0.18f))
                        // Tentacles
                        drawRect(Color(0xFF7DDA58), Offset(w * 0.28f, h * 0.7f), Size(w * 0.09f, h * 0.12f))
                        drawRect(Color(0xFF7DDA58), Offset(w * 0.45f, h * 0.7f), Size(w * 0.09f, h * 0.12f))
                        drawRect(Color(0xFF7DDA58), Offset(w * 0.62f, h * 0.7f), Size(w * 0.09f, h * 0.12f))
                    }
                }
            ),
            AssetItem(
                id = "sprite_coin",
                name = "Đồng Xu Vàng (Golden Coin)",
                category = "sprite",
                description = "Đồng tiền vàng bóng loáng phản chiếu ánh sáng trắng, hoàn hảo làm vật phẩm thu thập.",
                codeSnippet = """# --- IMPORTED ASSET: Đồng Xu Vàng ---
coin_x = 200
coin_y = 150
coin_active = True

def draw_coin(x, y):
    # Thân đồng xu vàng óng
    draw_circle(x, y, 15, "YELLOW")
    # Viền trong lấp lánh phản chiếu
    draw_circle(x, y, 10, "CYAN")
    # Lõi đồng xu màu cam đậm bóng
    draw_circle(x, y, 5, "RED")
""",
                renderer = { modifier ->
                    Canvas(modifier = modifier) {
                        val w = size.width
                        val h = size.height
                        val center = Offset(w / 2, h / 2)
                        // Outer gold rim
                        drawCircle(Color(0xFFFFD700), w * 0.36f, center)
                        // Inner circle shiny yellow
                        drawCircle(Color(0xFFFFF200), w * 0.28f, center)
                        // Shiny accent
                        drawCircle(Color(0xFFFFAB00), w * 0.16f, center)
                        drawCircle(Color.White, w * 0.06f, Offset(w * 0.42f, h * 0.42f))
                    }
                }
            ),
            AssetItem(
                id = "sprite_fireball",
                name = "Quả Cầu Lửa (Fireball Spell)",
                category = "sprite",
                description = "Chiêu thức ma pháp lửa rực rỡ với ba tầng nhiệt độ tăng dần từ đỏ, cam sang nhân trắng siêu nóng.",
                codeSnippet = """# --- IMPORTED ASSET: Quả Cầu Lửa ---
fireball_x = 50
fireball_y = 200
fire_speed = 6

def draw_fireball(x, y):
    # Lớp vỏ lửa đỏ rực rỡ bên ngoài
    draw_circle(x, y, 18, "RED")
    # Lớp lõi cam nóng bỏng ở giữa
    draw_circle(x - 3, y, 11, "YELLOW")
    # Tâm cầu lửa trắng siêu nhiệt
    draw_circle(x + 3, y, 5, "WHITE")
""",
                renderer = { modifier ->
                    Canvas(modifier = modifier) {
                        val w = size.width
                        val h = size.height
                        val center = Offset(w / 2, h / 2)
                        // Outer flame layer (red)
                        drawCircle(Color(0xFFE53935), w * 0.4f, center)
                        // Middle intense heat (orange-yellow)
                        drawCircle(Color(0xFFFFB300), w * 0.28f, Offset(w * 0.45f, h * 0.5f))
                        // Core plasma (white)
                        drawCircle(Color.White, w * 0.14f, Offset(w * 0.52f, h * 0.5f))
                    }
                }
            ),

            // --- BACKGROUNDS ---
            AssetItem(
                id = "bg_space",
                name = "Vũ Trụ Đầy Sao (Starry Nebula)",
                category = "background",
                description = "Ảnh nền không gian sâu thẳm đen huyền bí kết hợp các làn tinh vân tím/lam mờ ảo và các chòm sao lấp lánh.",
                codeSnippet = """# --- IMPORTED ASSET: Vũ Trụ Đầy Sao ---
def draw_background_space():
    # Phủ toàn bộ màn hình bằng màu đen huyền bí (kích thước chuẩn 400x600)
    draw_rect(0, 0, 400, 600, "BLACK")
    # Vẽ các chòm sao nhỏ lấp lánh khắp không gian
    draw_circle(45, 80, 2, "WHITE")
    draw_circle(120, 240, 3, "YELLOW")
    draw_circle(310, 150, 1, "WHITE")
    draw_circle(260, 450, 2, "CYAN")
    draw_circle(85, 520, 3, "WHITE")
    draw_circle(180, 380, 2, "YELLOW")
    draw_circle(340, 500, 2, "WHITE")
""",
                renderer = { modifier ->
                    Box(modifier = modifier.background(Color.Black)) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height
                            // Purple and Teal Nebula clouds
                            drawCircle(Color(0x384F378B), w * 0.35f, Offset(w * 0.3f, h * 0.35f))
                            drawCircle(Color(0x2400E5FF), w * 0.42f, Offset(w * 0.72f, h * 0.65f))
                            // Stars
                            drawCircle(Color.White, 3f, Offset(w * 0.18f, h * 0.18f))
                            drawCircle(Color(0xFFFFEB3B), 2.5f, Offset(w * 0.82f, h * 0.28f))
                            drawCircle(Color.White, 4f, Offset(w * 0.48f, h * 0.76f))
                            drawCircle(Color(0xFF80DEEA), 3f, Offset(w * 0.14f, h * 0.58f))
                            drawCircle(Color.White, 2f, Offset(w * 0.88f, h * 0.82f))
                        }
                    }
                }
            ),
            AssetItem(
                id = "bg_cyberpunk",
                name = "Cyberpunk Sunset Grid",
                category = "background",
                description = "Thiết kế hoàng hôn neon 80s huyền ảo với vệt nắng khổng lồ màu đỏ tươi, mây khói thành phố và lưới phối cảnh cyberpunk.",
                codeSnippet = """# --- IMPORTED ASSET: Cyberpunk Sunset ---
def draw_background_cyberpunk():
    # Bầu trời đêm đỏ tía mờ ảo
    draw_rect(0, 0, 400, 320, "MAGENTA")
    # Ông mặt trời neon khổng lồ
    draw_circle(200, 320, 90, "YELLOW")
    # Nền móng đô thị bóng đêm phủ đen
    draw_rect(0, 320, 400, 280, "BLACK")
    # Dãy cao ốc chọc trời sừng sững
    draw_rect(40, 260, 45, 60, "GRAY")
    draw_rect(110, 200, 50, 120, "GRAY")
    draw_rect(270, 240, 60, 80, "GRAY")
""",
                renderer = { modifier ->
                    Box(modifier = modifier.background(Color(0xFF150824))) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height
                            // Giant Synthwave sun
                            drawCircle(Color(0xFFFF007F), w * 0.38f, Offset(w * 0.5f, h * 0.58f))
                            // Horizontal split scanlines for sun
                            for (yLine in (h * 0.25f).toInt()..(h * 0.58f).toInt() step 10) {
                                drawLine(Color(0xFF150824), Offset(0f, yLine.toFloat()), Offset(w, yLine.toFloat()), strokeWidth = 3.5f)
                            }
                            // Ground base
                            drawRect(Color(0xFF0A0214), Offset(0f, h * 0.62f), Size(w, h * 0.38f))
                            // Perspective grids
                            val yGround = h * 0.62f
                            for (stepX in 0..8) {
                                val targetX = w * (stepX / 8f)
                                drawLine(Color(0xFF00E5FF), Offset(w * 0.5f, yGround), Offset(targetX, h), strokeWidth = 1.8f)
                            }
                        }
                    }
                }
            ),
            AssetItem(
                id = "bg_meadow",
                name = "Đồng Cỏ Thanh Bình (Grass Meadow)",
                category = "background",
                description = "Phong cảnh thanh bình lý tưởng cho game vượt ải, chạy vô tận với nền trời xanh thẳm, mặt trời vàng rực và đồi núi xanh mướt.",
                codeSnippet = """# --- IMPORTED ASSET: Đồng Cỏ Thanh Bình ---
def draw_background_meadow():
    # Bầu trời xanh lơ dịu mát
    draw_rect(0, 0, 400, 450, "CYAN")
    # Mặt trời tỏa nắng ấm áp góc trên
    draw_circle(330, 80, 35, "YELLOW")
    # Ngọn núi hùng vĩ đằng xa mờ
    # (Đồng cỏ nền cỏ xanh rì mướt)
    draw_rect(0, 450, 400, 150, "GREEN")
""",
                renderer = { modifier ->
                    Box(modifier = modifier.background(Color(0xFFB3E5FC))) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height
                            // Sun
                            drawCircle(Color(0xFFFFEE58), w * 0.14f, Offset(w * 0.82f, h * 0.25f))
                            // Mountains (path)
                            val path = Path().apply {
                                moveTo(0f, h * 0.7f)
                                lineTo(w * 0.32f, h * 0.44f)
                                lineTo(w * 0.65f, h * 0.7f)
                                close()
                            }
                            drawPath(path, Color(0xFF81C784))
                            val path2 = Path().apply {
                                moveTo(w * 0.4f, h * 0.7f)
                                lineTo(w * 0.75f, h * 0.48f)
                                lineTo(w, h * 0.7f)
                                close()
                            }
                            drawPath(path2, Color(0xFF66BB6A))
                            // Green ground
                            drawRect(Color(0xFF4CAF50), Offset(0f, h * 0.68f), Size(w, h * 0.32f))
                        }
                    }
                }
            ),

            // --- SOUNDS ---
            AssetItem(
                id = "sfx_laser",
                name = "Bắn Pháo Laser (Laser Shoot)",
                category = "sound",
                description = "Tạo hiệu ứng âm thanh rít cao vút của đạn pháo plasma laser dồn dập.",
                codeSnippet = """# Kích hoạt âm thanh bắn tia laser nhanh
play_sound("laser")
""",
                sfxTrigger = "laser",
                renderer = { modifier ->
                    SoundWaveCard(modifier = modifier, color = Color(0xFF00E5FF))
                }
            ),
            AssetItem(
                id = "sfx_coin",
                name = "Ăn Tiền Vàng (Coin Collect)",
                category = "sound",
                description = "Âm thanh lảnh lót leng keng cực vui khi nhân vật thu thập tài sản, vật phẩm hoặc thăng cấp.",
                codeSnippet = """# Kích hoạt âm thanh leng keng của đồng xu vàng
play_sound("coin")
""",
                sfxTrigger = "coin",
                renderer = { modifier ->
                    SoundWaveCard(modifier = modifier, color = Color(0xFFFFD700))
                }
            ),
            AssetItem(
                id = "sfx_explode",
                name = "Vụ Nổ Đổ Vỡ (Explosion Sfx)",
                category = "sound",
                description = "Hiệu ứng nổ rầm vang trầm đục cực mạnh khi hạ gục kẻ thù hoặc chịu sát thương nguy kịch.",
                codeSnippet = """# Kích hoạt âm thanh nổ rầm rung động vật lý
play_sound("explode")
""",
                sfxTrigger = "explode",
                renderer = { modifier ->
                    SoundWaveCard(modifier = modifier, color = Color(0xFFFF5722))
                }
            ),
            AssetItem(
                id = "sfx_jump",
                name = "Nhảy Bứt Phá (High Jump / Jetpack)",
                category = "sound",
                description = "Âm thanh tưng bừng vút lên của lò xo nhảy cao hoặc phản lực đẩy nhân vật bay vút lên bầu trời.",
                codeSnippet = """# Kích hoạt âm thanh bứt tốc nhảy cao bật nảy
play_sound("jump")
""",
                sfxTrigger = "jump",
                renderer = { modifier ->
                    SoundWaveCard(modifier = modifier, color = Color(0xFFEADDFF))
                }
            ),
            AssetItem(
                id = "sfx_game_over",
                name = "Thất Bại (Game Over)",
                category = "sound",
                description = "Âm trầm báo động ngân vang nặng nề đánh dấu sự kết thúc của màn chơi thất bại dở dang.",
                codeSnippet = """# Kích hoạt âm thanh báo động game kết thúc
play_sound("game_over")
""",
                sfxTrigger = "game_over",
                renderer = { modifier ->
                    SoundWaveCard(modifier = modifier, color = Color(0xFFF2B8B5))
                }
            ),

            // --- UI ELEMENTS ---
            AssetItem(
                id = "ui_score",
                name = "Khung Điểm Số (Score HUD)",
                category = "ui",
                description = "Hộp chứa thông tin hiển thị điểm số nằm tinh gọn ở góc trái màn hình, thích hợp hiển thị liên tục.",
                codeSnippet = """# --- IMPORTED ASSET: Khung Điểm Số ---
score = 0

def draw_score_hud():
    global score
    # Vẽ hộp nền màu xám đậm bo mờ
    draw_rect(10, 10, 130, 40, "GRAY")
    # Vẽ chữ điểm số vàng lấp lánh phản chiếu
    draw_text("ĐIỂM SỐ: " + str(score), 20, 35, 14, "YELLOW")
""",
                renderer = { modifier ->
                    Box(modifier = modifier.background(Color(0xFF211F24)), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("ĐIỂM SỐ", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = GrayMuted)
                            Text("002,450", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFFFF200))
                        }
                    }
                }
            ),
            AssetItem(
                id = "ui_gameover",
                name = "Màn Hình Game Over (GameOver Panel)",
                category = "ui",
                description = "Bảng thông báo thua cuộc toàn màn hình đỏ rực sầm uất kèm lời nhắc chạm màn hình chơi lại.",
                codeSnippet = """# --- IMPORTED ASSET: Màn Hình Game Over ---
def draw_game_over_screen():
    # Hộp thông báo màu đen mờ trung tâm
    draw_rect(40, 180, 320, 200, "BLACK")
    # Dòng chữ chính THẤT BẠI đỏ rực nguy hiểm
    draw_text("GAME OVER", 110, 250, 26, "RED")
    # Chỉ dẫn chạm để hồi sinh tái sinh
    draw_text("Chạm màn hình để CHƠI LẠI!", 80, 320, 13, "YELLOW")
""",
                renderer = { modifier ->
                    Box(modifier = modifier.background(Color(0xDD120202)), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.Dangerous, contentDescription = null, tint = Color(0xFFE53935), modifier = Modifier.size(20.dp))
                            Text("GAME OVER", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFE53935))
                            Text("Chạm để chơi lại", fontSize = 9.sp, color = Color(0xFFFFD54F))
                        }
                    }
                }
            ),
            AssetItem(
                id = "ui_start_btn",
                name = "Nút Khởi Động (Play Button)",
                category = "ui",
                description = "Nút bấm 'CHƠI NGAY' xanh mướt kích thích năng lượng bắt đầu trận chiến kịch tính dồn dập.",
                codeSnippet = """# --- IMPORTED ASSET: Nút Chơi Ngay ---
def draw_play_button():
    # Hộp nút màu xanh lá rực rỡ góc chính diện
    draw_rect(120, 260, 160, 56, "GREEN")
    # Nhãn dán nút bắt đầu cực súc tích
    draw_text("CHƠI NGAY", 155, 296, 15, "BLACK")
""",
                renderer = { modifier ->
                    Box(modifier = modifier.background(Color(0xFF2E7D32)), contentAlignment = Alignment.Center) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                            Text("CHƠI NGAY", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            ),
            AssetItem(
                id = "ui_dpad",
                name = "Tay Cầm Ảo (Virtual Touch D-Pad)",
                category = "ui",
                description = "Trình điều khiển phím ảo tròn tinh xảo giúp giả lập tay cầm cơ học hỗ trợ chạm kéo linh động.",
                codeSnippet = """# --- IMPORTED ASSET: Tay Cầm Di Chuyển Ảo ---
dpad_cx = 200
dpad_cy = 480
dpad_r = 45

def draw_virtual_dpad():
    # Đế tròn dpad xám bóng loáng
    draw_circle(dpad_cx, dpad_cy, dpad_r, "GRAY")
    # Núm trung tâm giữ ngón tay điều động màu trắng tinh
    draw_circle(dpad_cx, dpad_cy, 18, "WHITE")
""",
                renderer = { modifier ->
                    Canvas(modifier = modifier) {
                        val w = size.width
                        val h = size.height
                        val center = Offset(w / 2, h / 2)
                        // Outer DPad base
                        drawCircle(Color(0xFF424242), w * 0.4f, center)
                        // Arrow accents
                        drawRect(Color(0xFF212121), Offset(w * 0.38f, h * 0.18f), Size(w * 0.24f, h * 0.64f))
                        drawRect(Color(0xFF212121), Offset(w * 0.18f, h * 0.38f), Size(w * 0.64f, h * 0.24f))
                        // Center thumbstick cap
                        drawCircle(Color.LightGray, w * 0.18f, center)
                    }
                }
            )
        )
    }

    // Filter items based on category and search query
    val filteredAssets = remember(selectedCategory, searchQuery, assets) {
        assets.filter { item ->
            val matchesCategory = selectedCategory == "all" || item.category == selectedCategory
            val matchesSearch = item.name.contains(searchQuery, ignoreCase = true) || 
                                item.description.contains(searchQuery, ignoreCase = true)
            matchesCategory && matchesSearch
        }
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(SpaceBlack)
    ) {
        // Left Column: Asset selection pane
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .weight(1.1f)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Tìm assets nhanh...", fontSize = 12.sp, color = GrayMuted) },
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = GrayMuted, modifier = Modifier.size(16.dp)) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(16.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", tint = GrayMuted, modifier = Modifier.size(14.dp))
                        }
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = WhiteCream,
                    unfocusedTextColor = WhiteCream,
                    focusedContainerColor = GraphiteDeep,
                    unfocusedContainerColor = GraphiteDeep,
                    focusedBorderColor = TealCosmic,
                    unfocusedBorderColor = BorderMuted
                ),
                textStyle = LocalTextStyle.current.copy(fontSize = 12.sp),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
            )

            // Horizontal scrolling Category Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val categoryMap = listOf(
                    "all" to "Tất cả",
                    "sprite" to "Sprite",
                    "background" to "Bối cảnh",
                    "sound" to "Âm thanh",
                    "ui" to "Giao diện"
                )

                categoryMap.forEach { (catId, catName) ->
                    val isSelected = selectedCategory == catId
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSelected) TealCosmic else GraphiteDeep)
                            .border(1.dp, if (isSelected) TealCosmic else BorderMuted, RoundedCornerShape(6.dp))
                            .clickable { selectedCategory = catId }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = catName,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) SpacePurpleDark else WhiteCream
                        )
                    }
                }
            }

            // Asset Grid List
            if (filteredAssets.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Category, contentDescription = null, tint = GrayMuted, modifier = Modifier.size(36.dp))
                        Text(
                            text = "Không tìm thấy Asset phù hợp",
                            fontSize = 12.sp,
                            color = GrayMuted
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 8.dp)
                ) {
                    items(filteredAssets) { asset ->
                        val isSelected = selectedAsset?.id == asset.id
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) AiSuggestionBg else GraphiteDeep
                            ),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) TealCosmic else BorderMuted
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedAsset = asset
                                    asset.sfxTrigger?.let { viewModel.playPreviewSound(it) }
                                }
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                // Live customized Compose visual preview
                                asset.renderer(
                                    Modifier
                                        .size(72.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(SpaceBlack)
                                )

                                Text(
                                    text = asset.name.substringBefore(" ("),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = WhiteCream,
                                    maxLines = 1,
                                    modifier = Modifier.align(Alignment.CenterHorizontally)
                                )

                                // Category Indicator Chip
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(SpaceBlack)
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    val (icon, tint) = when (asset.category) {
                                        "sprite" -> Icons.Default.SmartToy to Color(0xFF7DDA58)
                                        "background" -> Icons.Default.Landscape to Color(0xFF80DEEA)
                                        "sound" -> Icons.Default.VolumeUp to Color(0xFFFFB300)
                                        else -> Icons.Default.Widgets to Color(0xFFEADDFF)
                                    }
                                    Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(10.dp))
                                    Text(
                                        text = asset.category.uppercase(),
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = tint
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Thin Vertical Divider between columns
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(1.dp)
                .background(BorderMuted)
        )

        // Right Column: Active Asset Inspect Detail Panel
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .weight(0.9f)
                .background(SpaceBlack)
                .padding(14.dp)
        ) {
            val asset = selectedAsset
            if (asset == null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Select asset",
                            tint = TealCosmic,
                            modifier = Modifier.size(42.dp)
                        )
                        Text(
                            text = "CHỌN MỘT ASSET\nĐỂ XEM CHI TIẾT & IMPORT",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = GrayMuted,
                            lineHeight = 18.sp,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    }
                }
            } else {
                // Asset Inspection Screen
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Big Preview Card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(GraphiteDeep)
                            .border(1.dp, BorderMuted, RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        asset.renderer(
                            Modifier
                                .size(110.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(SpaceBlack)
                        )

                        // Floating Sfx Test Trigger Button for sound effects
                        if (asset.category == "sound" && asset.sfxTrigger != null) {
                            FloatingActionButton(
                                onClick = { viewModel.playPreviewSound(asset.sfxTrigger) },
                                containerColor = TealCosmic,
                                contentColor = SpacePurpleDark,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(8.dp)
                                    .size(38.dp)
                            ) {
                                Icon(Icons.Default.VolumeUp, contentDescription = "Play Test", modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    // Metadata Header
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = asset.name,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TealCosmic
                        )
                        Text(
                            text = "Thư mục: assets/${asset.id}",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = GrayMuted
                        )
                    }

                    // Asset Description Text
                    Text(
                        text = asset.description,
                        fontSize = 11.sp,
                        color = WhiteCream,
                        lineHeight = 16.sp
                    )

                    Divider(color = BorderMuted, thickness = 1.dp)

                    // Actions block
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Action 1: Insert Snippet to Code
                        Button(
                            onClick = {
                                val currentCode = viewModel.editorCode.value
                                val codeToInsert = "\n" + asset.codeSnippet + "\n"
                                viewModel.updateCurrentCode(currentCode + codeToInsert)
                                Toast.makeText(
                                    context,
                                    "Đã thêm asset '${asset.name.substringBefore(" (")}' vào cuối mã kịch bản!",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = TealCosmic,
                                contentColor = SpacePurpleDark
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Insert", modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Chèn Vào Mã", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        // Action 2: Copy to Clipboard
                        OutlinedButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(asset.codeSnippet))
                                Toast.makeText(context, "Đã chép mã asset vào khay nhớ tạm!", Toast.LENGTH_SHORT).show()
                            },
                            border = BorderStroke(1.dp, BorderMuted),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TealCosmic)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(14.dp))
                        }
                    }

                    // Python Code Snippet Syntax Highlight View Box
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(GraphiteDeep)
                            .border(1.dp, BorderMuted, RoundedCornerShape(12.dp))
                            .padding(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "MÃ NGUỒN PYTHON KHAI BÁO",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = GrayMuted,
                                letterSpacing = 1.sp
                            )
                            Icon(Icons.Default.Code, contentDescription = null, tint = GrayMuted, modifier = Modifier.size(12.dp))
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = asset.codeSnippet,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = WhiteCream,
                            lineHeight = 15.sp,
                            modifier = Modifier.horizontalScroll(rememberScrollState())
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SoundWaveCard(modifier: Modifier, color: Color) {
    Box(
        modifier = modifier.background(GraphiteDeep),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Equalizer animation mock
            repeat(6) { i ->
                val height = when (i) {
                    0 -> 20.dp
                    1 -> 34.dp
                    2 -> 50.dp
                    3 -> 40.dp
                    4 -> 24.dp
                    else -> 14.dp
                }
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(height)
                        .clip(RoundedCornerShape(2.dp))
                        .background(color)
                )
            }
        }
    }
}
