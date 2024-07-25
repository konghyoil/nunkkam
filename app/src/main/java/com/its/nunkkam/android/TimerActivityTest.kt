package com.its.nunkkam.android

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import com.google.firebase.firestore.FirebaseFirestore
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TimerActivityTest {

    @get:Rule
    var activityScenarioRule = ActivityScenarioRule(TimerActivity::class.java)

    @Test
    fun testTimerStartAndPause() {
        // 시작 버튼 클릭
        onView(withId(R.id.start_button)).perform(click())
        // 2초 대기
        Thread.sleep(2000)
        // 일시정지 버튼 클릭
        onView(withId(R.id.pause_button)).perform(click())
        // 재시작 버튼 확인
        onView(withId(R.id.restart_button)).check(matches(isDisplayed()))
        // 일시정지 버튼 확인
        onView(withId(R.id.pause_button)).check(matches(withEffectiveVisibility(Visibility.GONE)))
        // 재시작 버튼 클릭
        onView(withId(R.id.restart_button)).perform(click())
        // 2초 더 대기
        Thread.sleep(2000)
        // 일시정지 버튼 클릭
        onView(withId(R.id.pause_button)).perform(click())
        // 총 경과 시간 확인
        onView(withId(R.id.timer_text)).check(matches(isDisplayed()))
    }

    @Test
    fun testSaveMeasurementData() {
        // Firestore 에뮬레이터를 사용하여 테스트
        val db = FirebaseFirestore.getInstance()
        val testUserId = "test_user_id"
        val userDocument = db.collection("users").document(testUserId)

        // 시작 버튼 클릭
        onView(withId(R.id.start_button)).perform(click())
        // 5초 대기
        Thread.sleep(5000)
        // 일시정지 버튼 클릭
        onView(withId(R.id.pause_button)).perform(click())
        // 결과 확인 버튼 클릭
        onView(withId(R.id.result_button)).perform(click())

        // Firestore 데이터 확인
        userDocument.get().addOnSuccessListener { document ->
            val blinks = document.get("blinks") as List<Map<String, Any>>?
            assertNotNull(blinks)
            if (blinks != null && blinks.isNotEmpty()) {
                val lastBlink = blinks.last()
                val measurementTime = lastBlink["measurement_time"] as Long
                assert(measurementTime >= 5)
                assertNotNull(lastBlink["measurement_date"])
            }
        }.addOnFailureListener {
            assert(false) // 데이터 가져오기 실패
        }
    }
}
