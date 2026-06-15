package com.example.relab_tool.utils

/**
 * Non-localizable technical constants used across the app.
 * These are system property names, kernel scheduler/governor identifiers,
 * and other values that must NOT be translated.
 */
object DeviceConstants {
    // System Properties — SoC Detection
    const val PROP_SOC_MODEL = "ro.soc.model"
    const val PROP_SOC_PROCESS = "ro.soc.process"
    const val PROP_VENDOR_SOC_PROCESS = "vendor.soc.process"
    const val PROP_CPU_PROCESS = "ro.cpu.process"
    const val PROP_BOARD_PLATFORM = "ro.board.platform"
    const val PROP_CHIPNAME = "ro.chipname"
    const val PROP_HARDWARE = "ro.hardware"
    const val PROP_MEDIATEK_PLATFORM = "ro.mediatek.platform"

    // System Properties — Build Info
    const val PROP_BUILD_DESCRIPTION = "ro.build.description"
    const val PROP_BUILD_USER = "ro.build.user"
    const val PROP_BUILD_HOST = "ro.build.host"

    // System Properties — OS Skin Detection
    const val PROP_ONEUI = "ro.build.version.oneui"
    const val PROP_MIUI = "ro.miui.ui.version.name"
    const val PROP_HYPEROS = "ro.mi.os.version.name"
    const val PROP_COLOROS = "ro.build.version.opporom"
    const val PROP_REALME_UI = "ro.build.version.realmeui"
    const val PROP_OXYGENOS = "ro.build.version.ota"
    const val PROP_HARMONYOS = "hw_sc.build.platform.version"
    const val PROP_EMUI = "ro.build.version.emui"
    const val PROP_FUNTOUCHOS = "ro.vivo.os.build.display.id"
    const val PROP_ORIGINOS = "ro.vivo.product.overseas"
    const val PROP_NOTHING_OS = "ro.nothing.build"
    const val PROP_ROG_UI = "ro.build.asus.rog"
    const val PROP_ZENUI = "ro.build.asus.version"
    const val PROP_HELLO_UI = "ro.build.display.hello_id"
    const val PROP_MYUX = "ro.build.version.motoos"
    const val PROP_FLYME = "ro.build.display.id.flyme"
    const val PROP_BOS = "ro.config.knox"
    const val PROP_MAGICOS = "ro.build.version.magic"
    const val PROP_ZUI = "ro.com.google.gmsversion.zui"
}
