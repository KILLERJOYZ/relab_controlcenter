/**
 * benchmark_native.c — Low-level native benchmark helpers for rlcc_bench.
 *
 * Provides:
 *   1. O_DIRECT file I/O (IO_11, IO_12) — bypasses Linux kernel page cache,
 *      measuring raw UFS/eMMC flash throughput without cache interference.
 *   2. JNI roundtrip overhead (SC_20) — measures marshalling cost between
 *      the JVM and native execution environment.
 *   3. Simple ALU/FPU loops pinned to native code (SC_01, SC_02 fallback)
 *      that are immune to ART JIT dead-code elimination.
 *
 * All I/O operations write to paths within the app's Scoped Storage directory
 * (passed from Kotlin), ensuring Google Play policy compliance.
 * No MANAGE_EXTERNAL_STORAGE permission is required.
 */

#include <jni.h>
#include <android/log.h>

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <errno.h>
#include <fcntl.h>
#include <unistd.h>
#include <time.h>
#include <stdint.h>
#include <math.h>

#define TAG "rlcc_bench_native"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

/* O_DIRECT requires 512-byte aligned buffers and transfer sizes */
#define DIRECT_ALIGN 4096
#define DIRECT_BLOCK (4096)   /* 4KB block for random I/O tests */

static long long get_nanos(void) {
    struct timespec ts;
    clock_gettime(CLOCK_MONOTONIC, &ts);
    return (long long)ts.tv_sec * 1000000000LL + ts.tv_nsec;
}

/* ──────────────────────────────────────────────────────────────────
 * IO_11: O_DIRECT Sequential Write
 * Writes [sizeBytes] of zeroed data to [filePath] using O_DIRECT,
 * bypassing the kernel page cache.
 * Returns throughput in MB/s, or -1.0 on error.
 * ────────────────────────────────────────────────────────────────── */
JNIEXPORT jdouble JNICALL
Java_com_example_relab_1tool_benchmark_domain_engine_BenchmarkNativeBridge_nativeODirectWrite(
    JNIEnv *env, jclass clazz, jstring jFilePath, jlong sizeBytes)
{
    const char *filePath = (*env)->GetStringUTFChars(env, jFilePath, NULL);
    if (!filePath) return -1.0;

    int fd = open(filePath, O_WRONLY | O_CREAT | O_TRUNC | O_DIRECT, 0644);
    if (fd < 0) {
        /* Some Android kernels don't support O_DIRECT on ext4/f2fs — fall back gracefully */
        LOGE("O_DIRECT write open failed for %s: %s — falling back to buffered", filePath, strerror(errno));
        fd = open(filePath, O_WRONLY | O_CREAT | O_TRUNC, 0644);
        if (fd < 0) {
            (*env)->ReleaseStringUTFChars(env, jFilePath, filePath);
            return -1.0;
        }
    }

    void *buf = NULL;
    if (posix_memalign(&buf, DIRECT_ALIGN, DIRECT_BLOCK) != 0) {
        close(fd);
        (*env)->ReleaseStringUTFChars(env, jFilePath, filePath);
        return -1.0;
    }
    memset(buf, 0xAB, DIRECT_BLOCK);

    long long written = 0;
    long long start = get_nanos();

    while (written < sizeBytes) {
        long long toWrite = (sizeBytes - written < DIRECT_BLOCK) ? (sizeBytes - written) : DIRECT_BLOCK;
        ssize_t n = write(fd, buf, (size_t)toWrite);
        if (n < 0) break;
        written += n;
    }
    fsync(fd);

    long long elapsed = get_nanos() - start;
    free(buf);
    close(fd);
    (*env)->ReleaseStringUTFChars(env, jFilePath, filePath);

    if (elapsed <= 0 || written <= 0) return -1.0;
    double seconds = elapsed / 1e9;
    double mbPerSec = (double)written / (1024.0 * 1024.0) / seconds;
    LOGD("O_DIRECT write: %.2f MB/s (%lld bytes in %.3f s)", mbPerSec, written, seconds);
    return mbPerSec;
}

/* ──────────────────────────────────────────────────────────────────
 * IO_12: O_DIRECT Sequential Read
 * Reads [sizeBytes] from [filePath] using O_DIRECT.
 * Returns throughput in MB/s, or -1.0 on error.
 * ────────────────────────────────────────────────────────────────── */
JNIEXPORT jdouble JNICALL
Java_com_example_relab_1tool_benchmark_domain_engine_BenchmarkNativeBridge_nativeODirectRead(
    JNIEnv *env, jclass clazz, jstring jFilePath, jlong sizeBytes)
{
    const char *filePath = (*env)->GetStringUTFChars(env, jFilePath, NULL);
    if (!filePath) return -1.0;

    int fd = open(filePath, O_RDONLY | O_DIRECT);
    if (fd < 0) {
        LOGE("O_DIRECT read open failed for %s: %s — falling back to buffered", filePath, strerror(errno));
        fd = open(filePath, O_RDONLY);
        if (fd < 0) {
            (*env)->ReleaseStringUTFChars(env, jFilePath, filePath);
            return -1.0;
        }
    }

    void *buf = NULL;
    if (posix_memalign(&buf, DIRECT_ALIGN, DIRECT_BLOCK) != 0) {
        close(fd);
        (*env)->ReleaseStringUTFChars(env, jFilePath, filePath);
        return -1.0;
    }

    long long totalRead = 0;
    long long start = get_nanos();

    while (totalRead < sizeBytes) {
        ssize_t n = read(fd, buf, DIRECT_BLOCK);
        if (n <= 0) break;
        totalRead += n;
    }

    long long elapsed = get_nanos() - start;
    free(buf);
    close(fd);
    (*env)->ReleaseStringUTFChars(env, jFilePath, filePath);

    if (elapsed <= 0 || totalRead <= 0) return -1.0;
    double seconds = elapsed / 1e9;
    double mbPerSec = (double)totalRead / (1024.0 * 1024.0) / seconds;
    LOGD("O_DIRECT read: %.2f MB/s (%lld bytes in %.3f s)", mbPerSec, totalRead, seconds);
    return mbPerSec;
}

/* ──────────────────────────────────────────────────────────────────
 * IO_05/IO_06: 4KB Random I/O using O_DIRECT
 * Performs [opCount] random 4KB reads or writes at random offsets.
 * Returns IOPS (operations per second).
 * ────────────────────────────────────────────────────────────────── */
JNIEXPORT jdouble JNICALL
Java_com_example_relab_1tool_benchmark_domain_engine_BenchmarkNativeBridge_nativeODirectRandomRead(
    JNIEnv *env, jclass clazz, jstring jFilePath, jint opCount)
{
    const char *filePath = (*env)->GetStringUTFChars(env, jFilePath, NULL);
    if (!filePath) return -1.0;

    int fd = open(filePath, O_RDONLY | O_DIRECT);
    if (fd < 0) {
        fd = open(filePath, O_RDONLY);  /* O_DIRECT fallback */
        if (fd < 0) {
            (*env)->ReleaseStringUTFChars(env, jFilePath, filePath);
            return -1.0;
        }
    }

    off_t fileSize = lseek(fd, 0, SEEK_END);
    if (fileSize < DIRECT_BLOCK) {
        close(fd);
        (*env)->ReleaseStringUTFChars(env, jFilePath, filePath);
        return -1.0;
    }

    void *buf = NULL;
    posix_memalign(&buf, DIRECT_ALIGN, DIRECT_BLOCK);

    long long completed = 0;
    long long start = get_nanos();

    for (int i = 0; i < opCount; i++) {
        /* Random 4KB-aligned offset within file */
        off_t maxBlock = fileSize / DIRECT_BLOCK;
        off_t offset = ((off_t)rand() % maxBlock) * DIRECT_BLOCK;
        if (pread(fd, buf, DIRECT_BLOCK, offset) > 0) completed++;
    }

    long long elapsed = get_nanos() - start;
    free(buf);
    close(fd);
    (*env)->ReleaseStringUTFChars(env, jFilePath, filePath);

    if (elapsed <= 0) return -1.0;
    double iops = (double)completed / (elapsed / 1e9);
    LOGD("O_DIRECT random read: %.0f IOPS", iops);
    return iops;
}

/* ──────────────────────────────────────────────────────────────────
 * SC_20: JNI Roundtrip Overhead Measurement
 * This function body is intentionally empty — it exists only as the
 * target for millions of JNI calls from Kotlin to measure
 * call marshalling overhead (ns per call).
 * ────────────────────────────────────────────────────────────────── */
JNIEXPORT void JNICALL
Java_com_example_relab_1tool_benchmark_domain_engine_BenchmarkNativeBridge_nativeJniVoidCall(
    JNIEnv *env, jclass clazz)
{
    /* Intentionally empty — overhead measurement target */
}

/* ──────────────────────────────────────────────────────────────────
 * SC_01 Native ALU: 64-bit integer arithmetic
 * Performs [iterations] chained ALU operations (add/mul/xor/shift).
 * Returns measured throughput in Giga-operations/second.
 * ────────────────────────────────────────────────────────────────── */
JNIEXPORT jdouble JNICALL
Java_com_example_relab_1tool_benchmark_domain_engine_BenchmarkNativeBridge_nativeIntAluGops(
    JNIEnv *env, jclass clazz, jlong iterations)
{
    volatile uint64_t sink = 0;
    uint64_t a = 0x123456789ABCDEF0ULL;
    uint64_t b = 0xFEDCBA9876543210ULL;

    long long start = get_nanos();
    for (long long i = 0; i < iterations; i++) {
        a = a * 6364136223846793005ULL + 1442695040888963407ULL;
        b ^= (a >> 33);
        b = (b << 17) | (b >> 47);
        a += b;
    }
    long long elapsed = get_nanos() - start;
    sink = a ^ b;
    (void)sink;  /* prevent elimination */

    if (elapsed <= 0) return 0.0;
    /* Each iteration performs ~5 operations */
    double gops = (double)iterations * 5.0 / (elapsed / 1.0);
    return gops;
}

/* ──────────────────────────────────────────────────────────────────
 * SC_02 Native FPU: double-precision transcendental math
 * Returns measured throughput in Mega-operations/second.
 * ────────────────────────────────────────────────────────────────── */
JNIEXPORT jdouble JNICALL
Java_com_example_relab_1tool_benchmark_domain_engine_BenchmarkNativeBridge_nativeFpuMops(
    JNIEnv *env, jclass clazz, jlong iterations)
{
    volatile double sink = 0.0;
    double acc = 1.0;
    double step = 0.000001;

    long long start = get_nanos();
    for (long long i = 0; i < iterations; i++) {
        acc += step;
        double s = sin(acc);
        double c = cos(acc);
        double l = log(acc + 1.0);
        acc = s * c + l;
    }
    long long elapsed = get_nanos() - start;
    sink = acc;
    (void)sink;

    if (elapsed <= 0) return 0.0;
    /* Each iteration: ~3 transcendental ops */
    double mops = (double)iterations * 3.0 / (elapsed / 1e3);
    return mops;
}
