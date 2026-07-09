package com.example.interpreter

import android.util.Log
import com.example.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiClient {
    private const val TAG = "GeminiClient"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent"

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    /**
     * Call Gemini to generate content based on the prompt
     */
    suspend fun generateContent(prompt: String, systemInstruction: String? = null): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "Chưa cấu hình API Key. Vui lòng thêm API Key hợp lệ trong bảng điều khiển hoặc tệp cấu hình."
        }

        try {
            val url = "$BASE_URL?key=$apiKey"
            
            // Build JSON structure manually using JSONObject to avoid dependency/serialization issues
            val root = JSONObject()
            
            // Contents array
            val contentsArray = JSONArray()
            val contentObj = JSONObject()
            val partsArray = JSONArray()
            val partObj = JSONObject()
            partObj.put("text", prompt)
            partsArray.put(partObj)
            contentObj.put("parts", partsArray)
            contentsArray.put(contentObj)
            root.put("contents", contentsArray)

            // System instructions if present
            if (systemInstruction != null) {
                val systemInstructionObj = JSONObject()
                val systemPartsArray = JSONArray()
                val systemPartObj = JSONObject()
                systemPartObj.put("text", systemInstruction)
                systemPartsArray.put(systemPartObj)
                systemInstructionObj.put("parts", systemPartsArray)
                root.put("systemInstruction", systemInstructionObj)
            }

            // Generation config (low temperature for coding tasks)
            val generationConfig = JSONObject()
            generationConfig.put("temperature", 0.2)
            root.put("generationConfig", generationConfig)

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = root.toString().toRequestBody(mediaType)

            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string()
                if (!response.isSuccessful || bodyStr == null) {
                    Log.e(TAG, "Request failed: Code: ${response.code}, Body: $bodyStr")
                    return@withContext "Lỗi kết nối máy chủ AI: ${response.code}. Vui lòng thử lại sau."
                }

                // Parse response
                val responseJson = JSONObject(bodyStr)
                val candidates = responseJson.optJSONArray("candidates")
                val firstCandidate = candidates?.optJSONObject(0)
                val content = firstCandidate?.optJSONObject("content")
                val parts = content?.optJSONArray("parts")
                val firstPart = parts?.optJSONObject(0)
                
                return@withContext firstPart?.optString("text") ?: "Không nhận được phản hồi phù hợp."
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception calling Gemini API: ${e.message}", e)
            return@withContext "Lỗi kết nối AI: ${e.localizedMessage}. Kiểm tra kết nối Internet của bạn."
        }
    }

    /**
     * Fixes Python code with errors using AI
     */
    suspend fun fixPythonCode(code: String, errorMsg: String?): String {
        val systemPrompt = """
            Bạn là một chuyên gia lập trình game Python thông thái tích hợp trong ứng dụng PyDev Studio.
            Nhiệm vụ của bạn là sửa lỗi đoạn mã nguồn Python do người dùng gửi dựa trên thông báo lỗi (nếu có).
            
            Quy tắc sửa đổi mã nguồn:
            1. Bạn PHẢI trả về toàn bộ mã nguồn đã sửa.
            2. Sử dụng đúng các hàm vẽ có sẵn được thiết lập trong ứng dụng này:
               - draw_rect(x, y, w, h, color) -> Vẽ hình chữ nhật
               - draw_circle(x, y, r, color) -> Vẽ hình tròn
               - draw_text(text, x, y, size, color) -> Vẽ văn bản
               - draw_persistent_circle(x, y, size, color) -> Vẽ điểm cọ vẽ vĩnh viễn (để vẽ nét vẽ)
               - play_sound(sound_name) -> Phát hiệu ứng âm thanh (ví dụ: 'coin', 'hit', 'point', 'game_over')
               - random_range(min, max) -> Lấy số thực ngẫu nhiên từ min đến max
               - str(val) -> Chuyển số thành chuỗi
            3. Hãy đảm bảo khai báo biến toàn cục (global) đầy đủ bên trong các hàm `on_update` hay `on_touch` nếu có chỉnh sửa giá trị biến đó. Ví dụ: `global score, x`
            4. Chỉ trả về mã Python trong khối ```python ... ``` và một giải thích rất ngắn gọn, xúc tích bằng tiếng Việt phía dưới về lỗi đã sửa. Không viết lan man.
        """.trimIndent()

        val userPrompt = """
            Mã nguồn Python hiện tại:
            ```python
            $code
            ```
            
            Thông báo lỗi runtime:
            ${errorMsg ?: "Không có lỗi rõ ràng, hãy tối ưu hóa logic game mượt mà hơn."}
            
            Hãy sửa lỗi hoặc tối ưu hóa mã trên và trả về kết quả chuẩn xác.
        """.trimIndent()

        return generateContent(userPrompt, systemPrompt)
    }

    /**
     * Generates a game Python script from a description
     */
    suspend fun generateGameFromPrompt(userDescription: String): String {
        val systemPrompt = """
            Bạn là một lập trình viên game Python xuất chúng của PyDev Studio, tối ưu hóa viết game mượt mà trên điện thoại.
            Hãy viết một tệp game Python hoàn chỉnh dựa trên mô tả của người dùng.
            
            API vẽ game được hỗ trợ (BẠN CHỈ ĐƯỢC PHÉP DÙNG CÁC HÀM NÀY ĐỂ VẼ):
            - draw_rect(x, y, w, h, color): Vẽ hình chữ nhật. (color có thể là "RED", "GREEN", "BLUE", "YELLOW", "CYAN", "WHITE", "GRAY", "BLACK", "MAGENTA")
            - draw_circle(x, y, r, color): Vẽ hình tròn tại tọa độ (x, y) bán kính r.
            - draw_text(text, x, y, size, color): Vẽ chữ viết lên màn hình tại (x, y) cỡ chữ size.
            - play_sound(sound_name): Phát âm thanh ngắn (ví dụ: 'point', 'coin', 'hit', 'explode', 'jump', 'laser').
            - random_range(min, max): Trả về số thực ngẫu nhiên giữa min và max.
            
            Quy tắc cấu trúc game:
            1. Màn hình game có kích thước cố định là 400x600 (X từ 0-400, Y từ 0-600).
            2. Trò chơi có thể thiết lập các biến toàn cục ở ngoài cùng để quản lý trạng thái (ví dụ: player_x = 150, score = 0, game_over = False).
            3. Ba hàm sự kiện chính:
               - def on_start(): Khởi tạo lại toàn bộ giá trị biến khi bắt đầu/reset game.
               - def on_update(): Được gọi liên tục ở 60fps để vẽ các thành phần và di chuyển logic. Nhớ khai báo 'global' cho tất cả biến cần sửa đổi giá trị.
               - def on_touch(tx, ty): Được gọi khi người dùng chạm hoặc kéo trên màn hình canvas game với tọa độ chạm là tx, ty.
            4. Hãy viết mã nguồn Python tối giản, dễ hiểu, tối ưu mượt mà ngay cả trên điện thoại cấu hình thấp.
            5. Chỉ trả về mã nguồn trong khối ```python ... ``` và kèm theo giới thiệu ngắn bằng tiếng Việt.
        """.trimIndent()

        return generateContent(userDescription, systemPrompt)
    }
}
