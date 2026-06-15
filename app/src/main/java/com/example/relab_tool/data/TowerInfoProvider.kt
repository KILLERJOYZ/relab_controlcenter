package com.example.relab_tool.data

import android.annotation.SuppressLint
import android.content.Context
import android.telephony.*

class TowerInfoProvider(private val context: Context) {

    data class TowerInfo(
        val id: String,
        val type: String,
        val signalStrength: Int,
        val isRegistered: Boolean,
        val bands: String? = null
    )

    @SuppressLint("MissingPermission")
    fun getTowerInfo(): List<TowerInfo> {
        return try {
            val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            val allInfo = tm.allCellInfo ?: return emptyList()
            
            allInfo.map { info ->
                when (info) {
                    is CellInfoLte -> {
                        val id = "CID: ${info.cellIdentity.ci}, PCI: ${info.cellIdentity.pci}"
                        TowerInfo(id, "LTE", info.cellSignalStrength.dbm, info.isRegistered)
                    }
                    is CellInfoGsm -> {
                        val id = "CID: ${info.cellIdentity.cid}, LAC: ${info.cellIdentity.lac}"
                        TowerInfo(id, "GSM", info.cellSignalStrength.dbm, info.isRegistered)
                    }
                    is CellInfoWcdma -> {
                        val id = "CID: ${info.cellIdentity.cid}, LAC: ${info.cellIdentity.lac}"
                        TowerInfo(id, "WCDMA", info.cellSignalStrength.dbm, info.isRegistered)
                    }
                    else -> {
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q && info is CellInfoNr) {
                            val identity = info.cellIdentity as CellIdentityNr
                            val id = "NCI: ${identity.nci}, PCI: ${identity.pci}"
                            TowerInfo(id, "NR (5G)", info.cellSignalStrength.dbm, info.isRegistered)
                        } else {
                            TowerInfo("Unknown", "Other", 0, info.isRegistered)
                        }
                    }
                }
            }
        } catch (_: Throwable) {
            // OPPO ColorOS and some OEMs throw SecurityException even with permission granted
            emptyList()
        }
    }
}
