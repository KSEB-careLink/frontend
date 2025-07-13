// app/src/main/java/com/example/myapplication/ui/auth/AuthViewModel.kt
package com.example.myapplication.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.network.PatientInfo
import com.example.myapplication.network.RetrofitInstance
import com.example.myapplication.network.SignupRequest    // ← 추가
import com.example.myapplication.network.SignupResponse   // ← 이미 추가돼 있음
import com.example.myapplication.network.VerifyResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import retrofit2.Response

sealed class AuthState {
    object Idle                : AuthState()
    object Loading             : AuthState()
    data class GuardianSignedUp(val message: String = "보호자 가입 완료") : AuthState()
    data class PatientSignedUp (val message: String = "환자 가입 완료")   : AuthState()
    data class VerifiedRole    (val role: String)                       : AuthState()
    data class PatientInfoGot  (val info: PatientInfo)                 : AuthState()
    data class Error           (val error: String)                     : AuthState()
}

class AuthViewModel : ViewModel() {

    private val _state = MutableStateFlow<AuthState>(AuthState.Idle)
    val state: StateFlow<AuthState> = _state

    /**
     * 공통 회원가입
     * @param role "guardian" 또는 "patient"
     */
    fun signup(name: String, email: String, password: String, role: String) {
        _state.value = AuthState.Loading
        viewModelScope.launch {
            try {
                // 3) SignupRequest 로 JSON 바디 생성 후 호출
                val request = SignupRequest(
                    name     = name,
                    email    = email,
                    password = password,
                    role     = role
                )
                val resp: Response<SignupResponse> =
                    RetrofitInstance.api.signup(request)

                if (resp.isSuccessful && resp.body() != null) {
                    if (role == "guardian") {
                        _state.value = AuthState.GuardianSignedUp()
                    } else {
                        _state.value = AuthState.PatientSignedUp()
                    }
                } else {
                    _state.value = AuthState.Error("Code ${resp.code()}")
                }
            } catch (e: Exception) {
                _state.value = AuthState.Error(e.localizedMessage ?: "Unknown error")
            }
        }
    }

    fun verifyToken() {
        _state.value = AuthState.Loading
        viewModelScope.launch {
            try {
                val resp: Response<VerifyResponse> = RetrofitInstance.api.verifyToken()
                if (resp.isSuccessful && resp.body() != null) {
                    _state.value = AuthState.VerifiedRole(resp.body()!!.role)
                } else {
                    _state.value = AuthState.Error("Code ${resp.code()}")
                }
            } catch (e: Exception) {
                _state.value = AuthState.Error(e.localizedMessage ?: "Unknown error")
            }
        }
    }

    fun getPatientInfo() {
        _state.value = AuthState.Loading
        viewModelScope.launch {
            try {
                val resp: Response<PatientInfo> = RetrofitInstance.api.getPatient()
                if (resp.isSuccessful && resp.body() != null) {
                    _state.value = AuthState.PatientInfoGot(resp.body()!!)
                } else {
                    _state.value = AuthState.Error("Code ${resp.code()}")
                }
            } catch (e: Exception) {
                _state.value = AuthState.Error(e.localizedMessage ?: "Unknown error")
            }
        }
    }

    fun resetState() {
        _state.value = AuthState.Idle
    }
}




