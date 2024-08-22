package com.its.nunkkam.android.managers                                      // 패키지 선언

import android.content.Context                                       // Android Context 클래스 임포트
import android.content.SharedPreferences                             // SharedPreferences 클래스 임포트

object UserManager {                                                 // UserManager 싱글톤 객체 선언
    private const val PREF_NAME = "UserPrefs"                        // SharedPreferences 이름 상수
    private const val USER_ID_KEY = "user_id"                        // 사용자 ID 키 상수
    private const val LOGIN_TYPE_KEY = "login_type"                  // 로그인 타입 키 상수
    const val LOGIN_TYPE_GOOGLE = "google"                           // Google 로그인 타입 상수
    private lateinit var sharedPreferences: SharedPreferences        // SharedPreferences 객체 선언

    var userId: String? = null                                       // 사용자 ID 변수 (외부에서 직접 설정 불가)
        private set
    var loginType: String? = null                                    // 로그인 타입 변수 (외부에서 직접 설정 불가)
        private set

    fun initialize(context: Context) {                               // UserManager 초기화 함수
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)  // SharedPreferences 초기화
        userId = sharedPreferences.getString(USER_ID_KEY, null)      // 저장된 사용자 ID 불러오기
        loginType = sharedPreferences.getString(LOGIN_TYPE_KEY, null)  // 저장된 로그인 타입 불러오기
    }

    fun setUser(userId: String, loginType: String) {                 // 사용자 정보 설정 함수
        UserManager.userId = userId                                         // 사용자 ID 설정
        UserManager.loginType = loginType                                   // 로그인 타입 설정

        with(sharedPreferences.edit()) {                             // SharedPreferences에 정보 저장
            putString(USER_ID_KEY, userId)                           // 사용자 ID 저장
            putString(LOGIN_TYPE_KEY, loginType)                     // 로그인 타입 저장
            apply()                                                  // 변경사항 적용
        }
    }

    fun clearUserData() {                                            // 사용자 데이터 초기화 함수
        userId = null                                                // 사용자 ID 초기화
        loginType = null                                             // 로그인 타입 초기화
        with(sharedPreferences.edit()) {                             // SharedPreferences 데이터 초기화
            clear()                                                  // 모든 데이터 삭제
            apply()                                                  // 변경사항 적용
        }
    }

    fun deleteUserData(context: Context) {                           // 사용자 데이터 삭제 함수
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)  // SharedPreferences 재초기화
        clearUserData()                                              // 사용자 데이터 초기화 함수 호출
    }

    // 현재 사용자 정보를 반환하는 메서드 추가
    fun getCurrentUser(): User? {
        return if (userId != null && loginType != null) {
            User(userId!!, loginType!!)
        } else {
            null
        }
    }

    // User 데이터 클래스를 추가하여 사용자 정보를 캡슐화
    data class User(val userId: String, val loginType: String)
}
