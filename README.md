# 📱 프론트엔드

> **언어:** Kotlin  
> 실제로 보여지는 화면들은 `com.example.myapplication.screens` 패키지에,  
> 화면에서 쓰이는 추가 기능들은 `com.example.myapplication` 패키지에 위치합니다.

> ⚠️ **브랜치 안내:** `main` 브랜치는 사용하지 않습니다.

---

## 🧩 공통 기능 및 화면
- ✅ **로고 화면 및 애니메이션 효과** — `SplashScreen.kt`
- ✅ **보호자/환자 선택 화면** — `Choose_Position Page.kt`
- ✅ **잠금화면 해제 시 “소중한 하루를 챙겨보세요!” 알림 전송** — `LockOverlayNotificationHelper`, `LockOverlayReceiver`, `MyApplication`

---

## 🛡️ 보호자 기능 & 화면
1. ✅ **회원가입** — `GuardianSignInPage.kt`
2. ✅ **로그인** — `Guardian_Login.kt`
3. ✅ **joincode 연동** — `Code.kt`
4. ✅ **메인(연동된 환자 uid 선택 화면)** — `Main_Page.kt`
5. ✅ **튜토리얼 페이지** — `OnboardingScreen.kt`
6. ✅ **메인 화면** — `Main_Page2.kt`
7. ✅ **기본 정보 입력** — `GuardianBasicInfo.kt`
8. ✅ **회상 정보 입력** — `MemoryInfoInputScreen.kt`
9. ✅ **회상 정보 데이터 확인** — `MemoryInfoListScreen.kt`
10. ✅ **보호자 및 환자 정기 알림** — `Guardian_Alarm.kt`
11. ✅ **음성 목록/등록/자동 스크롤** — `Recode.kt`, `Recode.kt2`
12. ✅ **누적 카테고리별 통계 & 월간 통계** — `QuizStatsScreen.kt`
13. ✅ **환자 위치확인 & Geofence, 이탈 시 보호자 푸시 알림** — `LocationScreen.kt`

---

## 🧑‍⚕️ 환자 기능 & 화면
1. ✅ **회원가입** — `PatientSignInPage.kt`
2. ✅ **로그인 및 자동 로그인** — `Patient_Login.kt`
3. ✅ **joincode 연동** — `Code2.kt`
4. ✅ **회상문장** — `Patient_Sentence.kt`
5. ✅ **회상퀴즈** — `Patient_Quiz.kt`
6. ✅ **긴급알림** — `Patient_Alert.kt`

---

## 🧰 기술 스택
| 분야 | 내용                                                                        |
|---|---------------------------------------------------------------------------|
| 언어 | Kotlin                                                                    |
| 패키지 구조 | 화면: `com.example.myapplication.screens` / 공통: `com.example.myapplication` |

