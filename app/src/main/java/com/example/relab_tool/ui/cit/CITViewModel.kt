package com.example.relab_tool.ui.cit

import android.os.Build
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject

@HiltViewModel
class CITViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _session = MutableStateFlow<DiagnosticSession>(loadOrCreateSession())
    val session: StateFlow<DiagnosticSession> = _session.asStateFlow()

    private fun loadOrCreateSession(): DiagnosticSession {
        val savedJson = savedStateHandle.get<String>("diagnostic_session")
        if (savedJson != null) {
            try {
                return deserializeSession(savedJson)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return createFreshSession()
    }

    private fun createFreshSession(): DiagnosticSession {
        val devInfo = DeviceInfo(
            model = Build.MODEL,
            manufacturer = Build.MANUFACTURER,
            androidVersion = Build.VERSION.RELEASE,
            buildId = Build.ID
        )

        val catsList = CITSubTestManager.categories.map { cat ->
            CategoryResult(
                categoryId = cat.id,
                categoryName = cat.name,
                tests = cat.subTests.map { test ->
                    TestResult(
                        id = test.id,
                        name = test.name,
                        status = TestStatus.PENDING,
                        value = null,
                        notes = null
                    )
                }
            )
        }

        return DiagnosticSession(
            deviceInfo = devInfo,
            startedAt = System.currentTimeMillis(),
            completedAt = null,
            categories = catsList
        )
    }

    fun updateSubTestResult(
        categoryId: String,
        testId: String,
        status: TestStatus,
        value: String? = null,
        notes: String? = null
    ) {
        val currentSession = _session.value
        val updatedCategories = currentSession.categories.map { cat ->
            if (cat.categoryId == categoryId) {
                val updatedTests = cat.tests.map { test ->
                    if (test.id == testId) {
                        test.copy(
                            status = status,
                            value = value,
                            notes = notes,
                            timestamp = System.currentTimeMillis()
                        )
                    } else {
                        test
                    }
                }
                cat.copy(tests = updatedTests)
            } else {
                cat
            }
        }

        // Check if all tests across all categories are completed (not pending)
        val allDone = updatedCategories.flatMap { it.tests }.none { it.status == TestStatus.PENDING }
        val completedTime = if (allDone) System.currentTimeMillis() else null

        val updatedSession = currentSession.copy(
            categories = updatedCategories,
            completedAt = completedTime
        )

        _session.value = updatedSession
        
        // Persist to SavedStateHandle
        try {
            savedStateHandle["diagnostic_session"] = serializeSession(updatedSession)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun resetSession() {
        val fresh = createFreshSession()
        _session.value = fresh
        try {
            savedStateHandle["diagnostic_session"] = serializeSession(fresh)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun serializeSession(session: DiagnosticSession): String {
        val json = JSONObject()
        
        val devJson = JSONObject()
        devJson.put("model", session.deviceInfo.model)
        devJson.put("manufacturer", session.deviceInfo.manufacturer)
        devJson.put("androidVersion", session.deviceInfo.androidVersion)
        devJson.put("buildId", session.deviceInfo.buildId)
        json.put("deviceInfo", devJson)
        
        json.put("startedAt", session.startedAt)
        json.put("completedAt", session.completedAt ?: -1L)
        
        val catsArray = JSONArray()
        session.categories.forEach { cat ->
            val catJson = JSONObject()
            catJson.put("categoryId", cat.categoryId)
            catJson.put("categoryName", cat.categoryName)
            
            val testsArray = JSONArray()
            cat.tests.forEach { test ->
                val testJson = JSONObject()
                testJson.put("id", test.id)
                testJson.put("name", test.name)
                testJson.put("status", test.status.name)
                testJson.put("value", test.value ?: JSONObject.NULL)
                testJson.put("notes", test.notes ?: JSONObject.NULL)
                testJson.put("timestamp", test.timestamp)
                testsArray.put(testJson)
            }
            catJson.put("tests", testsArray)
            catsArray.put(catJson)
        }
        json.put("categories", catsArray)
        
        return json.toString()
    }

    private fun deserializeSession(jsonStr: String): DiagnosticSession {
        val json = JSONObject(jsonStr)
        
        val devJson = json.getJSONObject("deviceInfo")
        val devInfo = DeviceInfo(
            model = devJson.getString("model"),
            manufacturer = devJson.getString("manufacturer"),
            androidVersion = devJson.getString("androidVersion"),
            buildId = devJson.getString("buildId")
        )
        
        val startedAt = json.getLong("startedAt")
        val completedAtVal = json.getLong("completedAt")
        val completedAt = if (completedAtVal == -1L) null else completedAtVal
        
        val catsList = mutableListOf<CategoryResult>()
        val catsArray = json.getJSONArray("categories")
        for (i in 0 until catsArray.length()) {
            val catJson = catsArray.getJSONObject(i)
            val catId = catJson.getString("categoryId")
            val catName = catJson.getString("categoryName")
            
            val testsList = mutableListOf<TestResult>()
            val testsArray = catJson.getJSONArray("tests")
            for (j in 0 until testsArray.length()) {
                val testJson = testsArray.getJSONObject(j)
                val testVal = if (testJson.isNull("value")) null else testJson.getString("value")
                val testNotes = if (testJson.isNull("notes")) null else testJson.getString("notes")
                
                testsList.add(
                    TestResult(
                        id = testJson.getString("id"),
                        name = testJson.getString("name"),
                        status = TestStatus.valueOf(testJson.getString("status")),
                        value = testVal,
                        notes = testNotes,
                        timestamp = testJson.getLong("timestamp")
                    )
                )
            }
            catsList.add(CategoryResult(catId, catName, testsList))
        }
        
        return DiagnosticSession(devInfo, startedAt, completedAt, catsList)
    }
}
