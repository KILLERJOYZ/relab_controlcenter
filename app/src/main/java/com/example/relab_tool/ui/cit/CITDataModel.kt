package com.example.relab_tool.ui.cit

enum class TestStatus {
    PASS,
    FAIL,
    SKIPPED,
    NOT_APPLICABLE,
    PENDING
}

data class TestResult(
    val id: String,
    val name: String,
    val status: TestStatus,
    val value: String? = null,
    val notes: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

data class CategoryResult(
    val categoryId: String,
    val categoryName: String,
    val tests: List<TestResult>
)

data class DeviceInfo(
    val model: String,
    val manufacturer: String,
    val androidVersion: String,
    val buildId: String
)

data class DiagnosticSession(
    val deviceInfo: DeviceInfo,
    val startedAt: Long,
    val completedAt: Long? = null,
    val categories: List<CategoryResult>
)

data class SubTestDef(
    val id: String,
    val name: String,
    val instruction: String,
    val isAutomated: Boolean,
    val checkSupport: (android.content.Context) -> Boolean = { true },
    val runAutomated: (suspend (android.content.Context) -> Pair<TestStatus, String?>)? = null
)
