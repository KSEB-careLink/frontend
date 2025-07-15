package com.example.myapplication.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

// 알림 스케줄을 나타내는 데이터 클래스
data class ScheduleItem(
    val date: String,    // "YYYY-M-D"
    val content: String, // 입력한 일정 텍스트
    val hour: Int,       // 알림 시각 시(24h)
    val minute: Int      // 알림 시각 분
)

class OneTimeAlarmViewModel : ViewModel() {
    // 등록된 스케줄 목록
    var schedules by mutableStateOf(listOf<ScheduleItem>())
        private set

    // 입력 폼 보이기 여부
    var showInputArea by mutableStateOf(false)
        private set

    // 입력 중인 텍스트
    var scheduleText by mutableStateOf("")
        private set

    // 선택된 날짜 ("YYYY-M-D")
    var selectedDate by mutableStateOf<String?>(null)
        private set

    // 선택된 시간 (기본 09:00)
    var selectedHour by mutableStateOf(9)
        private set
    var selectedMinute by mutableStateOf(0)
        private set

    // 편집 중인 인덱스 (null 이면 새 등록)
    var editingIndex by mutableStateOf<Int?>(null)
        private set

    // 날짜 선택 시 호출
    fun onDateSelected(date: String) {
        selectedDate = date
        showInputArea = true
    }

    // 시간 선택 시 호출
    fun onTimeSelected(hour: Int, minute: Int) {
        selectedHour = hour
        selectedMinute = minute
    }

    // 텍스트 입력 시 호출
    fun onTextChanged(text: String) {
        scheduleText = text
    }

    // 등록 또는 수정 완료 시 호출
    fun addSchedule() {
        val date = selectedDate ?: return
        val content = scheduleText.trim().ifEmpty { return }
        val item = ScheduleItem(date, content, selectedHour, selectedMinute)

        schedules = if (editingIndex == null) {
            schedules + item
        } else {
            schedules.mapIndexed { idx, old ->
                if (idx == editingIndex) item else old
            }
        }

        // 상태 초기화
        editingIndex = null
        showInputArea = false
        scheduleText = ""
        selectedDate = null
        selectedHour = 9
        selectedMinute = 0
    }

    // 기존 스케줄 수정 버튼 클릭 시 호출
    fun editSchedule(index: Int) {
        val item = schedules[index]
        editingIndex = index
        scheduleText = item.content
        selectedDate = item.date
        selectedHour = item.hour
        selectedMinute = item.minute
        showInputArea = true
    }

    // 삭제 버튼 클릭 시 호출
    fun deleteSchedule(index: Int) {
        schedules = schedules.filterIndexed { idx, _ -> idx != index }
        // 편집 중이던 항목을 지우는 경우 리셋
        if (editingIndex == index) {
            editingIndex = null
            showInputArea = false
            scheduleText = ""
            selectedDate = null
            selectedHour = 9
            selectedMinute = 0
        }
    }
}


