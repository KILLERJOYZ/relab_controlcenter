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
import java.io.*
import java.nio.ByteBuffer
import java.util.Random

class StorageBenchmark(private val context: Context) : BenchmarkEngine {
    override val pillar: BenchmarkPillar = BenchmarkPillar.STORAGE_IO

    override suspend fun run(onProgress: suspend (Float) -> Unit): List<SubScore> = withContext(Dispatchers.IO) {
        val list = mutableListOf<SubScore>()
        
        val filesDir = context.filesDir
        val seqFile = File(filesDir, "bench_seq.tmp")
        val seqFileSmall = File(filesDir, "bench_seq_small.tmp")
        val randFile = File(filesDir, "bench_rand.tmp")
        val randFile16K = File(filesDir, "bench_rand_16k.tmp")
        val mixFile = File(filesDir, "bench_mix.tmp")
        val appendFile = File(filesDir, "bench_append.tmp")
        val copySrcFile = File(filesDir, "bench_copy_src.tmp")
        val copyDstFile = File(filesDir, "bench_copy_dst.tmp")
        
        try {
            // 1. Sequential Write (256MB - scaled to 64MB for safety/speed)
            onProgress(0.00f)
            val seqWriteVal = runSeqWrite(seqFile, 64 * 1024 * 1024, 8 * 1024 * 1024)
            list.add(SubScore("Sequential Write (64MB)", seqWriteVal, "MB/s", ScoreNormalizer.normalize(seqWriteVal, 300.0, 1500.0, false)))
            
            // 2. Sequential Read (256MB - scaled to 64MB)
            onProgress(0.05f)
            val seqReadVal = runSeqRead(seqFile, 8 * 1024 * 1024)
            list.add(SubScore("Sequential Read (64MB)", seqReadVal, "MB/s", ScoreNormalizer.normalize(seqReadVal, 500.0, 2500.0, false)))
            
            // 3. Sequential Write (64MB, small buf)
            onProgress(0.10f)
            val seqWriteSmallBufVal = runSeqWrite(seqFileSmall, 16 * 1024 * 1024, 4 * 1024)
            list.add(SubScore("Seq Write (4KB Buffer)", seqWriteSmallBufVal, "MB/s", ScoreNormalizer.normalize(seqWriteSmallBufVal, 100.0, 500.0, false)))
            
            // 4. Sequential Read (64MB, small buf)
            onProgress(0.15f)
            val seqReadSmallBufVal = runSeqRead(seqFileSmall, 4 * 1024)
            list.add(SubScore("Seq Read (4KB Buffer)", seqReadSmallBufVal, "MB/s", ScoreNormalizer.normalize(seqReadSmallBufVal, 200.0, 1000.0, false)))
            
            // 5. Random 4K Write
            onProgress(0.20f)
            val rand4kWriteVal = runRandomWrite(randFile, 8 * 1024 * 1024, 4096, 1000)
            list.add(SubScore("Random 4K Write", rand4kWriteVal, "IOPS", ScoreNormalizer.normalize(rand4kWriteVal, 15000.0, 60000.0, false)))
            
            // 6. Random 4K Read
            onProgress(0.25f)
            val rand4kReadVal = runRandomRead(randFile, 4096, 1000)
            list.add(SubScore("Random 4K Read", rand4kReadVal, "IOPS", ScoreNormalizer.normalize(rand4kReadVal, 20000.0, 80000.0, false)))
            
            // 7. Random 16K Write
            onProgress(0.30f)
            val rand16kWriteVal = runRandomWrite(randFile16K, 8 * 1024 * 1024, 16384, 500)
            list.add(SubScore("Random 16K Write", rand16kWriteVal, "IOPS", ScoreNormalizer.normalize(rand16kWriteVal, 8000.0, 32000.0, false)))
            
            // 8. Random 16K Read
            onProgress(0.35f)
            val rand16kReadVal = runRandomRead(randFile16K, 16384, 500)
            list.add(SubScore("Random 16K Read", rand16kReadVal, "IOPS", ScoreNormalizer.normalize(rand16kReadVal, 10000.0, 40000.0, false)))
            
            // 9. Mixed Random R/W (70/30)
            onProgress(0.40f)
            val mixed7030Val = runMixedRandomRW(mixFile, 8 * 1024 * 1024, 4096, 1000, 0.7)
            list.add(SubScore("Mixed Random RW (70/30)", mixed7030Val, "IOPS", ScoreNormalizer.normalize(mixed7030Val, 12000.0, 48000.0, false)))
            
            // 10. Mixed Random R/W (50/50)
            onProgress(0.45f)
            val mixed5050Val = runMixedRandomRW(mixFile, 8 * 1024 * 1024, 4096, 1000, 0.5)
            list.add(SubScore("Mixed Random RW (50/50)", mixed5050Val, "IOPS", ScoreNormalizer.normalize(mixed5050Val, 10000.0, 40000.0, false)))
            
            // 11. SQLite WAL Commit (single)
            onProgress(0.50f)
            val sqliteSingleVal = runSqliteCommitLatency()
            list.add(SubScore("SQLite Single Commit", sqliteSingleVal, "ms", ScoreNormalizer.normalize(sqliteSingleVal, 5.0, 0.5, true)))
            
            // 12. SQLite Batch Transaction
            onProgress(0.55f)
            val sqliteBatchVal = runSqliteBatchLatency()
            list.add(SubScore("SQLite Batch Transaction", sqliteBatchVal, "ms", ScoreNormalizer.normalize(sqliteBatchVal, 20.0, 2.0, true)))
            
            // 13. Large File Create/Delete
            onProgress(0.60f)
            val largeFileMetadataVal = runLargeFileMetadataOps(filesDir)
            list.add(SubScore("Large File Create/Delete", largeFileMetadataVal, "ms", ScoreNormalizer.normalize(largeFileMetadataVal, 100.0, 10.0, true)))
            
            // 14. Small File Create (1KB)
            onProgress(0.65f)
            val smallFileMetadataVal = runSmallFileMetadataOps(filesDir)
            list.add(SubScore("Small File Create/Delete", smallFileMetadataVal, "ms", ScoreNormalizer.normalize(smallFileMetadataVal, 200.0, 20.0, true)))
            
            // 15. Directory Traversal
            onProgress(0.70f)
            val dirTraversalVal = runDirectoryTraversal(filesDir)
            list.add(SubScore("Directory Traversal", dirTraversalVal, "ms", ScoreNormalizer.normalize(dirTraversalVal, 50.0, 5.0, true)))
            
            // 16. File Rename Stress
            onProgress(0.75f)
            val renameVal = runFileRenameStress(filesDir)
            list.add(SubScore("File Rename Latency", renameVal, "ms", ScoreNormalizer.normalize(renameVal, 50.0, 5.0, true)))
            
            // 17. Append-Only Write
            onProgress(0.80f)
            val appendVal = runAppendOnlyWrite(appendFile)
            list.add(SubScore("Append-Only Write Speed", appendVal, "MB/s", ScoreNormalizer.normalize(appendVal, 5.0, 25.0, false)))
            
            // 18. File Copy Speed
            onProgress(0.85f)
            val copyVal = runFileCopy(copySrcFile, copyDstFile)
            list.add(SubScore("File Copy Throughput", copyVal, "MB/s", ScoreNormalizer.normalize(copyVal, 100.0, 500.0, false)))
            
            // 19. Multi-File Parallel Write
            onProgress(0.90f)
            val parallelWriteVal = runParallelWriteStress(filesDir)
            list.add(SubScore("Parallel File Writes", parallelWriteVal, "MB/s", ScoreNormalizer.normalize(parallelWriteVal, 150.0, 600.0, false)))
            
            // 20. Temp File Throughput
            onProgress(0.95f)
            val tempFileLifecycleVal = runTempFileLifecycle(filesDir)
            list.add(SubScore("Temp File Lifecycle Speed", tempFileLifecycleVal, "ms", ScoreNormalizer.normalize(tempFileLifecycleVal, 150.0, 15.0, true)))
            
        } finally {
            // Clean up files
            try { seqFile.delete() } catch (e: Exception) {}
            try { seqFileSmall.delete() } catch (e: Exception) {}
            try { randFile.delete() } catch (e: Exception) {}
            try { randFile16K.delete() } catch (e: Exception) {}
            try { mixFile.delete() } catch (e: Exception) {}
            try { appendFile.delete() } catch (e: Exception) {}
            try { copySrcFile.delete() } catch (e: Exception) {}
            try { copyDstFile.delete() } catch (e: Exception) {}
        }
        
        onProgress(1.00f)
        list
    }

    private fun runSeqWrite(file: File, size: Int, bufferSize: Int): Double {
        return try {
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
            (size.toDouble() / (1024.0 * 1024.0)) / elapsed
        } catch (e: IOException) {
            0.0
        }
    }

    private fun runSeqRead(file: File, bufferSize: Int): Double {
        if (!file.exists()) return 0.0
        return try {
            val size = file.length()
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
            (size.toDouble() / (1024.0 * 1024.0)) / elapsed
        } catch (e: IOException) {
            0.0
        }
    }

    private fun runRandomWrite(file: File, fileSize: Int, blockSize: Int, iterations: Int): Double {
        return try {
            val raf = RandomAccessFile(file, "rw")
            raf.setLength(fileSize.toLong())
            val buffer = ByteArray(blockSize) { 0xBB.toByte() }
            val random = Random(42)
            val maxOffsets = fileSize / blockSize
            val startTime = System.nanoTime()
            for (i in 0 until iterations) {
                val offset = random.nextInt(maxOffsets) * blockSize.toLong()
                raf.seek(offset)
                raf.write(buffer)
                if (i % 50 == 0) {
                    raf.fd.sync()
                }
            }
            raf.fd.sync()
            raf.close()
            val elapsed = (System.nanoTime() - startTime) / 1e9
            iterations.toDouble() / elapsed
        } catch (e: IOException) {
            0.0
        }
    }

    private fun runRandomRead(file: File, blockSize: Int, iterations: Int): Double {
        if (!file.exists()) return 0.0
        return try {
            val fileSize = file.length().toInt()
            val raf = RandomAccessFile(file, "r")
            val buffer = ByteArray(blockSize)
            val random = Random(84)
            val maxOffsets = fileSize / blockSize
            val startTime = System.nanoTime()
            for (i in 0 until iterations) {
                val offset = random.nextInt(maxOffsets) * blockSize.toLong()
                raf.seek(offset)
                raf.readFully(buffer)
            }
            raf.close()
            val elapsed = (System.nanoTime() - startTime) / 1e9
            iterations.toDouble() / elapsed
        } catch (e: IOException) {
            0.0
        }
    }

    private fun runMixedRandomRW(file: File, fileSize: Int, blockSize: Int, iterations: Int, readRatio: Double): Double {
        return try {
            val raf = RandomAccessFile(file, "rw")
            raf.setLength(fileSize.toLong())
            val buffer = ByteArray(blockSize)
            val random = Random(123)
            val maxOffsets = fileSize / blockSize
            val startTime = System.nanoTime()
            for (i in 0 until iterations) {
                val offset = random.nextInt(maxOffsets) * blockSize.toLong()
                raf.seek(offset)
                if (random.nextDouble() < readRatio) {
                    raf.readFully(buffer)
                } else {
                    raf.write(buffer)
                }
                if (i % 100 == 0) {
                    raf.fd.sync()
                }
            }
            raf.fd.sync()
            raf.close()
            val elapsed = (System.nanoTime() - startTime) / 1e9
            iterations.toDouble() / elapsed
        } catch (e: IOException) {
            0.0
        }
    }

    private fun runSqliteCommitLatency(): Double {
        return try {
            val db = AppDatabase.getDatabase(context)
            val dao = db.benchmarkDao()
            val latencies = mutableListOf<Long>()
            
            for (i in 0 until 20) {
                val entity = BenchmarkResultEntity(
                    timestamp = System.currentTimeMillis() + i,
                    deviceModel = "Test", deviceSoc = "Test",
                    hardwareScore = 1, connectivityScore = 1, totalScore = 2,
                    tier = ScoreTier.MID, pillarScores = emptyList(), isQuickTest = true
                )
                val start = System.nanoTime()
                val insertId = dao.insertResult(entity)
                val elapsed = System.nanoTime() - start
                latencies.add(elapsed)
                dao.deleteResult(entity.copy(id = insertId))
            }
            val avgNs = latencies.average()
            avgNs / 1e6
        } catch (e: Exception) {
            5.0
        }
    }

    private fun runSqliteBatchLatency(): Double {
        return try {
            val db = AppDatabase.getDatabase(context)
            val dao = db.benchmarkDao()
            val list = List(10) { i ->
                BenchmarkResultEntity(
                    timestamp = System.currentTimeMillis() + i,
                    deviceModel = "Test", deviceSoc = "Test",
                    hardwareScore = 1, connectivityScore = 1, totalScore = 2,
                    tier = ScoreTier.MID, pillarScores = emptyList(), isQuickTest = true
                )
            }
            val start = System.nanoTime()
            db.runInTransaction {
                for (item in list) {
                    dao.insertResult(item)
                }
            }
            val elapsed = System.nanoTime() - start
            // Cleanup
            val all = dao.getAllResultsSync()
            for (entity in all) {
                if (entity.deviceModel == "Test") {
                    dao.deleteResult(entity)
                }
            }
            elapsed.toDouble() / 1e6
        } catch (e: Exception) {
            20.0
        }
    }

    private fun runLargeFileMetadataOps(dir: File): Double {
        return try {
            val startTime = System.nanoTime()
            val fileList = mutableListOf<File>()
            for (i in 0 until 10) {
                val file = File(dir, "metadata_large_$i.tmp")
                val out = FileOutputStream(file)
                out.write(ByteArray(1024 * 1024)) // 1MB
                out.close()
                fileList.add(file)
            }
            for (file in fileList) {
                file.delete()
            }
            val elapsed = System.nanoTime() - startTime
            elapsed.toDouble() / 1e6
        } catch (e: Exception) {
            100.0
        }
    }

    private fun runSmallFileMetadataOps(dir: File): Double {
        return try {
            val startTime = System.nanoTime()
            val fileList = mutableListOf<File>()
            for (i in 0 until 50) {
                val file = File(dir, "metadata_small_$i.tmp")
                val out = FileOutputStream(file)
                out.write(ByteArray(1024)) // 1KB
                out.close()
                fileList.add(file)
            }
            for (file in fileList) {
                file.delete()
            }
            val elapsed = System.nanoTime() - startTime
            elapsed.toDouble() / 1e6
        } catch (e: Exception) {
            200.0
        }
    }

    private fun runDirectoryTraversal(dir: File): Double {
        return try {
            // Build a small directory tree
            val subDirs = List(3) { i -> File(dir, "sub_dir_$i").apply { mkdirs() } }
            for (subDir in subDirs) {
                for (j in 0 until 5) {
                    File(subDir, "traversal_file_$j.tmp").createNewFile()
                }
            }
            val startTime = System.nanoTime()
            // Traverse
            var count = 0
            dir.walkTopDown().forEach {
                if (it.isFile) count++
            }
            val elapsed = System.nanoTime() - startTime
            // Clean up
            for (subDir in subDirs) {
                subDir.deleteRecursively()
            }
            elapsed.toDouble() / 1e6
        } catch (e: Exception) {
            50.0
        }
    }

    private fun runFileRenameStress(dir: File): Double {
        return try {
            val file = File(dir, "rename_source.tmp")
            file.createNewFile()
            val startTime = System.nanoTime()
            for (i in 0 until 50) {
                val target = File(dir, "rename_target_$i.tmp")
                file.renameTo(target)
                target.renameTo(file)
            }
            val elapsed = System.nanoTime() - startTime
            file.delete()
            elapsed.toDouble() / 1e6
        } catch (e: Exception) {
            50.0
        }
    }

    private fun runAppendOnlyWrite(file: File): Double {
        return try {
            val buffer = ByteArray(1024) { 0xCC.toByte() } // 1KB
            val startTime = System.nanoTime()
            val out = FileOutputStream(file, true)
            for (i in 0 until 1000) {
                out.write(buffer)
            }
            out.flush()
            out.close()
            val elapsed = (System.nanoTime() - startTime) / 1e9
            (1.0) / elapsed // 1MB / elapsed
        } catch (e: Exception) {
            0.0
        }
    }

    private fun runFileCopy(src: File, dst: File): Double {
        return try {
            // Write 8MB source
            val size = 8 * 1024 * 1024
            val buffer = ByteArray(64 * 1024)
            val out = FileOutputStream(src)
            var written = 0
            while (written < size) {
                out.write(buffer)
                written += buffer.size
            }
            out.close()
            // Copy
            val startTime = System.nanoTime()
            val input = FileInputStream(src)
            val output = FileOutputStream(dst)
            val buf = ByteArray(64 * 1024)
            var len: Int
            while (input.read(buf).also { len = it } > 0) {
                output.write(buf, 0, len)
            }
            input.close()
            output.close()
            val elapsed = (System.nanoTime() - startTime) / 1e9
            (8.0) / elapsed // 8MB copied
        } catch (e: Exception) {
            0.0
        }
    }

    private fun runParallelWriteStress(dir: File): Double {
        return try {
            val threads = 4
            val size = 2 * 1024 * 1024 // 2MB each
            val startTime = System.nanoTime()
            val list = mutableListOf<Thread>()
            for (t in 0 until threads) {
                list.add(Thread {
                    val file = File(dir, "parallel_write_$t.tmp")
                    val out = FileOutputStream(file)
                    out.write(ByteArray(size))
                    out.close()
                    file.delete()
                })
            }
            list.forEach { it.start() }
            list.forEach { it.join() }
            val elapsed = (System.nanoTime() - startTime) / 1e9
            val totalBytes = size.toDouble() * threads
            (totalBytes / (1024.0 * 1024.0)) / elapsed
        } catch (e: Exception) {
            0.0
        }
    }

    private fun runTempFileLifecycle(dir: File): Double {
        return try {
            val startTime = System.nanoTime()
            for (i in 0 until 20) {
                val temp = File.createTempFile("lifecycle_temp", ".tmp", dir)
                val out = FileOutputStream(temp)
                out.write(ByteArray(1024))
                out.close()
                val read = FileInputStream(temp).readBytes()
                temp.delete()
            }
            val elapsed = System.nanoTime() - startTime
            elapsed.toDouble() / 1e6
        } catch (e: Exception) {
            150.0
        }
    }
}
