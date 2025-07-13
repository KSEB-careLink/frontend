// app/src/main/java/com/example/myapplication/ui/auth/SignupGuardianActivity.kt
package com.example.myapplication.ui.auth

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.R
import kotlinx.coroutines.flow.collect

class SignupGuardianActivity : ComponentActivity() {

    // ViewModel 인스턴스 얻기
    private val authViewModel: AuthViewModel by viewModels()

    // 뷰 레퍼런스
    private lateinit var etName      : EditText   // ← 추가
    private lateinit var etEmail     : EditText
    private lateinit var etPassword  : EditText
    private lateinit var btnSignup   : Button
    private lateinit var progressBar : ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup_guardian)

        // ① 뷰 바인딩
        etName      = findViewById(R.id.etName)      // ← 레이아웃에 etName이 있어야 합니다
        etEmail     = findViewById(R.id.etEmail)
        etPassword  = findViewById(R.id.etPassword)
        btnSignup   = findViewById(R.id.btnSignup)
        progressBar = findViewById(R.id.progressBar)

        // ② 버튼 클릭 시 ViewModel 함수 호출
        btnSignup.setOnClickListener {
            val name  = etName.text.toString().trim()     // ← 이름 읽기
            val email = etEmail.text.toString().trim()
            val pw    = etPassword.text.toString().trim()

            // 유효성 검사
            when {
                name.isEmpty() -> {
                    Toast.makeText(this, "이름을 입력해주세요.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                email.isEmpty() || pw.isEmpty() -> {
                    Toast.makeText(this, "이메일과 비밀번호를 모두 입력해주세요.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                    Toast.makeText(this, "유효한 이메일을 입력해주세요.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            }

            // 공통 signup 호출 (name, role 포함)
            authViewModel.signup(
                name     = name,
                email    = email,
                password = pw,
                role     = "guardian"
            )
        }

        // ③ StateFlow 관찰하여 UI 업데이트
        lifecycleScope.launchWhenStarted {
            authViewModel.state.collect { state ->
                when (state) {
                    is AuthState.Loading -> {
                        etName.isEnabled      = false
                        etEmail.isEnabled     = false
                        etPassword.isEnabled  = false
                        btnSignup.isEnabled   = false
                        progressBar.visibility = View.VISIBLE
                    }
                    is AuthState.GuardianSignedUp -> {
                        progressBar.visibility = View.GONE
                        Toast.makeText(
                            this@SignupGuardianActivity,
                            state.message,
                            Toast.LENGTH_SHORT
                        ).show()
                        finish()
                    }
                    is AuthState.Error -> {
                        progressBar.visibility = View.GONE
                        etName.isEnabled      = true
                        etEmail.isEnabled     = true
                        etPassword.isEnabled  = true
                        btnSignup.isEnabled   = true
                        Toast.makeText(
                            this@SignupGuardianActivity,
                            "오류: ${state.error}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    else -> {
                        progressBar.visibility = View.GONE
                        etName.isEnabled      = true
                        etEmail.isEnabled     = true
                        etPassword.isEnabled  = true
                        btnSignup.isEnabled   = true
                    }
                }
            }
        }
    }
}

