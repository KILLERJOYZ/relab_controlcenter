package com.example.relab_tool.benchmark.domain.engine

import android.content.Context
import com.example.relab_tool.benchmark.data.AppDatabase
import com.example.relab_tool.benchmark.data.BenchmarkResultEntity
import com.example.relab_tool.benchmark.domain.model.BenchmarkPillar
import com.example.relab_tool.benchmark.domain.model.ScoreTier
import com.example.relab_tool.benchmark.domain.model.SubScore
import com.example.relab_tool.benchmark.scoring.ScoreNormalizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.RandomAccessFile
import java.util.Random

class StorageBenchmark(private val context: Context) : BenchmarkEngine {
    override val pillar: BenchmarkPillar = BenchmarkPillar.STORAGE_IO

    override suspend fun run(onProgress: (Float) -> Unit): List<SubScore> = withContext(Dispatchers.IO) {
        val list = mutableListOf<SubScore>()
        
        val filesDir = context.filesDir
        val seqFile = File(filesDir, "bench_seq.tmp")
        val randFile = File(filesDir, "bench_rand.tmp")
        
        // 5a. Sequential Write
        onProgress(0.0f)
        val seqWriteResult = runSeqWrite(seqFile)
        list.add(SubScore("Sequential Write", seqWriteResult.speed, "MB/s", ScoreNormalizer.normalize(seqWriteResult.speed, 600.0, 2400.0, false), seqWriteResult.isPartial))
        
        // 5b. Sequential Read
        onProgress(0.2f)
        val seqReadResult = runSeqRead(seqFile)
        list.add(SubScore("Sequential Read", seqReadResult.speed, "MB/s", ScoreNormalizer.normalize(seqReadResult.speed, 900.0, 3600.0, false), seqReadResult.isPartial))
        
        try { seqFile.delete() } catch (e: Exception) {}
        
        // 5c. Random 4K Write
        onProgress(0.4f)
        val randWriteResult = runRandWrite(randFile)
        list.add(SubScore("Random 4K Write", randWriteResult.iops, "IOPS", ScoreNormalizer.normalize(randWriteResult.iops, 22000.0, 88000.0, false), randWriteResult.isPartial))
        
        // 5d. Random 4K Read
        onProgress(0.6f)
        val randReadResult = runRandRead(randFile)
        list.add(SubScore("Random 4K Read", randReadResult.iops, "IOPS", ScoreNormalizer.normalize(randReadResult.iops, 28000.0, 112000.0, false), randReadResult.isPartial))
        
        try { randFile.delete() } catch (e: Exception) {}
        
        // 5e. SQLite WAL p99 commit latency
        onProgress(0.8f)
        val sqliteResult = runSqliteLatency()
        list.add(SubScore("SQLite WAL Commit Latency", sqliteResult.p99, "ms", ScoreNormalizer.normalize(sqliteResult.p99, 2.5, 0.25, true), sqliteResult.isPartial))
        
        onProgress(1.0f)
        list
    }

    data class SpeedResult(val speed: Double, val isPartial: Boolean)
    data class IopsResult(val iops: Double, val isPartial: Boolean)
    data class SqliteResult(val p99: Double, val isPartial: Boolean)

    private fun runSeqWrite(file: File): SpeedResult {
        return try {
            val size = 64 * 1024 * 1024
            val bufferSize = 8 * 1024 * 1024
            val buffer = ByteArray(bufferSize) { 0xAA.toByte() }
            val startTime = System.nanoTime()
            val out = BufferedOutputStream(FileOutputStream(file), bufferSize)
            var written = 0
            while (written < size) {
                out.write(buffer)
                written += bufferSize
            }
            out.flush()
            out.close()
            val elapsed = (System.nanoTime() - startTime) / 1e9
            val mbPerSec = (size.toDouble() / (1024.0 * 1024.0)) / elapsed
            SpeedResult(mbPerSec, false)
        } catch (e: IOException) {
            SpeedResult(0.0, true)
        }
    }

    private fun runSeqRead(file: File): SpeedResult {
        if (!file.exists()) return SpeedResult(0.0, true)
        return try {
            val size = file.length()
            val bufferSize = 8 * 1024 * 1024
            val buffer = ByteArray(bufferSize)
            val startTime = System.nanoTime()
            val `in` = BufferedInputStream(FileInputStream(file), bufferSize)
            var read = 0
            while (true) {
                val len = `in`.read(buffer)
                if (len <= 0) break
                read += len
            }
            `in`.close()
            val elapsed = (System.nanoTime() - startTime) / 1e9
            val mbPerSec = (size.toDouble() / (1024.0 * 1024.0)) / elapsed
            SpeedResult(mbPerSec, false)
        } catch (e: IOException) {
            SpeedResult(0.0, true)
        }
    }

    private fun runRandWrite(file: File): IopsResult {
        return try {
            val size = 32 * 1024 * 1024
            val raf = RandomAccessFile(file, "rw")
            raf.setLength(size.toLong())
            
            val buffer = ByteArray(4096) { 0xBB.toByte() }
            val random = Random(42)
            val maxOffsets = size / 4096
            
            val iterations = 2000
            val startTime = System.nanoTime()
            for (i in 0 until iterations) {
                val offset = random.nextInt(maxOffsets) * 4096L
                raf.seek(offset)
                raf.write(buffer)
                if (i % 100 == 0) {
                    raf.getFD().sync()
                }
            }
            raf.getFD().sync()
            raf.close()
            val elapsed = (System.nanoTime() - startTime) / 1e9
            val iops = iterations.toDouble() / elapsed
            IopsResult(iops, false)
        } catch (e: IOException) {
            IopsResult(0.0, true)
        }
    }

    private fun runRandRead(file: File): IopsResult {
        if (!file.exists()) return IopsResult(0.0, true)
        return try {
            val size = file.length().toInt()
            val raf = RandomAccessFile(file, "r")
            val buffer = ByteArray(4096)
            val random = Random(84)
            val maxOffsets = size / 4096
            
            val iterations = 2000
            val startTime = System.nanoTime()
            for (i in 0 until iterations) {
                val offset = random.nextInt(maxOffsets) * 4096L
                raf.seek(offset)
                raf.readFully(buffer)
            }
            raf.close()
            val elapsed = (System.nanoTime() - startTime) / 1e9
            val iops = iterations.toDouble() / elapsed
            IopsResult(iops, false)
        } catch (e: IOException) {
            IopsResult(0.0, true)
        }
    }

    private suspend fun runSqliteLatency(): SqliteResult = withContext(Dispatchers.IO) {
        try {
            val db = AppDatabase.getDatabase(context)
            val dao = db.benchmarkDao()
            val latencies = LongArray(100)
            
            for (i in 0 until 100) {
                val entity = BenchmarkResultEntity(
                    timestamp = System.currentTimeMillis() + i,
                    deviceModel = "TestModel",
                    deviceSoc = "TestSoC",
                    hardwareScore = 1000 + i,
                    connectivityScore = 200 + i,
                    totalScore = 1200 + i,
                    tier = ScoreTier.MID,
                    pillarScores = emptyList(),
                    isQuickTest = true
                )
                val start = System.nanoTime()
                val insertId = dao.insertResult(entity)
                val elapsed = System.nanoTime() - start
                latencies[i] = elapsed
                
                dao.deleteResult(entity.copy(id = insertId))
            }
            
            latencies.sort()
            val p99Ns = latencies[98]
            val p99Ms = p99Ns.toDouble() / 1e6
            SqliteResult(p99Ms, false)
        } catch (e: Exception) {
            SqliteResult(2.5, true)
        }
    }
}
