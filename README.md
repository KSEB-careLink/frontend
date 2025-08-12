# frontend
main branch는 사용하지 않음 
언어: Kotlin
com.example.myapplication.screens에 실제로 보여지는 화면들이 있고 화면에 쓰이는 기능들은 com.example.myapplication의 package에 존재함

공통 기능 및 화면: 
1.로고 화면 및 에니메이션 효과.
2.보호자,환자 선택 화면.
3.잠금화면 해제시 소중한 하루를 챙겨보세요! 알림 전송.

보호자 쪽 기능 및 화면:
1.회원가입. / GuardianSignInPage.kt
2.로그인. / Guardian_Login.kt
3.joincode 연동. / Code.kt
4.보호자 main 화면에서 연동된 환자 uid 선택 메인화면 구현. / Main_Page.kt
5.튜토리얼 페이지. / OnboardingScreen.kt
6.메인화면. / Main_Page2.kt
7.기본 정보 입력. / GuardianBasicInfo.kt
8.회상 정보 입력. / MemoryInfoInputScreen.kt
9.회상 정보 데이터 확인. / MemoryInfoListScreen.kt
10.보호자 및 환자 정기알림. / Guardian_Alarm.kt
11.음성 목록,등록,자동 스크롤 기능. / Recode.kt, Recode.kt2
12.누적 카테고리별 통계와 월간 통계. / QuizStatsScreen.kt
13.환자 위치확인 기능과 Geofence 설정 및 벗어났을 시 보호자에게 푸시 알림 구현. / LocationScreen.kt



환자 쪽 기능 및 화면:
1.회원가입. / PatientSignInPage.kt 
2.로그인 및 자동 로그인 / Patient_Login.kt
3.joincode 연동. / Code2.kt
4.회상문장. / Patient_Sentence.kt
5.회상퀴즈. / Patient_Quiz.kt
6.긴급알림. / Patient_Alert.kt