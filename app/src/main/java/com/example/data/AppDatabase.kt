package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [Project::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "pydev_studio_db"
                )
                .addCallback(AppDatabaseCallback(scope))
                .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class AppDatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populateDatabase(database.projectDao())
                }
            }
        }

        suspend fun populateDatabase(projectDao: ProjectDao) {
            // Check if there are already projects to prevent double inserting
            // But onCreate is only called once, so we insert the templates here.
            
            val catchBallCode = """# Game Hứng Bóng
# Di chuyển thanh đỡ bằng cách chạm/kéo màn hình

paddle_x = 150
paddle_y = 500
paddle_w = 100
paddle_h = 20

ball_x = 200
ball_y = 100
ball_speed_y = 6
ball_speed_x = 3
ball_r = 15

score = 0
game_over = False

def on_start():
    global paddle_x, ball_x, ball_y, score, game_over, ball_speed_x, ball_speed_y
    paddle_x = 150
    ball_x = 180
    ball_y = 100
    ball_speed_x = 3
    ball_speed_y = 6
    score = 0
    game_over = False

def on_update():
    global ball_x, ball_y, ball_speed_x, ball_speed_y, score, game_over, paddle_x
    if game_over:
        draw_text("GAME OVER", 110, 250, 32, "RED")
        draw_text("Chạm để chơi lại", 120, 310, 18, "WHITE")
        return
        
    # Di chuyển bóng
    ball_x = ball_x + ball_speed_x
    ball_y = ball_y + ball_speed_y
    
    # Va chạm tường trái/phải
    if ball_x < ball_r or ball_x > 400 - ball_r:
        ball_speed_x = -ball_speed_x
        
    # Va chạm đỉnh
    if ball_y < ball_r:
        ball_speed_y = -ball_speed_y
        
    # Va chạm thanh đỡ
    if ball_y + ball_r >= paddle_y and ball_y - ball_r <= paddle_y + paddle_h:
        if ball_x >= paddle_x and ball_x <= paddle_x + paddle_w:
            ball_speed_y = -abs(ball_speed_y) - 0.3
            # Góc nảy ngẫu nhiên nhẹ dựa trên vị trí chạm
            hit_offset = ball_x - (paddle_x + paddle_w / 2)
            ball_speed_x = hit_offset * 0.12
            score = score + 1
            play_sound("coin")
            
    # Rơi xuống dưới - Thua cuộc
    if ball_y > 600:
        game_over = True
        play_sound("game_over")
        
    # Vẽ các thực thể
    draw_rect(paddle_x, paddle_y, paddle_w, paddle_h, "GREEN")
    draw_circle(ball_x, ball_y, ball_r, "YELLOW")
    draw_text("Điểm số: " + str(score), 20, 40, 20, "WHITE")
"""

            val spaceDodgeCode = """# Tránh Né Thiên Thạch
# Né tránh các thiên thạch rơi tự do bằng cách di chuyển phi thuyền trái phải

player_x = 180
player_y = 480
player_w = 40
player_h = 40

ast_x = 150
ast_y = 0
ast_speed = 5
ast_size = 25

score = 0
game_over = False

def on_start():
    global player_x, ast_x, ast_y, ast_speed, score, game_over
    player_x = 180
    ast_x = 150
    ast_y = 0
    ast_speed = 5
    score = 0
    game_over = False

def on_update():
    global player_x, player_y, ast_x, ast_y, ast_speed, score, game_over
    if game_over:
        draw_text("GAME OVER", 110, 250, 32, "RED")
        draw_text("Chạm để chơi lại", 120, 310, 18, "WHITE")
        return
    
    # Di chuyển thiên thạch xuống dưới
    ast_y = ast_y + ast_speed
    if ast_y > 600:
        ast_y = 0
        ast_x = random_range(20, 360)
        ast_speed = ast_speed + 0.3
        score = score + 1
        play_sound("point")
    
    # Kiểm tra va chạm hộp (bounding box collision)
    if ast_x + ast_size > player_x and ast_x < player_x + player_w:
        if ast_y + ast_size > player_y and ast_y < player_y + player_h:
            game_over = True
            play_sound("explode")
            
    # Vẽ phi thuyền hình chữ nhật và thiên thạch hình tròn
    draw_rect(player_x, player_y, player_w, player_h, "CYAN")
    draw_circle(ast_x + ast_size/2, ast_y + ast_size/2, ast_size/2, "RED")
    
    # Vẽ bảng điểm
    draw_text("Điểm: " + str(score), 20, 40, 22, "WHITE")
    draw_text("Di chuyển ngón tay để lái phi thuyền", 20, 560, 14, "GRAY")

def on_touch(tx, ty):
    global player_x, game_over
    if game_over:
        on_start()
    else:
        # Lái tàu theo ngón tay
        player_x = tx - player_w / 2
        if player_x < 10:
            player_x = 10
        if player_x > 390 - player_w:
            player_x = 390 - player_w
"""

            val magicPaintCode = """# Bản Vẽ Ma Thuật
# Vẽ tự do bằng ngón tay và nhấn các nút chọn màu ở bên dưới

brush_color = "YELLOW"
brush_size = 10

def on_start():
    print("Magic Paint Sẵn Sàng!")

def on_update():
    # Tiêu đề hướng dẫn
    draw_text("MAGIC PAINT", 130, 40, 22, "CYAN")
    draw_text("Hãy chạm và kéo để vẽ!", 100, 75, 14, "GRAY")
    
    # Vẽ bảng điều khiển màu sắc ở dưới cùng
    draw_rect(20, 500, 60, 40, "RED")
    draw_rect(100, 500, 60, 40, "GREEN")
    draw_rect(180, 500, 60, 40, "BLUE")
    draw_rect(260, 500, 60, 40, "YELLOW")
    draw_rect(340, 500, 60, 40, "WHITE")
    
    # Hiển thị màu đang chọn
    draw_text("Màu cọ: " + brush_color, 20, 460, 16, brush_color)

def on_touch(tx, ty):
    global brush_color
    # Nhấn chọn nút màu sắc
    if ty >= 500 and ty <= 540:
        if tx >= 20 and tx <= 80:
            brush_color = "RED"
        elif tx >= 100 and tx <= 160:
            brush_color = "GREEN"
        elif tx >= 180 and tx <= 240:
            brush_color = "BLUE"
        elif tx >= 260 and tx <= 320:
            brush_color = "YELLOW"
        elif tx >= 340 and tx <= 400:
            brush_color = "WHITE"
    else:
        # Vẽ một vòng tròn vĩnh viễn trên canvas tại tọa độ chạm
        draw_persistent_circle(tx, ty, brush_size, brush_color)
"""

            val flappyBirdCode = """# Flappy Bird Mini
# Chạm màn hình để nhảy tránh chướng ngại vật

bird_y = 250
bird_v = 0
gravity = 0.4
jump = -7

pipe_x = 400
pipe_gap = 130
pipe_w = 60
pipe_top_h = 150

score = 0
game_over = False

def on_start():
    global bird_y, bird_v, pipe_x, pipe_top_h, score, game_over
    bird_y = 250
    bird_v = 0
    pipe_x = 400
    pipe_top_h = random_range(100, 250)
    score = 0
    game_over = False

def on_update():
    global bird_y, bird_v, pipe_x, pipe_top_h, score, game_over
    if game_over:
        draw_text("BIRD CRASHED", 90, 230, 32, "RED")
        draw_text("Điểm số: " + str(score), 150, 290, 22, "YELLOW")
        draw_text("Chạm để hồi sinh", 125, 340, 16, "WHITE")
        return
        
    # Áp dụng trọng lực
    bird_v = bird_v + gravity
    bird_y = bird_y + bird_v
    
    # Giới hạn biên màn hình
    if bird_y < 10 or bird_y > 470:
        game_over = True
        play_sound("die")
        
    # Di chuyển ống cản
    pipe_x = pipe_x - 4
    if pipe_x < -pipe_w:
        pipe_x = 400
        pipe_top_h = random_range(100, 250)
        score = score + 1
        play_sound("point")
        
    # Kiểm tra va chạm ống trên & ống dưới
    bird_x = 100
    bird_r = 15
    if pipe_x <= bird_x + bird_r and pipe_x + pipe_w >= bird_x - bird_r:
        if bird_y - bird_r <= pipe_top_h or bird_y + bird_r >= pipe_top_h + pipe_gap:
            game_over = True
            play_sound("hit")
            
    # Vẽ ống nước
    draw_rect(pipe_x, 0, pipe_w, pipe_top_h, "GREEN")
    draw_rect(pipe_x, pipe_top_h + pipe_gap, pipe_w, 500 - (pipe_top_h + pipe_gap), "GREEN")
    
    # Vẽ chú chim
    draw_circle(bird_x, bird_y, bird_r, "YELLOW")
    # Vẽ mắt nhỏ đáng yêu
    draw_circle(bird_x + 6, bird_y - 4, 3, "BLACK")
    
    # Vẽ bảng điểm
    draw_text("Điểm: " + str(score), 20, 40, 22, "WHITE")

def on_touch(tx, ty):
    global bird_v, game_over
    if game_over:
        on_start()
    else:
        bird_v = jump
        play_sound("wing")
"""

            projectDao.insertProject(Project(
                name = "Game Hứng Bóng (Catch Ball)",
                description = "Game hứng quả bóng rơi tự do từ trên cao bằng tấm gỗ trượt.",
                pythonCode = catchBallCode,
                visualDesignJson = """[{"id":"paddle","name":"paddle","type":"rect","x":150,"y":500,"w":100,"h":20,"color":"GREEN"},{"id":"ball","name":"ball","type":"circle","x":200,"y":100,"r":15,"color":"YELLOW"}]"""
            ))

            projectDao.insertProject(Project(
                name = "Tránh Né Thiên Thạch",
                description = "Điều khiển phi thuyền của bạn để tránh các thiên thạch đang rơi dồn dập.",
                pythonCode = spaceDodgeCode,
                visualDesignJson = """[{"id":"player","name":"player","type":"rect","x":180,"y":480,"w":40,"h":40,"color":"CYAN"},{"id":"ast","name":"ast","type":"circle","x":150,"y":0,"r":12,"color":"RED"}]"""
            ))

            projectDao.insertProject(Project(
                name = "Bản Vẽ Ma Thuật",
                description = "Ứng dụng vẽ cơ bản vẽ trực tiếp bằng ngón tay và đổi cọ màu.",
                pythonCode = magicPaintCode,
                visualDesignJson = "[]"
            ))

            projectDao.insertProject(Project(
                name = "Flappy Bird Mini",
                description = "Game Flappy Bird nổi tiếng xây dựng với đồ họa canvas mượt mà.",
                pythonCode = flappyBirdCode,
                visualDesignJson = """[{"id":"bird","name":"bird","type":"circle","x":100,"y":250,"r":15,"color":"YELLOW"}]"""
            ))
        }
    }
}
