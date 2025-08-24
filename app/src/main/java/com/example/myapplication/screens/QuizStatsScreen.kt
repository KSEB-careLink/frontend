// app/src/main/java/com/example/myapplication/screens/QuizStatsScreen.kt
package com.example.myapplication.screens

import android.content.Context
import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.BuildConfig
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.math.min
import androidx.compose.ui.graphics.nativeCanvas

private const val TAG = "QuizStats"

@Composable
fun QuizStatsScreen() {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
    val activePatientId = prefs.getString("patient_id", null)

    // --- 카테고리별 누적 통계 ---
    var statsList by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var totalAccuracy by remember { mutableStateOf<Double?>(null) }
    var totalAvgTime by remember { mutableStateOf<Double?>(null) }

    // --- 월간 통계 + 드롭다운 ---
    val seoul = remember { ZoneId.of("Asia/Seoul") }
    val ymFormatter = remember { DateTimeFormatter.ofPattern("yyyy-MM") }
    val currentMonth = remember { YearMonth.now(seoul).format(ymFormatter) }
    var month by remember { mutableStateOf(currentMonth) }

    var monthlyObj by remember { mutableStateOf<JSONObject?>(null) }
    var monthlyDaily by remember { mutableStateOf<List<JSONObject>>(emptyList()) }

    // 월간 예측/차트 필드
    var monthlyPredAcc by remember { mutableStateOf<Double?>(null) }
    var monthlyAccuracyPct by remember { mutableStateOf<Double?>(null) }
    var monthlyColdStart by remember { mutableStateOf<Boolean?>(null) }
    var monthlyBadges by remember { mutableStateOf<List<String>>(emptyList()) }

    // 최근 6개월 옵션
    val monthOptions = remember(currentMonth) {
        val start = YearMonth.parse(currentMonth, ymFormatter)
        (0 until 6).map { i -> start.minusMonths(i.toLong()).format(ymFormatter) }
    }

    var errorMsg by remember { mutableStateOf<String?>(null) }

    // 타임아웃 포함 클라이언트
    val client = remember {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    // 서버 경로 상수
    val PATH_STATS = "/quiz-stats"
    val PATH_MONTHLY_BASE = "/quiz-stats"

    // 초기 환경 로그
    LaunchedEffect(Unit) {
        Log.d(TAG, "BASE_URL=${BuildConfig.BASE_URL}")
        Log.d(TAG, "PATH_STATS=$PATH_STATS, PATH_MONTHLY_BASE=$PATH_MONTHLY_BASE")
    }

    LaunchedEffect(activePatientId, month) {
        if (activePatientId.isNullOrBlank()) {
            errorMsg = "환자 ID가 없습니다."
            Log.e(TAG, "patientId is null/blank. Abort requests.")
            return@LaunchedEffect
        }

        // Firebase ID 토큰
        val idToken = try {
            Firebase.auth.currentUser?.getIdToken(true)?.await()?.token
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get Firebase ID token: ${e.message}", e)
            null
        }

        if (idToken.isNullOrBlank()) {
            errorMsg = "인증 토큰이 없습니다."
            Log.e(TAG, "ID token is null/blank. Abort requests.")
            return@LaunchedEffect
        }

        Log.d(TAG, "patientId=$activePatientId, tokenLen=${idToken.length}, tokenMask=${mask(idToken)}")
        Log.d(TAG, "Selected month=$month")

        // 선택 월 변경 시 월간 섹션 초기화
        monthlyObj = null
        monthlyDaily = emptyList()
        monthlyPredAcc = null
        monthlyAccuracyPct = null
        monthlyColdStart = null
        monthlyBadges = emptyList()

        // 1) 카테고리별 누적 통계
        launch {
            val url = "${BuildConfig.BASE_URL}$PATH_STATS?patient_id=${enc(activePatientId)}"
            try {
                val t0 = System.nanoTime()
                Log.d(TAG, "[REQ] GET $url")
                Log.d(TAG, "[HDR] Authorization=Bearer ${mask(idToken)}")
                val resp = withContext(Dispatchers.IO) {
                    client.newCall(
                        Request.Builder()
                            .url(url)
                            .addHeader("Authorization", "Bearer $idToken")
                            .get()
                            .build()
                    ).execute()
                }
                val bodyStr = resp.body?.string().orEmpty()
                val tookMs = (System.nanoTime() - t0) / 1_000_000.0
                Log.d(TAG, "[RES] code=${resp.code}, took=${"%.1f".format(tookMs)}ms")
                Log.d(TAG, "[BODY] ${bodyStr.take(2000)}")

                if (!resp.isSuccessful) throw Exception("통계 API 오류: ${resp.code}")

                val root = JSONObject(bodyStr)
                totalAccuracy = root.optDoubleOrNull("total_accuracy")
                totalAvgTime = root.optDoubleOrNull("total_avg_time")
                val arr: JSONArray = root.optJSONArray("categories") ?: throw Exception("categories 배열이 없습니다.")
                statsList = List(arr.length()) { i -> arr.getJSONObject(i) }
                if (errorMsg?.startsWith("통계") == true) errorMsg = null
            } catch (e: Exception) {
                Log.e(TAG, "누적 통계 요청 실패: ${e.message}", e)
                errorMsg = "통계 로드 실패: ${e.message}"
                statsList = emptyList()
                totalAccuracy = null
                totalAvgTime = null
            }
        }

        // 2) 월간 통계
        launch {
            val ts = System.currentTimeMillis()
            val url = "${BuildConfig.BASE_URL}$PATH_MONTHLY_BASE/monthly" +
                    "?patient_id=${enc(activePatientId)}&month=${enc(month)}&_ts=$ts"
            try {
                val t0 = System.nanoTime()
                Log.d(TAG, "[REQ] GET $url")
                Log.d(TAG, "[HDR] Authorization=Bearer ${mask(idToken)}")
                val resp = withContext(Dispatchers.IO) {
                    client.newCall(
                        Request.Builder()
                            .url(url)
                            .addHeader("Authorization", "Bearer $idToken")
                            .get()
                            .build()
                    ).execute()
                }
                val bodyStr = resp.body?.string().orEmpty()
                val tookMs = (System.nanoTime() - t0) / 1_000_000.0
                Log.d(TAG, "[RES] code=${resp.code}, took=${"%.1f".format(tookMs)}ms")
                Log.d(TAG, "[BODY] ${bodyStr.take(2000)}")

                if (!resp.isSuccessful) throw Exception("월간 API 오류: ${resp.code}")

                val root = JSONObject(bodyStr)
                monthlyObj = root

                monthlyPredAcc = root.optDoubleOrNull("predicted_final_accuracy")
                monthlyColdStart = root.optBooleanOrNull("cold_start")
                monthlyAccuracyPct = root.optDoubleOrNull("accuracy_percent")
                    ?: monthlyPredAcc?.let { if (it <= 1.0) it * 100.0 else it }

                val dailyArr = root.optJSONArray("daily")
                    ?: root.optJSONArray("by_day")
                    ?: root.optJSONArray("days")
                monthlyDaily = if (dailyArr != null) {
                    List(dailyArr.length()) { i -> dailyArr.getJSONObject(i) }
                } else emptyList()

                if (errorMsg?.startsWith("월간") == true) errorMsg = null
            } catch (e: Exception) {
                Log.e(TAG, "월간 통계 요청 실패: ${e.message}", e)
                errorMsg = "월간 통계 로드 실패: ${e.message}"
                monthlyObj = null
                monthlyDaily = emptyList()
                monthlyPredAcc = null
                monthlyAccuracyPct = null
                monthlyColdStart = null
                monthlyBadges = emptyList()
            }
        }
    }

    // ─────────────────────────────────────────────────────────────
    // 화면 UI (테두리 + 꺾은선 그래프)
    // ─────────────────────────────────────────────────────────────
    Box(
        modifier = Modifier
            .fillMaxSize()
            .border(width = 7.dp, color = Color(0xFFE795BF))
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            errorMsg?.let {
                Text("오류: $it", color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(8.dp))
            }

            Text("월간 통계", fontSize = 20.sp)
            Spacer(Modifier.height(26.dp))

            MonthSelector(
                month = month,
                options = monthOptions,
                onSelect = { selected ->
                    Log.d(TAG, "[UI] month option selected=$selected")
                    month = selected
                }
            )

            Spacer(Modifier.height(18.dp))

            val chartPoints = remember(month, monthlyDaily, monthlyAccuracyPct) {
                buildSampledLinePoints(
                    month = month,
                    dailyList = monthlyDaily,
                    accuracyPercent = monthlyAccuracyPct,
                    zoneId = seoul
                )
            }

            if (chartPoints.isNotEmpty()) {
                ChartCard(
                    title = "정답률 추이 (전 달 15일 / 미래 10일 주기)",
                    subtitle = "• 과거: 1·16·말일  • 미래: 10·20·말일\n" +
                            "• 미래는 말일의 월 기준값(예측/월 정답률)까지 선형 보간"
                ) {
                    LineChart(
                        points = chartPoints,
                        month = month,
                        yMin = 0.0,
                        yMax = 100.0,
                        height = 220.dp,
                        leftPadding = 36.dp,
                        rightPadding = 16.dp,
                        topPadding = 12.dp,
                        bottomPadding = 28.dp
                    )
                }
            } else {
                Text("차트 데이터 없음")
            }

            Spacer(Modifier.height(24.dp))
            Divider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(24.dp))

            Text("카테고리별 누적 통계", fontSize = 20.sp)
            Text(
                "풀이 기록이 많을수록 예측 정답률이 실제에 가까워집니다.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
            Spacer(Modifier.height(6.dp))

            totalAccuracy?.let { Text("전체 정답률: ${"%.1f".format(it)}%") }
            totalAvgTime?.let { Text("평균 응답 시간: ${"%.1f".format(it)}초") }
            if (totalAccuracy != null || totalAvgTime != null) Spacer(Modifier.height(8.dp))

            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                statsList.forEach { stat ->
                    val category = stat.optString("category", "-")
                    val total = stat.optInt("total", 0)
                    val correct = stat.optInt("correct", 0)
                    val accPct = stat.optDouble("accuracy", 0.0).coerceIn(0.0, 100.0)
                    val avgTime = stat.optDouble("avg_time", 0.0)

                    Column {
                        Text(
                            "$category: 정답 $correct / $total, 정답률 ${"%.1f".format(accPct)}%, 평균응답 ${"%.1f".format(avgTime)}초"
                        )
                        Spacer(Modifier.height(6.dp))
                        Column {
                            Text(
                                "정답률 ${"%.1f".format(accPct)}%",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                            Spacer(Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = (accPct / 100.0).toFloat(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Spacer(Modifier.navigationBarsPadding())
        }
    }
}

/** 월 선택: 드롭다운 + 이전/다음달 버튼 */
@Composable
private fun MonthSelector(
    month: String,
    options: List<String>,
    onSelect: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val idx = remember(month, options) { options.indexOf(month).coerceAtLeast(0) }
    val canGoNewer = idx > 0
    val canGoOlder = idx < options.size - 1

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        TextButton(
            onClick = {
                val next = options.getOrNull(idx + 1) ?: return@TextButton
                Log.d(TAG, "[UI] month prev-button -> $next")
                onSelect(next)
            },
            enabled = canGoOlder
        ) { Text("이전달") }

        Spacer(Modifier.width(8.dp))

        Box(
            modifier = Modifier
                .weight(1f)
                .wrapContentHeight(),
            contentAlignment = Alignment.TopStart
        ) {
            OutlinedTextField(
                value = month,
                onValueChange = {},
                readOnly = true,
                label = { Text("월 선택 (YYYY-MM)") },
                trailingIcon = { Text(if (expanded) "▲" else "▼") },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
            )

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            onSelect(option)
                            expanded = false
                        }
                    )
                }
            }
        }

        Spacer(Modifier.width(8.dp))

        TextButton(
            onClick = {
                val prev = options.getOrNull(idx - 1) ?: return@TextButton
                Log.d(TAG, "[UI] month next-button -> $prev")
                onSelect(prev)
            },
            enabled = canGoNewer
        ) { Text("다음달") }
    }
}

/* ─────────────────────────────
   꺾은선 차트 Card 래퍼
   ───────────────────────────── */
@Composable
private fun ChartCard(
    title: String,
    subtitle: String? = null,
    content: @Composable () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.ShowChart,
                    contentDescription = null
                )
                Spacer(Modifier.width(6.dp))
                Text(title, fontSize = 16.sp)
            }
            if (!subtitle.isNullOrBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    lineHeight = 18.sp
                )
            }
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

/* ─────────────────────────────
   데이터 포인트
   ───────────────────────────── */
private data class ChartPoint(
    val day: Int,            // 1..말일
    val valuePct: Double,    // 0..100
    val isFuture: Boolean
)

/* ─────────────────────────────
   라인 차트 (Canvas)
   ───────────────────────────── */
@Composable
private fun LineChart(
    points: List<ChartPoint>,
    month: String,
    yMin: Double = 0.0,
    yMax: Double = 100.0,
    height: Dp = 220.dp,
    leftPadding: Dp = 36.dp,
    rightPadding: Dp = 16.dp,
    topPadding: Dp = 16.dp,
    bottomPadding: Dp = 28.dp,
) {
    val ym = remember(month) { YearMonth.parse(month, DateTimeFormatter.ofPattern("yyyy-MM")) }
    val lastDay = ym.lengthOfMonth()
    val dashed = remember { PathEffect.dashPathEffect(floatArrayOf(18f, 12f), 0f) }

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
    ) {
        val lp = leftPadding.toPx()
        val rp = rightPadding.toPx()
        val tp = topPadding.toPx()
        val bp = bottomPadding.toPx()

        val w = size.width - lp - rp
        val h = size.height - tp - bp
        if (w <= 0f || h <= 0f || points.isEmpty()) return@Canvas

        fun xForDay(day: Int): Float {
            val t = if (lastDay <= 1) 0f else (day - 1).toFloat() / (lastDay - 1).toFloat()
            return lp + t * w
        }
        fun yForVal(v: Double): Float {
            val clamped = v.coerceIn(yMin, yMax)
            val t = if (yMax == yMin) 0.0 else (clamped - yMin) / (yMax - yMin)
            return (tp + (1f - t.toFloat()) * h)
        }

        // 가이드 라인 (0/50/100%)
        val guideVals = listOf(0.0, 50.0, 100.0)
        guideVals.forEach { gv ->
            val y = yForVal(gv)
            drawLine(
                color = Color(0x22000000),
                start = Offset(lp, y),
                end = Offset(size.width - rp, y),
                strokeWidth = 1f
            )
        }

        // X축 주요일(1/10/15/20/말일) 가이드
        val keyXs = listOf(1, 10, 15, 20, lastDay).distinct()
        keyXs.forEach { d ->
            val x = xForDay(d)
            drawLine(
                color = Color(0x11000000),
                start = Offset(x, tp),
                end = Offset(x, size.height - bp),
                strokeWidth = 1f
            )
        }

        // 과거/미래 경로
        val sorted = points.sortedBy { it.day }
        val firstFutureIndex = sorted.indexOfFirst { it.isFuture }.let { if (it < 0) sorted.size else it }

        if (firstFutureIndex > 0) {
            val pastPath = Path().apply {
                moveTo(xForDay(sorted[0].day), yForVal(sorted[0].valuePct))
                for (i in 1 until firstFutureIndex) {
                    lineTo(xForDay(sorted[i].day), yForVal(sorted[i].valuePct))
                }
            }
            drawPath(path = pastPath, color = Color(0xFF1E88E5), style = Stroke(width = 4f))
        }

        if (firstFutureIndex < sorted.size) {
            val startIndex = max(0, firstFutureIndex - 1)
            val futPath = Path().apply {
                moveTo(xForDay(sorted[startIndex].day), yForVal(sorted[startIndex].valuePct))
                for (i in startIndex + 1 until sorted.size) {
                    lineTo(xForDay(sorted[i].day), yForVal(sorted[i].valuePct))
                }
            }
            drawPath(
                path = futPath,
                color = Color(0xFFD81B60),
                style = Stroke(width = 4f, pathEffect = dashed)
            )
        }

        // 포인트 & 값 라벨 (drawIntoCanvas 사용)
        sorted.forEachIndexed { idx, p ->
            val cx = xForDay(p.day)
            val cy = yForVal(p.valuePct)
            val sz = if (p.isFuture) 5f else 6.5f
            drawCircle(
                color = if (p.isFuture) Color(0xFFD81B60) else Color(0xFF1E88E5),
                radius = sz,
                center = Offset(cx, cy)
            )

            val labelNeeded = idx == 0 || idx == sorted.lastIndex || p.day in listOf(1, 10, 15, 20, lastDay)
            if (labelNeeded) {
                drawIntoCanvas { canvas ->
                    val paint = android.graphics.Paint().apply {
                        color = android.graphics.Color.DKGRAY
                        textSize = 26f
                        isAntiAlias = true
                    }
                    canvas.nativeCanvas.drawText(
                        "${"%.0f".format(p.valuePct)}%",
                        cx + 6f,
                        cy - 10f,
                        paint
                    )
                }
            }
        }

        // X축 라벨
        val labelDays = listOf(1, 10, 15, 20, lastDay).distinct().sorted()
        labelDays.forEach { d ->
            drawIntoCanvas { canvas ->
                val paint = android.graphics.Paint().apply {
                    color = android.graphics.Color.GRAY
                    textSize = 26f
                    isAntiAlias = true
                }
                val tx = xForDay(d)
                val ty = size.height - bp + 22f
                val text = if (d == lastDay) "말일" else "${d}일"
                canvas.nativeCanvas.drawText(text, tx - 18f, ty, paint)
            }
        }

        // Y축 라벨
        guideVals.forEach { gv ->
            val y = yForVal(gv)
            drawIntoCanvas { canvas ->
                val paint = android.graphics.Paint().apply {
                    color = android.graphics.Color.GRAY
                    textSize = 26f
                    isAntiAlias = true
                }
                canvas.nativeCanvas.drawText("${gv.toInt()}%", 4f, y - 6f, paint)
            }
        }
    }
}

/* ─────────────────────────────
   표본 생성 로직
   ───────────────────────────── */
private fun buildSampledLinePoints(
    month: String,
    dailyList: List<JSONObject>,
    accuracyPercent: Double?,
    zoneId: ZoneId
): List<ChartPoint> {
    val ym = YearMonth.parse(month, DateTimeFormatter.ofPattern("yyyy-MM"))
    val lastDay = ym.lengthOfMonth()
    val today = LocalDate.now(zoneId)
    val relation = ym.compareTo(YearMonth.from(today))

    // 일→정답률 맵
    val dailyMap = mutableMapOf<Int, Double>()
    dailyList.forEach { obj ->
        val acc = obj.optDoubleOrNull("accuracy")
            ?: obj.optDoubleOrNull("accuracy_percent")
        if (acc != null) {
            val day = runCatching {
                when {
                    obj.has("date") && !obj.isNull("date") -> {
                        LocalDate.parse(obj.getString("date")).dayOfMonth
                    }
                    obj.has("day") && !obj.isNull("day") -> {
                        val s = obj.get("day").toString()
                        if (s.contains("-")) LocalDate.parse(s).dayOfMonth else s.toInt()
                    }
                    obj.has("dt") && !obj.isNull("dt") -> {
                        val s = obj.get("dt").toString()
                        if (s.contains("-")) LocalDate.parse(s).dayOfMonth else s.toInt()
                    }
                    else -> null
                }
            }.getOrNull()
            if (day != null && day in 1..lastDay) {
                dailyMap[day] = acc.coerceIn(0.0, 100.0)
            }
        }
    }

    val monthRef = (accuracyPercent ?: 50.0).coerceIn(0.0, 100.0)

    fun nearestKnownBeforeOrEqual(d: Int): Pair<Int, Double>? {
        var best: Pair<Int, Double>? = null
        for (i in 1..d) {
            val v = dailyMap[i] ?: continue
            best = i to v
        }
        return best
    }
    fun nearestKnownAfterOrEqual(d: Int): Pair<Int, Double>? {
        for (i in d..lastDay) {
            val v = dailyMap[i] ?: continue
            return i to v
        }
        return null
    }
    fun interpolate(aDay: Int, aVal: Double, bDay: Int, bVal: Double, d: Int): Double {
        if (bDay == aDay) return aVal
        val t = (d - aDay).toDouble() / (bDay - aDay).toDouble()
        return (aVal + (bVal - aVal) * t).coerceIn(0.0, 100.0)
    }
    fun valueForPastDay(d: Int): Double {
        dailyMap[d]?.let { return it }
        val left = nearestKnownBeforeOrEqual(d)
        val right = nearestKnownAfterOrEqual(d)
        return when {
            left != null && right != null -> interpolate(left.first, left.second, right.first, right.second, d)
            left != null -> left.second
            right != null -> right.second
            else -> monthRef
        }
    }

    val sampledDaysPast = mutableListOf<Int>()
    val sampledDaysFuture = mutableListOf<Int>()

    when {
        // 과거 달: 전부 과거 → 15일 주기
        relation < 0 -> {
            sampledDaysPast += listOf(1, min(16, lastDay), lastDay).distinct()
        }
        // 현재 달: 과거 15일 / 미래 10일
        relation == 0 -> {
            val cutoff = min(today.dayOfMonth, lastDay)
            sampledDaysPast += 1
            if (16 <= cutoff) sampledDaysPast += 16
            if (cutoff !in sampledDaysPast) sampledDaysPast += cutoff
            listOf(10, 20, lastDay).forEach { d ->
                if (d > cutoff) sampledDaysFuture += d
            }
        }
        // 미래 달: 전부 미래 → 10일 주기
        else -> {
            sampledDaysFuture += listOf(1, min(10, lastDay), min(20, lastDay), lastDay).distinct()
        }
    }

    val result = mutableListOf<ChartPoint>()
    sampledDaysPast.distinct().sorted().forEach { d ->
        result += ChartPoint(day = d, valuePct = valueForPastDay(d), isFuture = false)
    }

    if (sampledDaysFuture.isNotEmpty()) {
        val startDay = if (result.isNotEmpty()) result.last().day else sampledDaysFuture.first()
        val startVal = if (result.isNotEmpty()) result.last().valuePct else monthRef
        val endDay = lastDay
        val endVal = monthRef

        sampledDaysFuture.distinct().sorted().forEach { d ->
            val v = if (endDay == startDay) endVal
            else {
                val t = (d - startDay).toDouble() / (endDay - startDay).toDouble()
                (startVal + (endVal - startVal) * t).coerceIn(0.0, 100.0)
            }
            result += ChartPoint(day = d, valuePct = v, isFuture = true)
        }
    }

    return result
        .groupBy { it.day to it.isFuture }
        .map { (_, list) -> list.maxBy { it.valuePct } }
        .sortedBy { it.day }
}

/* ─────────────────────────────
   유틸
   ───────────────────────────── */
private fun enc(s: String): String =
    URLEncoder.encode(s, StandardCharsets.UTF_8.toString())

private fun mask(token: String, prefix: Int = 10, suffix: Int = 6): String = when {
    token.length <= prefix + suffix -> "****"
    else -> token.take(prefix) + "..." + token.takeLast(suffix)
}

private fun JSONObject.optDoubleOrNull(key: String): Double? =
    if (has(key) && !isNull(key)) {
        val v = optDouble(key, Double.NaN)
        if (v.isNaN()) null else v
    } else null

private fun JSONObject.optIntOrNull(key: String): Int? =
    if (has(key) && !isNull(key)) optInt(key) else null

private fun JSONObject.optBooleanOrNull(key: String): Boolean? =
    if (has(key) && !isNull(key)) optBoolean(key) else null







