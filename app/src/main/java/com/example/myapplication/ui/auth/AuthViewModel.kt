// app/src/main/java/com/example/myapplication/ui/auth/AuthViewModel.kt
package com.example.myapplication.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.network.SignupRequest
import com.example.myapplication.network.SignupResponse
import com.example.myapplication.network.VerifyResponse
import com.example.myapplication.network.PatientInfo
import com.example.myapplication.network.RetrofitInstance
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import retrofit2.Response

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class GuardianSignedUp(val joinCode: String?) : AuthState()
    data class PatientSignedUp(val uid: String) : AuthState()
    data class VerifiedRole(val role: String) : AuthState()
    data class PatientInfoGot(val info: PatientInfo) : AuthState()
    data class Error(val error: String) : AuthState()
}

class AuthViewModel : ViewModel() {
    private val _state = MutableStateFlow<AuthState>(AuthState.Idle)
    val state: StateFlow<AuthState> = _state

    fun signup(email: String, password: String, name: String, role: String) {
        _state.value = AuthState.Loading
        viewModelScope.launch {
            try {
                // ✅ 서버에 바로 요청만 보낸다 (Firebase X)
                val resp = RetrofitInstance.api.signup(SignupRequest(email, password, name, role))

                if (resp.isSuccessful && resp.body() != null) {
                    val body = resp.body()!!
                    if (role == "guardian") {
                        _state.value = AuthState.GuardianSignedUp(body.joinCode)
                    } else {
                        _state.value = AuthState.PatientSignedUp(body.uid)
                    }
                } else {
                    val err = resp.errorBody()?.string()
                    _state.value = AuthState.Error("서버 오류: ${resp.code()} / $err")
                }

            } catch (e: Exception) {
                _state.value = AuthState.Error(e.localizedMessage ?: "회원가입 실패")
            }
        }
    }


    /** 2) Firebase 로그인 */
    suspend fun firebaseLogin(email: String, password: String) {
        Firebase.auth.signInWithEmailAndPassword(email, password).await()
    }

    /** 3) 토큰 검증 → 역할 확인 */
    fun verifyToken() {
        _state.value = AuthState.Loading
        viewModelScope.launch {
            try {
                val resp: Response<VerifyResponse> = RetrofitInstance.api.verifyToken()
                if (resp.isSuccessful && resp.body() != null) {
                    _state.value = AuthState.VerifiedRole(resp.body()!!.role)
                } else {
                    _state.value = AuthState.Error("토큰 검증 실패: 코드 ${resp.code()}")
                }
            } catch (e: Exception) {
                _state.value = AuthState.Error(e.localizedMessage ?: "알 수 없는 오류")
            }
        }
    }

    /** 4) 보호된 환자 정보 조회 */
    fun getPatientInfo() {
        _state.value = AuthState.Loading
        viewModelScope.launch {
            try {
                val resp = RetrofitInstance.api.getPatient()
                if (resp.isSuccessful && resp.body() != null) {
                    _state.value = AuthState.PatientInfoGot(resp.body()!!)
                } else {
                    _state.value = AuthState.Error("조회 실패: 코드 ${resp.code()}")
                }
            } catch (e: Exception) {
                _state.value = AuthState.Error(e.localizedMessage ?: "알 수 없는 오류")
            }
        }
    }

    /** 상태 초기화 */
    fun resetState() {
        _state.value = AuthState.Idle
    }
}








