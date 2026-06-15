package com.example.relab_tool.utils

import android.content.Context
import android.os.Build
import android.os.CancellationSignal
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintResultCallbackBypass
import android.webkit.WebView
import android.webkit.WebViewClient
import com.example.relab_tool.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.coroutines.resume

object ReportGenerator {

    suspend fun generateHardwareReport(
        context: Context,
        summary: DeviceSummary?,
        system: SystemInfo?,
        cpu: CpuInfo?,
        battery: BatteryInfo?,
        display: DisplayInfo?,
        memory: MemoryInfo?,
        soc: SocInfo?,
        cameras: List<CameraInfo>,
        bluetooth: BluetoothInfo?,
        network: NetworkInfo?,
        audio: AudioInfo?,
        security: SecurityInfo?
    ): File? = withContext(Dispatchers.Main) {
        suspendCancellableCoroutine { continuation ->
            val webView = WebView(context)
            webView.settings.blockNetworkLoads = true
            
            // Find activity and its root view group to attach WebView to window hierarchy.
            // This is required on Android 10+ so the layout pass resolves instantly instead of hanging/failing in headless mode.
            val activity = findActivity(context)
            val rootView = activity?.window?.decorView as? android.view.ViewGroup
            
            webView.visibility = android.view.View.INVISIBLE
            rootView?.addView(webView, android.view.ViewGroup.LayoutParams(1, 1))

            fun cleanup() {
                rootView?.removeView(webView)
                webView.destroy()
            }
            
            continuation.invokeOnCancellation {
                cleanup()
            }
            
            webView.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    webView.post {
                        val fileName = "Relab_Report_${System.currentTimeMillis()}.pdf"
                        val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName)
                        
                        val printAdapter = webView.createPrintDocumentAdapter("Relab Hardware Report")
                        val printAttributes = PrintAttributes.Builder()
                            .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                            .setResolution(PrintAttributes.Resolution("pdf", "pdf", 600, 600))
                            .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                            .build()

                        val layoutCallback = PrintResultCallbackBypass.createLayoutCallback(object : PrintResultCallbackBypass.LayoutCallbackImpl {
                            override fun onLayoutFinished(info: PrintDocumentInfo?, changed: Boolean) {
                                val pfd: ParcelFileDescriptor
                                try {
                                    pfd = ParcelFileDescriptor.open(
                                        file,
                                        ParcelFileDescriptor.MODE_READ_WRITE or ParcelFileDescriptor.MODE_CREATE or ParcelFileDescriptor.MODE_TRUNCATE
                                    )
                                } catch (e: Exception) {
                                    cleanup()
                                    if (continuation.isActive) continuation.resume(null)
                                    return
                                }

                                val writeCallback = PrintResultCallbackBypass.createWriteCallback(object : PrintResultCallbackBypass.WriteCallbackImpl {
                                    override fun onWriteFinished(pages: Array<PageRange>?) {
                                        try {
                                            pfd.close()
                                        } catch (e: Exception) {}
                                        cleanup()
                                        if (continuation.isActive) continuation.resume(file)
                                    }

                                    override fun onWriteFailed(error: CharSequence?) {
                                        try {
                                            pfd.close()
                                        } catch (e: Exception) {}
                                        cleanup()
                                        if (continuation.isActive) continuation.resume(null)
                                    }

                                    override fun onWriteCancelled() {
                                        try {
                                            pfd.close()
                                        } catch (e: Exception) {}
                                        cleanup()
                                        if (continuation.isActive) continuation.resume(null)
                                    }
                                })

                                printAdapter.onWrite(
                                    arrayOf(PageRange.ALL_PAGES),
                                    pfd,
                                    CancellationSignal(),
                                    writeCallback
                                )
                            }

                            override fun onLayoutFailed(error: CharSequence?) {
                                cleanup()
                                if (continuation.isActive) continuation.resume(null)
                            }

                            override fun onLayoutCancelled() {
                                cleanup()
                                if (continuation.isActive) continuation.resume(null)
                            }
                        })

                        printAdapter.onLayout(
                            null,
                            printAttributes,
                            null,
                            layoutCallback,
                            null
                        )
                    }
                }
            }
            
            val html = generateHtmlReport(
                summary, system, cpu, battery, display, memory, soc, cameras, bluetooth, network, audio, security
            )
            webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
        }
    }



    private fun generateHtmlReport(
        summary: DeviceSummary?,
        system: SystemInfo?,
        cpu: CpuInfo?,
        battery: BatteryInfo?,
        display: DisplayInfo?,
        memory: MemoryInfo?,
        soc: SocInfo?,
        cameras: List<CameraInfo>,
        bluetooth: BluetoothInfo?,
        network: NetworkInfo?,
        audio: AudioInfo?,
        security: SecurityInfo?
    ): String {
        val date = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        val deviceModel = summary?.model ?: system?.model ?: "Unknown Device"
        val deviceManufacturer = summary?.manufacturer ?: system?.manufacturer ?: "Unknown"

        val isRooted = security?.rootAccess == "Yes" || security?.rootAccess == "true" || (system?.buildType ?: "").contains("test-keys")
        val rootStatusText = if (isRooted) "Rooted (Warning)" else "Secured (No Root)"
        val rootClass = if (isRooted) "badge warning" else "badge success"

        // Check storage percent
        val totalBytes = memory?.internalTotalBytes ?: 0L
        val freeBytes = memory?.internalFreeBytes ?: 0L
        val usedBytes = totalBytes - freeBytes
        val storagePercent = if (totalBytes > 0) ((usedBytes * 100) / totalBytes).toInt() else 0
        val storageUsedStr = memory?.internalUsed ?: "0 GB"
        val storageTotalStr = memory?.internalTotal ?: "0 GB"

        return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="utf-8">
            <title>Hardware Diagnostic Report - $deviceModel</title>
            <style>
                @page {
                    size: A4;
                    margin: 15mm 15mm 15mm 15mm;
                }
                body {
                    font-family: 'Inter', -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
                    color: #1F2937;
                    background-color: #FFFFFF;
                    margin: 0;
                    padding: 0;
                    line-height: 1.5;
                    font-size: 12px;
                    -webkit-print-color-adjust: exact;
                }
                /* Header */
                .header-banner {
                    background: linear-gradient(135deg, #1E1B4B 0%, #312E81 100%);
                    color: #FFFFFF;
                    padding: 24px;
                    border-radius: 12px;
                    margin-bottom: 24px;
                    position: relative;
                    overflow: hidden;
                }
                .header-banner::after {
                    content: '';
                    position: absolute;
                    top: 0; right: 0; bottom: 0; left: 0;
                    background: radial-gradient(circle at top right, rgba(99, 102, 241, 0.15), transparent 60%);
                }
                .header-title {
                    font-size: 10px;
                    text-transform: uppercase;
                    letter-spacing: 1.5px;
                    font-weight: 700;
                    color: #A5B4FC;
                    margin: 0 0 6px 0;
                }
                .device-model {
                    font-size: 26px;
                    font-weight: 800;
                    margin: 0;
                    letter-spacing: -0.5px;
                }
                .device-manufacturer {
                    font-size: 14px;
                    color: #C7D2FE;
                    font-weight: 500;
                    margin: 2px 0 12px 0;
                }
                .meta-grid {
                    display: flex;
                    gap: 24px;
                    font-size: 11px;
                    color: #E0E7FF;
                    border-top: 1px solid rgba(255, 255, 255, 0.1);
                    padding-top: 12px;
                    margin-top: 12px;
                }
                .meta-item strong {
                    color: #FFFFFF;
                }
                
                /* Hero Cards */
                .hero-container {
                    display: grid;
                    grid-template-columns: repeat(4, 1fr);
                    gap: 12px;
                    margin-bottom: 24px;
                }
                .hero-card {
                    background: #F9FAFB;
                    border: 1px solid #F3F4F6;
                    border-radius: 8px;
                    padding: 14px;
                    box-sizing: border-box;
                }
                .hero-label {
                    font-size: 9px;
                    text-transform: uppercase;
                    color: #6B7280;
                    font-weight: 600;
                    letter-spacing: 0.5px;
                    margin-bottom: 6px;
                }
                .hero-value {
                    font-size: 13px;
                    font-weight: 700;
                    color: #111827;
                    word-break: break-word;
                }
                .hero-subtext {
                    font-size: 10px;
                    color: #9CA3AF;
                    margin-top: 4px;
                }

                /* Section styling */
                .section-container {
                    margin-bottom: 24px;
                    page-break-inside: avoid;
                }
                .section-header {
                    display: flex;
                    align-items: center;
                    gap: 8px;
                    border-bottom: 2px solid #E5E7EB;
                    padding-bottom: 8px;
                    margin-bottom: 12px;
                }
                .section-header svg {
                    color: #4F46E5;
                }
                .section-title {
                    font-size: 14px;
                    font-weight: 700;
                    color: #111827;
                    margin: 0;
                }
                
                /* Grids for Specs */
                .spec-grid {
                    display: grid;
                    grid-template-columns: repeat(2, 1fr);
                    gap: 8px 24px;
                    padding: 4px 0;
                }
                .spec-item {
                    display: flex;
                    justify-content: space-between;
                    align-items: center;
                    border-bottom: 1px solid #F3F4F6;
                    padding-bottom: 6px;
                    padding-top: 4px;
                }
                .spec-item.full-width {
                    grid-column: span 2;
                }
                .spec-label {
                    font-size: 12px;
                    color: #4B5563;
                    font-weight: 500;
                }
                .spec-value {
                    font-size: 12px;
                    color: #111827;
                    font-weight: 600;
                    text-align: right;
                    word-break: break-all;
                }

                /* Badges */
                .badge {
                    display: inline-block;
                    padding: 2px 8px;
                    border-radius: 4px;
                    font-size: 9px;
                    font-weight: 700;
                    text-transform: uppercase;
                }
                .badge.success {
                    background-color: #D1FAE5;
                    color: #065F46;
                }
                .badge.warning {
                    background-color: #FEE2E2;
                    color: #991B1B;
                }
                .badge.neutral {
                    background-color: #F3F4F6;
                    color: #374151;
                }

                /* Progress Bar */
                .progress-container {
                    display: flex;
                    align-items: center;
                    gap: 8px;
                    width: 150px;
                }
                .progress-bar {
                    flex-grow: 1;
                    background: #E5E7EB;
                    border-radius: 4px;
                    height: 6px;
                    overflow: hidden;
                }
                .progress-fill {
                    background: #4F46E5;
                    height: 100%;
                    border-radius: 4px;
                }
                .progress-text {
                    font-size: 9px;
                    font-weight: 700;
                    color: #4B5563;
                    min-width: 28px;
                    text-align: right;
                }

                /* Cameras Table */
                .cameras-container {
                    display: grid;
                    grid-template-columns: repeat(2, 1fr);
                    gap: 12px;
                }
                .camera-card {
                    background: #F9FAFB;
                    border: 1px solid #F3F4F6;
                    border-radius: 8px;
                    padding: 12px;
                    page-break-inside: avoid;
                }
                .camera-title {
                    font-size: 11px;
                    font-weight: 700;
                    color: #111827;
                    margin: 0 0 6px 0;
                    border-bottom: 1px solid #E5E7EB;
                    padding-bottom: 4px;
                    display: flex;
                    justify-content: space-between;
                }
                .camera-grid {
                    display: grid;
                    grid-template-columns: repeat(2, 1fr);
                    gap: 4px 12px;
                }
                .camera-prop {
                    font-size: 10px;
                    color: #4B5563;
                }
                .camera-val {
                    font-size: 10px;
                    font-weight: 600;
                    color: #111827;
                    text-align: right;
                }

                /* Footer */
                .footer {
                    margin-top: 30px;
                    border-top: 1px solid #E5E7EB;
                    padding-top: 12px;
                    text-align: center;
                    font-size: 10px;
                    color: #9CA3AF;
                    page-break-after: avoid;
                }
            </style>
        </head>
        <body>
            <!-- Header -->
            <div class="header-banner">
                <div class="header-title">Relab Diagnostic Center</div>
                <div class="device-model">${deviceModel.escape()}</div>
                <div class="device-manufacturer">${deviceManufacturer.escape()}</div>
                <div class="meta-grid">
                    <div class="meta-item">Report ID: <strong>#${System.currentTimeMillis()}</strong></div>
                    <div class="meta-item">Date: <strong>${date.escape()}</strong></div>
                    <div class="meta-item">Integrity: <span class="${rootClass}">${rootStatusText}</span></div>
                </div>
            </div>

            <!-- Hero section -->
            <div class="hero-container">
                <div class="hero-card">
                    <div class="hero-label">Processor</div>
                    <div class="hero-value">${(soc?.processor ?: cpu?.processor ?: "Unknown").escape()}</div>
                    <div class="hero-subtext">${(soc?.gpu ?: "Unknown GPU").escape()}</div>
                </div>
                <div class="hero-card">
                    <div class="hero-label">RAM Memory</div>
                    <div class="hero-value">${memory?.totalRam ?: "Unknown"}</div>
                    <div class="hero-subtext">${memory?.ramType ?: "LPDDR"} / Free: ${memory?.availableRam ?: "N/A"}</div>
                </div>
                <div class="hero-card">
                    <div class="hero-label">Battery Health</div>
                    <div class="hero-value">${battery?.health ?: "Good"}</div>
                    <div class="hero-subtext">${battery?.levelString ?: "N/A"} / Cap: ${battery?.actualCapacity ?: battery?.capacity ?: "N/A"}</div>
                </div>
                <div class="hero-card">
                    <div class="hero-label">OS Version</div>
                    <div class="hero-value">Android ${system?.androidVersion ?: Build.VERSION.RELEASE}</div>
                    <div class="hero-subtext">Patch: ${system?.securityPatch ?: "Unknown"}</div>
                </div>
            </div>

            <!-- Section 1: System Details -->
            <div class="section-container">
                <div class="section-header">
                    <!-- System Icon -->
                    <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2"><rect x="5" y="2" width="14" height="20" rx="2" ry="2"/><path d="M12 18h.01"/></svg>
                    <div class="section-title">System & Platform</div>
                </div>
                <div class="spec-grid">
                    <div class="spec-item">
                        <div class="spec-label">Device Brand</div>
                        <div class="spec-value">${(system?.brand ?: summary?.manufacturer ?: "Unknown").escape()}</div>
                    </div>
                    <div class="spec-item">
                        <div class="spec-label">Board Platform</div>
                        <div class="spec-value">${(system?.board ?: summary?.board ?: "Unknown").escape()}</div>
                    </div>
                    <div class="spec-item">
                        <div class="spec-label">Kernel Version</div>
                        <div class="spec-value">${(system?.kernel ?: "Unknown").escape()}</div>
                    </div>
                    <div class="spec-item">
                        <div class="spec-label">Build ID</div>
                        <div class="spec-value">${(system?.buildId ?: "Unknown").escape()}</div>
                    </div>
                    <div class="spec-item">
                        <div class="spec-label">Android ID</div>
                        <div class="spec-value">${(summary?.androidId ?: "Unknown").escape()}</div>
                    </div>
                    <div class="spec-item">
                        <div class="spec-label">Google Play Services</div>
                        <div class="spec-value">${(system?.googlePlayServices ?: "Unknown").escape()}</div>
                    </div>
                    <div class="spec-item">
                        <div class="spec-label">System Uptime</div>
                        <div class="spec-value">${(system?.uptime ?: "Unknown").escape()}</div>
                    </div>
                    <div class="spec-item">
                        <div class="spec-label">SDK Level</div>
                        <div class="spec-value">API ${(system?.sdkLevel ?: Build.VERSION.SDK_INT).toString().escape()}</div>
                    </div>
                    <div class="spec-item full-width">
                        <div class="spec-label">Build Fingerprint</div>
                        <div class="spec-value">${(system?.fingerprint ?: summary?.buildFingerprint ?: "Unknown").escape()}</div>
                    </div>
                </div>
            </div>

            <!-- Section 2: Processor & GPU -->
            <div class="section-container">
                <div class="section-header">
                    <!-- Processor Icon -->
                    <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2"><rect x="4" y="4" width="16" height="16" rx="2"/><path d="M9 9h6v6H9zM9 1v3M15 1v3M9 20v3M15 20v3M20 9h3M20 15h3M1 9h3M1 15h3"/></svg>
                    <div class="section-title">Processor (SoC) & GPU Details</div>
                </div>
                <div class="spec-grid">
                    <div class="spec-item">
                        <div class="spec-label">SoC Model</div>
                        <div class="spec-value">${(soc?.processor ?: cpu?.processor ?: "Unknown").escape()}</div>
                    </div>
                    <div class="spec-item">
                        <div class="spec-label">Manufacturing Process</div>
                        <div class="spec-value">${(soc?.process ?: "N/A").escape()}</div>
                    </div>
                    <div class="spec-item">
                        <div class="spec-label">Cores Count</div>
                        <div class="spec-value">${(soc?.cores ?: cpu?.cores?.toString() ?: "N/A").escape()}</div>
                    </div>
                    <div class="spec-item">
                        <div class="spec-label">CPU Architecture</div>
                        <div class="spec-value">${(soc?.architecture ?: cpu?.architecture ?: "Unknown").escape()}</div>
                    </div>
                    <div class="spec-item">
                        <div class="spec-label">CPU Governor</div>
                        <div class="spec-value">${(soc?.governor ?: cpu?.cpuGovernor ?: "N/A").escape()}</div>
                    </div>
                    <div class="spec-item">
                        <div class="spec-label">Supported ABIs</div>
                        <div class="spec-value">${(soc?.abi ?: cpu?.supportedAbis ?: "N/A").escape()}</div>
                    </div>
                    <div class="spec-item">
                        <div class="spec-label">GPU Model</div>
                        <div class="spec-value">${(soc?.gpu ?: "Unknown").escape()}</div>
                    </div>
                    <div class="spec-item">
                        <div class="spec-label">GPU Vendor</div>
                        <div class="spec-value">${(soc?.gpuVendor ?: "Unknown").escape()}</div>
                    </div>
                    <div class="spec-item">
                        <div class="spec-label">Vulkan Version</div>
                        <div class="spec-value">${(soc?.vulkanVersion ?: "N/A").escape()}</div>
                    </div>
                    <div class="spec-item">
                        <div class="spec-label">OpenGL ES Version</div>
                        <div class="spec-value">${(soc?.openGlEs ?: system?.openGlEs ?: "N/A").escape()}</div>
                    </div>
                </div>
            </div>

            <!-- Section 3: Display & Graphics -->
            <div class="section-container">
                <div class="section-header">
                    <!-- Display Icon -->
                    <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2"><rect x="2" y="3" width="20" height="14" rx="2" ry="2"/><path d="M8 21h8M12 17v4"/></svg>
                    <div class="section-title">Display & Graphics</div>
                </div>
                <div class="spec-grid">
                    <div class="spec-item">
                        <div class="spec-label">Resolution</div>
                        <div class="spec-value">${(display?.currentResolution ?: summary?.resolution ?: "Unknown").escape()}</div>
                    </div>
                    <div class="spec-item">
                        <div class="spec-label">Refresh Rate</div>
                        <div class="spec-value">${(display?.currentRefreshRate ?: "Unknown").escape()}</div>
                    </div>
                    <div class="spec-item">
                        <div class="spec-label">Physical Screen Size</div>
                        <div class="spec-value">${(display?.physicalSize ?: display?.diagonal ?: "Unknown").escape()}</div>
                    </div>
                    <div class="spec-item">
                        <div class="spec-label">Screen Density</div>
                        <div class="spec-value">${(display?.density ?: "N/A").escape()}</div>
                    </div>
                    <div class="spec-item">
                        <div class="spec-label">Pixel Density (PPI)</div>
                        <div class="spec-value">${(display?.ppi ?: "N/A").escape()}</div>
                    </div>
                    <div class="spec-item">
                        <div class="spec-label">Wide Gamut Color</div>
                        <div class="spec-value">
                            <span class="badge ${if (display?.wideColorGamut == true) "success" else "neutral"}">
                                ${if (display?.wideColorGamut == true) "Supported" else "N/A"}
                            </span>
                        </div>
                    </div>
                    <div class="spec-item">
                        <div class="spec-label">HDR Support</div>
                        <div class="spec-value">${(display?.hdrSupport ?: "N/A").escape()}</div>
                    </div>
                    <div class="spec-item">
                        <div class="spec-label">Widevine DRM Level</div>
                        <div class="spec-value">${(display?.widevineLevel ?: "N/A").escape()}</div>
                    </div>
                </div>
            </div>

            <!-- Section 4: Memory & Storage -->
            <div class="section-container">
                <div class="section-header">
                    <!-- Memory Icon -->
                    <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 12h-4l-3 9L9 3l-3 9H2"/></svg>
                    <div class="section-title">Memory & Internal Storage</div>
                </div>
                <div class="spec-grid">
                    <div class="spec-item">
                        <div class="spec-label">Total Installed RAM</div>
                        <div class="spec-value">${(memory?.totalRam ?: "Unknown").escape()}</div>
                    </div>
                    <div class="spec-item">
                        <div class="spec-label">RAM Configuration Type</div>
                        <div class="spec-value">${(memory?.ramType ?: summary?.ramType ?: "N/A").escape()}</div>
                    </div>
                    <div class="spec-item">
                        <div class="spec-label">Available Free RAM</div>
                        <div class="spec-value">${(memory?.availableRam ?: "N/A").escape()}</div>
                    </div>
                    <div class="spec-item">
                        <div class="spec-label">Internal Storage FS Type</div>
                        <div class="spec-value">${(memory?.internalFsType ?: "N/A").escape()}</div>
                    </div>
                    <div class="spec-item">
                        <div class="spec-label">Internal Storage Status</div>
                        <div class="spec-value">
                            <div class="progress-container">
                                <div class="progress-bar">
                                    <div class="progress-fill" style="width: ${storagePercent}%;"></div>
                                </div>
                                <div class="progress-text">${storagePercent}%</div>
                            </div>
                        </div>
                    </div>
                    <div class="spec-item">
                        <div class="spec-label">Storage Capacity (Used / Total)</div>
                        <div class="spec-value">${storageUsedStr.escape()} / ${storageTotalStr.escape()}</div>
                    </div>
                </div>
            </div>

            <!-- Section 5: Cameras -->
            <div class="section-container">
                <div class="section-header">
                    <!-- Cameras Icon -->
                    <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2"><path d="M23 19a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h4l2-3h6l2 3h4a2 2 0 0 1 2 2z"/><circle cx="12" cy="13" r="4"/></svg>
                    <div class="section-title">Camera Sensors</div>
                </div>
                <div class="cameras-container">
                    ${if (cameras.isEmpty()) "<div class='spec-item full-width'><div class='spec-label'>No Camera details found.</div></div>" else cameras.joinToString("") { camera ->
                        """
                        <div class="camera-card">
                            <div class="camera-title">
                                <span>Camera ID: ${camera.id}</span>
                                <span class="badge neutral">${camera.facing}</span>
                            </div>
                            <div class="camera-grid">
                                <div class="camera-prop">Resolution</div>
                                <div class="camera-val">${camera.resolution} (${camera.physicalMP} MP)</div>
                                
                                <div class="camera-prop">Aperture</div>
                                <div class="camera-val">f/${camera.aperture}</div>
                                
                                <div class="camera-prop">Focal Length</div>
                                <div class="camera-val">${camera.focalLength}mm (35mm equiv: ${camera.focalLength35mm}mm)</div>
                                
                                <div class="camera-prop">Sensor Size</div>
                                <div class="camera-val">${camera.sensorSize} (${camera.diagonal} diag)</div>
                                
                                <div class="camera-prop">Pixel Size</div>
                                <div class="camera-val">${camera.pixelSize}</div>
                                
                                <div class="camera-prop">Zoom Range</div>
                                <div class="camera-val">${camera.zoom}</div>
                                
                                <div class="camera-prop">OIS Stabilization</div>
                                <div class="camera-val">${if (camera.opticalStabilization) "Supported" else "No"}</div>
                                
                                <div class="camera-prop">Video EIS</div>
                                <div class="camera-val">${if (camera.videoStabilization) "Supported" else "No"}</div>
                            </div>
                        </div>
                        """
                    }}
                </div>
            </div>

            <!-- Section 6: Connectivity -->
            <div class="section-container">
                <div class="section-header">
                    <!-- Connectivity Icon -->
                    <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2"><path d="M5 12.55a11 11 0 0 1 14.08 0M1.42 9a16 16 0 0 1 21.16 0M8.53 16.11a6 6 0 0 1 6.95 0M12 20h.01"/></svg>
                    <div class="section-title">Connectivity & Network</div>
                </div>
                <div class="spec-grid">
                    <div class="spec-item">
                        <div class="spec-label">Wi-Fi Standard</div>
                        <div class="spec-value">${(network?.wifiStandard ?: network?.standard ?: "N/A").escape()}</div>
                    </div>
                    <div class="spec-item">
                        <div class="spec-label">Connected Wi-Fi SSID</div>
                        <div class="spec-value">${(network?.wifiSsid ?: "Not Connected").escape()}</div>
                    </div>
                    <div class="spec-item">
                        <div class="spec-label">Wi-Fi Frequency</div>
                        <div class="spec-value">${(network?.frequency ?: "N/A").escape()}</div>
                    </div>
                    <div class="spec-item">
                        <div class="spec-label">Bluetooth Standard</div>
                        <div class="spec-value">${(bluetooth?.version ?: system?.bluetoothVersion ?: "Unknown").escape()}</div>
                    </div>
                    <div class="spec-item">
                        <div class="spec-label">Bluetooth Status</div>
                        <div class="spec-value">${(bluetooth?.state ?: "Disabled").escape()}</div>
                    </div>
                    <div class="spec-item">
                        <div class="spec-label">Active SIM Card Carrier</div>
                        <div class="spec-value">${(network?.cellularInfo?.operator ?: network?.cellularInfo?.simInfos?.firstOrNull()?.carrier ?: "No SIM").escape()}</div>
                    </div>
                    <div class="spec-item">
                        <div class="spec-label">Mobile Network Type</div>
                        <div class="spec-value">${(network?.cellularInfo?.networkType ?: network?.networkType ?: "N/A").escape()}</div>
                    </div>
                    <div class="spec-item">
                        <div class="spec-label">Network Link Speed</div>
                        <div class="spec-value">${(network?.linkSpeed ?: "N/A").escape()}</div>
                    </div>
                </div>
            </div>

            <!-- Section 7: Security & Device Integrity -->
            <div class="section-container">
                <div class="section-header">
                    <!-- Security Icon -->
                    <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/></svg>
                    <div class="section-title">Security & Device Integrity</div>
                </div>
                <div class="spec-grid">
                    <div class="spec-item">
                        <div class="spec-label">Bootloader Lock Status</div>
                        <div class="spec-value">
                            <span class="badge ${if (security?.bootloaderStatus == "Locked") "success" else "warning"}">
                                ${(security?.bootloaderStatus ?: system?.bootloader ?: "Unknown").escape()}
                            </span>
                        </div>
                    </div>
                    <div class="spec-item">
                        <div class="spec-label">SELinux Status Mode</div>
                        <div class="spec-value">
                            <span class="badge ${if (security?.selinuxStatus == "Enforcing") "success" else "warning"}">
                                ${(security?.selinuxStatus ?: "Unknown").escape()}
                            </span>
                        </div>
                    </div>
                    <div class="spec-item">
                        <div class="spec-label">Root Access Status</div>
                        <div class="spec-value">
                            <span class="badge ${if (isRooted) "warning" else "success"}">
                                ${(security?.rootAccess ?: if (isRooted) "Rooted" else "Secured").escape()}
                            </span>
                        </div>
                    </div>
                    <div class="spec-item">
                        <div class="spec-label">Security Patch Level</div>
                        <div class="spec-value">${(security?.securityPatch ?: system?.securityPatch ?: "Unknown").escape()}</div>
                    </div>
                    <div class="spec-item">
                        <div class="spec-label">Verified Boot State</div>
                        <div class="spec-value">${(security?.verifiedBootState ?: "Unknown").escape()}</div>
                    </div>
                    <div class="spec-item">
                        <div class="spec-label">Keystore Type</div>
                        <div class="spec-value">${(security?.keystoreType ?: "Unknown").escape()}</div>
                    </div>
                    <div class="spec-item">
                        <div class="spec-label">Hardware-backed Keystore</div>
                        <div class="spec-value">
                            <span class="badge ${if (security?.hardwareBackedKeystore == true) "success" else "neutral"}">
                                ${if (security?.hardwareBackedKeystore == true) "Supported" else "N/A"}
                            </span>
                        </div>
                    </div>
                    <div class="spec-item">
                        <div class="spec-label">Biometric Strength Class</div>
                        <div class="spec-value">${(security?.biometricClass ?: "N/A").escape()}</div>
                    </div>
                </div>
            </div>

            <!-- Section 8: Audio Capabilities -->
            <div class="section-container">
                <div class="section-header">
                    <!-- Audio Icon -->
                    <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2"><path d="M9 18V5l12-2v13"/><circle cx="6" cy="18" r="3"/><circle cx="18" cy="16" r="3"/></svg>
                    <div class="section-title">Audio Capabilities</div>
                </div>
                <div class="spec-grid">
                    <div class="spec-item">
                        <div class="spec-label">Low Latency Support</div>
                        <div class="spec-value">
                            <span class="badge ${if (audio?.lowLatency == true) "success" else "neutral"}">
                                ${if (audio?.lowLatency == true) "Yes" else "No"}
                            </span>
                        </div>
                    </div>
                    <div class="spec-item">
                        <div class="spec-label">Pro Audio Support</div>
                        <div class="spec-value">
                            <span class="badge ${if (audio?.proAudio == true) "success" else "neutral"}">
                                ${if (audio?.proAudio == true) "Yes" else "No"}
                            </span>
                        </div>
                    </div>
                    <div class="spec-item">
                        <div class="spec-label">MIDI Interface Support</div>
                        <div class="spec-value">
                            <span class="badge ${if (audio?.midiSupport == true) "success" else "neutral"}">
                                ${if (audio?.midiSupport == true) "Yes" else "No"}
                            </span>
                        </div>
                    </div>
                    <div class="spec-item">
                        <div class="spec-label">System Sample Rate</div>
                        <div class="spec-value">${(audio?.sampleRate ?: "Unknown").escape()}</div>
                    </div>
                    <div class="spec-item">
                        <div class="spec-label">Audio Bit Depth</div>
                        <div class="spec-value">${(audio?.bitDepth ?: "N/A").escape()}</div>
                    </div>
                    <div class="spec-item">
                        <div class="spec-label">Current Audio Output Route</div>
                        <div class="spec-value">${(audio?.outputRoute ?: "Unknown").escape()}</div>
                    </div>
                </div>
            </div>

            <!-- Footer -->
            <div class="footer">
                Relab Control Center &copy; 2026. This diagnostic report is generated using device telemetry.
            </div>
        </body>
        </html>
        """.trimIndent()
    }
}

private fun String?.escape(): String {
    if (this == null) return "N/A"
    return this.replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
}

private fun findActivity(context: Context): android.app.Activity? {
    var ctx = context
    while (ctx is android.content.ContextWrapper) {
        if (ctx is android.app.Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}
