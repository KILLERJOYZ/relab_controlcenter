package com.example.relab_tool.benchmark.domain.model

import com.example.relab_tool.R

/**
 * 7 benchmark subsystems — each contains exactly 20 independent tests.
 * Total: 140 tests.
 *
 * Weight distribution:
 *  CPU_SINGLE_CORE  15% — Prime-core IPC, branch prediction, single-thread throughput
 *  CPU_MULTI_CORE   15% — Parallel scalability, cache coherency, scheduler efficiency
 *  GPU_VULKAN       20% — Vulkan compute, procedural geometry, advanced shading
 *  GPU_OPENGL       15% — OpenGL ES 3.2 rendering, fill-rate, texture throughput
 *  STORAGE_IO       10% — Sequential/random I/O, SQLite, Scoped Storage overhead
 *  VIDEO_CODEC      13% — MediaCodec encode/decode, AV1, multi-stream, transcode
 *  NETWORK_IPC      12% — Loopback TCP/UDP, IPC Binder, TLS, socket throughput
 */
enum class BenchmarkPillar(
    val weight: Float,
    val displayNameRes: Int
) {
    CPU_SINGLE_CORE(0.15f, R.string.pillar_cpu_single),
    CPU_MULTI_CORE(0.15f, R.string.pillar_cpu_multi),
    GPU_VULKAN(0.20f, R.string.pillar_gpu_vulkan),
    GPU_OPENGL(0.15f, R.string.pillar_gpu_opengl),
    STORAGE_IO(0.10f, R.string.pillar_storage),
    VIDEO_CODEC(0.13f, R.string.pillar_video_codec),
    NETWORK_IPC(0.12f, R.string.pillar_network_ipc),
}
