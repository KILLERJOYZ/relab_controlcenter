import com.example.relab_tool.data.BatteryHistoryPolicy
import com.example.relab_tool.utils.RootUtils

fun main() {
    val day = BatteryHistoryPolicy.RETENTION_MS
    val history = listOf(Triple(day - 60_000, 80, false), Triple(day + 60_000, 79, false))
    check(BatteryHistoryPolicy.retained(history, day + 60_000) == history)
    check(BatteryHistoryPolicy.retained(listOf(Triple(0L, 80, false), Triple(day * 3, -1, false), Triple(day * 3 + 1, 50, false)), day * 3).isEmpty())
    val discharge = listOf(Triple(0L, 100, false), Triple(3_600_000L, 50, false), Triple(7_200_000L, 90, true), Triple(10_800_000L, 90, false), Triple(14_400_000L, 80, false))
    check(BatteryHistoryPolicy.dischargeRate(discharge) == 10f)
    check(BatteryHistoryPolicy.dischargeRate(discharge + Triple(15_000_000L, 85, true)) == 0f)
    check(!RootUtils.isRootAvailable())
    check(!RootUtils.setChargeLimit(80))
    check(!RootUtils.setChargeLimit(-1))
    check(!RootUtils.setGovernor("performance"))
    check(!RootUtils.runShellCommand("exit 0"))
    println("PASS: 9 checks against production battery-history and fail-closed root code")
}
