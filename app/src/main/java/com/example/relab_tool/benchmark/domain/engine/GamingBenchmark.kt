package com.example.relab_tool.benchmark.domain.engine

import android.content.Context
import com.example.relab_tool.benchmark.domain.model.BenchmarkPillar
import com.example.relab_tool.benchmark.domain.model.SubScore
import com.example.relab_tool.benchmark.scoring.ScoreNormalizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.PriorityQueue
import java.util.Random

class GamingBenchmark(private val context: Context) : BenchmarkEngine {
    override val pillar: BenchmarkPillar = BenchmarkPillar.GAMING_SIMULATION

    override suspend fun run(onProgress: suspend (Float) -> Unit): List<SubScore> = withContext(Dispatchers.Default) {
        val list = mutableListOf<SubScore>()

        // 1. Rigid Body Physics
        onProgress(0.00f)
        val physicsVal = runRigidBodyPhysics()
        list.add(SubScore("Rigid Body Physics", physicsVal, "k-steps/s", ScoreNormalizer.normalize(physicsVal, 100.0, 1000.0, false), false))

        // 2. Particle System
        onProgress(0.05f)
        val particlesVal = runParticleSystem()
        list.add(SubScore("Particle System", particlesVal, "M-updates/s", ScoreNormalizer.normalize(particlesVal, 50.0, 500.0, false), false))

        // 3. Ray-Sphere Intersection
        onProgress(0.10f)
        val raySphereVal = runRaySphereIntersection()
        list.add(SubScore("Ray-Sphere Intersection", raySphereVal, "M-rays/s", ScoreNormalizer.normalize(raySphereVal, 10.0, 100.0, false), false))

        // 4. A* Pathfinding
        onProgress(0.15f)
        val astarVal = runAStarPathfinding()
        list.add(SubScore("A* Pathfinding", astarVal, "paths/s", ScoreNormalizer.normalize(astarVal, 200.0, 2000.0, false), false))

        // 5. AABB Collision Detection
        onProgress(0.20f)
        val collisionVal = runAABBCollision()
        list.add(SubScore("AABB Collision", collisionVal, "M-checks/s", ScoreNormalizer.normalize(collisionVal, 5.0, 50.0, false), false))

        // 6. Scene Graph Traversal
        onProgress(0.25f)
        val sceneGraphVal = runSceneGraphTraversal()
        list.add(SubScore("Scene Graph Traversal", sceneGraphVal, "k-traversals/s", ScoreNormalizer.normalize(sceneGraphVal, 20.0, 200.0, false), false))

        // 7. Spatial Hash Grid
        onProgress(0.30f)
        val spatialHashVal = runSpatialHashGrid()
        list.add(SubScore("Spatial Hash Grid", spatialHashVal, "M-ops/s", ScoreNormalizer.normalize(spatialHashVal, 1.0, 10.0, false), false))

        // 8. Skeletal Animation
        onProgress(0.35f)
        val skeletonVal = runSkeletalAnimation()
        list.add(SubScore("Skeletal Animation", skeletonVal, "k-bones/s", ScoreNormalizer.normalize(skeletonVal, 50.0, 500.0, false), false))

        // 9. Terrain LOD Mesh
        onProgress(0.40f)
        val terrainVal = runTerrainLodMesh()
        list.add(SubScore("Terrain LOD Mesh", terrainVal, "vertices/s", ScoreNormalizer.normalize(terrainVal, 10000.0, 100000.0, false), false))

        // 10. Audio Mixing
        onProgress(0.45f)
        val audioMixingVal = runAudioMixing()
        list.add(SubScore("Audio Mixing", audioMixingVal, "M-samples/s", ScoreNormalizer.normalize(audioMixingVal, 5.0, 50.0, false), false))

        // 11. Game Loop Jitter
        onProgress(0.50f)
        val jitterVal = runGameLoopJitter()
        list.add(SubScore("Game Loop Jitter", jitterVal, "ms", ScoreNormalizer.normalize(jitterVal, 5.0, 0.1, true), false))

        // 12. Entity Component System
        onProgress(0.55f)
        val ecsVal = runECSQuery()
        list.add(SubScore("ECS Query", ecsVal, "M-updates/s", ScoreNormalizer.normalize(ecsVal, 10.0, 100.0, false), false))

        // 13. Frustum Culling
        onProgress(0.60f)
        val frustumCullingVal = runFrustumCulling()
        list.add(SubScore("Frustum Culling", frustumCullingVal, "M-checks/s", ScoreNormalizer.normalize(frustumCullingVal, 2.0, 20.0, false), false))

        // 14. Octree Build & Query
        onProgress(0.65f)
        val octreeVal = runOctreeBuildAndQuery()
        list.add(SubScore("Octree Ops", octreeVal, "ops/s", ScoreNormalizer.normalize(octreeVal, 100.0, 1000.0, false), false))

        // 15. Procedural Noise
        onProgress(0.70f)
        val noiseVal = runProceduralNoise()
        list.add(SubScore("Procedural Noise", noiseVal, "M-pixels/s", ScoreNormalizer.normalize(noiseVal, 5.0, 50.0, false), false))

        // 16. State Machine AI
        onProgress(0.75f)
        val stateMachineVal = runStateMachineAI()
        list.add(SubScore("State Machine AI", stateMachineVal, "M-transitions/s", ScoreNormalizer.normalize(stateMachineVal, 1.0, 10.0, false), false))

        // 17. Navmesh Raycast
        onProgress(0.80f)
        val navmeshVal = runNavmeshRaycast()
        list.add(SubScore("Navmesh Raycast", navmeshVal, "k-casts/s", ScoreNormalizer.normalize(navmeshVal, 10.0, 100.0, false), false))

        // 18. Input Buffer
        onProgress(0.85f)
        val inputBufferVal = runInputBuffer()
        list.add(SubScore("Input Event Throughput", inputBufferVal, "M-events/s", ScoreNormalizer.normalize(inputBufferVal, 5.0, 50.0, false), false))

        // 19. Behavior Tree AI
        onProgress(0.90f)
        val behaviorTreeVal = runBehaviorTreeAI()
        list.add(SubScore("Behavior Tree AI", behaviorTreeVal, "k-ticks/s", ScoreNormalizer.normalize(behaviorTreeVal, 10.0, 100.0, false), false))

        // 20. Memory Pool Allocator
        onProgress(0.95f)
        val memPoolVal = runMemoryPoolAllocator()
        list.add(SubScore("Memory Pool Allocator", memPoolVal, "M-allocs/s", ScoreNormalizer.normalize(memPoolVal, 2.0, 20.0, false), false))

        onProgress(1.00f)
        list
    }

    // 1. Rigid Body Physics
    private fun runRigidBodyPhysics(): Double {
        data class Body(var x: Float, var y: Float, var vx: Float, var vy: Float, val radius: Float)
        val bodies = Array(500) { Body(
            x = (it % 20).toFloat(), y = (it / 20).toFloat(),
            vx = ((it * 7) % 10 - 5) / 10f, vy = ((it * 13) % 10 - 5) / 10f,
            radius = 0.5f
        )}
        val dt = 0.016f
        val startTime = System.nanoTime()
        for (step in 0 until 200) {
            for (b in bodies) {
                b.vy -= 9.8f * dt
                b.x += b.vx * dt
                b.y += b.vy * dt
                if (b.y < b.radius) { b.y = b.radius; b.vy = -b.vy * 0.8f }
            }
            // Intentionally O(N^2) lightweight checks
            for (i in 0 until 100) {
                for (j in i + 1 until bodies.size) {
                    val dx = bodies[i].x - bodies[j].x
                    val dy = bodies[i].y - bodies[j].y
                    val dist = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
                    if (dist < bodies[i].radius + bodies[j].radius && dist > 0.001f) {
                        bodies[i].vx = -bodies[i].vx
                        bodies[j].vx = -bodies[j].vx
                    }
                }
            }
        }
        val elapsed = (System.nanoTime() - startTime) / 1e9
        return (500 * 200) / elapsed / 1000.0 // k-steps/s
    }

    // 2. Particle System
    private fun runParticleSystem(): Double {
        val count = 10000
        val px = FloatArray(count)
        val py = FloatArray(count)
        val vx = FloatArray(count)
        val vy = FloatArray(count)
        val random = Random(42)
        for (i in 0 until count) {
            vx[i] = random.nextFloat() - 0.5f
            vy[i] = random.nextFloat() - 0.5f
        }
        val startTime = System.nanoTime()
        for (frame in 0 until 100) {
            for (i in 0 until count) {
                px[i] += vx[i]
                py[i] += vy[i]
                vy[i] -= 0.01f
            }
        }
        val elapsed = (System.nanoTime() - startTime) / 1e9
        return (count.toDouble() * 100) / elapsed / 1e6 // M-updates/s
    }

    // 3. Ray-Sphere Intersection
    private fun runRaySphereIntersection(): Double {
        val rayCount = 50000
        val ox = FloatArray(rayCount) { it.toFloat() * 0.001f }
        val oy = FloatArray(rayCount) { it.toFloat() * 0.001f }
        val oz = FloatArray(rayCount) { -10.0f }
        val dx = FloatArray(rayCount) { 0.0f }
        val dy = FloatArray(rayCount) { 0.0f }
        val dz = FloatArray(rayCount) { 1.0f }
        
        val sx = 0.0f
        val sy = 0.0f
        val sz = 0.0f
        val sr = 2.0f
        
        var hitCount = 0
        val startTime = System.nanoTime()
        for (pass in 0 until 10) {
            for (i in 0 until rayCount) {
                val cx = ox[i] - sx
                val cy = oy[i] - sy
                val cz = oz[i] - sz
                val b = cx * dx[i] + cy * dy[i] + cz * dz[i]
                val c = cx * cx + cy * cy + cz * cz - sr * sr
                val discriminant = b * b - c
                if (discriminant >= 0) {
                    hitCount++
                }
            }
        }
        val elapsed = (System.nanoTime() - startTime) / 1e9
        return (rayCount.toDouble() * 10) / elapsed / 1e6 // M-rays/s
    }

    // 4. A* Pathfinding
    private fun runAStarPathfinding(): Double {
        val gridSize = 64
        val blocked = BooleanArray(gridSize * gridSize)
        val random = Random(42)
        for (i in blocked.indices) { blocked[i] = random.nextFloat() < 0.2f }
        blocked[0] = false
        blocked[gridSize * gridSize - 1] = false

        val startTime = System.nanoTime()
        var totalSearchCount = 0
        for (path in 0 until 100) {
            val start = random.nextInt(gridSize * gridSize)
            val goal = random.nextInt(gridSize * gridSize)
            if (!blocked[start] && !blocked[goal]) {
                aStarSearch(gridSize, blocked, start, goal)
                totalSearchCount++
            }
        }
        val elapsed = (System.nanoTime() - startTime) / 1e9
        return totalSearchCount / elapsed // paths/s
    }

    private fun aStarSearch(gridSize: Int, blocked: BooleanArray, start: Int, goal: Int): Int {
        val openSet = PriorityQueue<Pair<Int, Int>>(compareBy { it.second })
        val closedSet = HashSet<Int>()
        openSet.add(Pair(start, 0))
        val gScore = HashMap<Int, Int>()
        gScore[start] = 0

        while (openSet.isNotEmpty()) {
            val current = openSet.poll()?.first ?: break
            if (current == goal) return gScore[goal] ?: 0
            closedSet.add(current)

            val cy = current / gridSize
            val cx = current % gridSize

            val neighbors = mutableListOf<Int>()
            if (cx > 0) neighbors.add(current - 1)
            if (cx < gridSize - 1) neighbors.add(current + 1)
            if (cy > 0) neighbors.add(current - gridSize)
            if (cy < gridSize - 1) neighbors.add(current + gridSize)

            for (neighbor in neighbors) {
                if (blocked[neighbor] || closedSet.contains(neighbor)) continue
                val tentativeG = (gScore[current] ?: 0) + 1
                if (tentativeG < (gScore[neighbor] ?: Int.MAX_VALUE)) {
                    gScore[neighbor] = tentativeG
                    val h = Math.abs(neighbor % gridSize - goal % gridSize) + Math.abs(neighbor / gridSize - goal / gridSize)
                    openSet.add(Pair(neighbor, tentativeG + h))
                }
            }
            if (closedSet.size > 200) break // Cap pathfinding to keep it fast
        }
        return 0
    }

    // 5. AABB Collision
    private fun runAABBCollision(): Double {
        val count = 2000
        val minX = FloatArray(count) { it.toFloat() * 0.1f }
        val minY = FloatArray(count) { it.toFloat() * 0.1f }
        val minZ = FloatArray(count) { it.toFloat() * 0.1f }
        val maxX = FloatArray(count) { minX[it] + 1.0f }
        val maxY = FloatArray(count) { minY[it] + 1.0f }
        val maxZ = FloatArray(count) { minZ[it] + 1.0f }
        
        var overlaps = 0
        val startTime = System.nanoTime()
        // Pairwise checking a subset to keep it fast
        for (i in 0 until 500) {
            for (j in 0 until count) {
                if (maxX[i] >= minX[j] && minX[i] <= maxX[j] &&
                    maxY[i] >= minY[j] && minY[i] <= maxY[j] &&
                    maxZ[i] >= minZ[j] && minZ[i] <= maxZ[j]) {
                    overlaps++
                }
            }
        }
        val elapsed = (System.nanoTime() - startTime) / 1e9
        return (500.0 * count) / elapsed / 1e6 // M-checks/s
    }

    // 6. Scene Graph Traversal
    private fun runSceneGraphTraversal(): Double {
        class Node(var parent: Node? = null) {
            val localTransform = FloatArray(16) { 1f }
            val worldTransform = FloatArray(16) { 1f }
            val children = mutableListOf<Node>()
        }
        // Build 1000-node tree
        val root = Node()
        var current = root
        for (i in 1..1000) {
            val child = Node(current)
            current.children.add(child)
            if (i % 5 == 0) {
                current = child
            }
        }
        val startTime = System.nanoTime()
        var traversalCount = 0
        fun updateWorld(n: Node) {
            if (n.parent != null) {
                // Sim matrix multiply
                for (r in 0 until 4) {
                    for (c in 0 until 4) {
                        n.worldTransform[r * 4 + c] = n.parent!!.worldTransform[r * 4 + c] * n.localTransform[r * 4 + c]
                    }
                }
            }
            for (c in n.children) {
                updateWorld(c)
            }
        }
        for (pass in 0 until 50) {
            updateWorld(root)
            traversalCount++
        }
        val elapsed = (System.nanoTime() - startTime) / 1e9
        return (traversalCount * 1000.0) / elapsed / 1000.0 // k-traversals/s
    }

    // 7. Spatial Hash Grid
    private fun runSpatialHashGrid(): Double {
        val count = 2000
        val x = FloatArray(count) { it.toFloat() * 1.5f }
        val y = FloatArray(count) { it.toFloat() * 1.5f }
        val cellSize = 2.0f
        val grid = HashMap<Int, MutableList<Int>>()
        
        val startTime = System.nanoTime()
        for (pass in 0 until 50) {
            grid.clear()
            for (i in 0 until count) {
                val h = ((x[i] / cellSize).toInt() * 73856093) xor ((y[i] / cellSize).toInt() * 19349663)
                var list = grid[h]
                if (list == null) {
                    list = ArrayList()
                    grid[h] = list
                }
                list.add(i)
            }
        }
        val elapsed = (System.nanoTime() - startTime) / 1e9
        return (count.toDouble() * 50) / elapsed / 1e6 // M-ops/s
    }

    // 8. Skeletal Animation
    private fun runSkeletalAnimation(): Double {
        val bones = 64
        val matrices = Array(bones) { FloatArray(16) { 1f } }
        val boneParent = IntArray(bones) { it - 1 }
        val localPose = Array(bones) { FloatArray(16) { 1.1f } }
        val startTime = System.nanoTime()
        var boneUpdates = 0L
        for (pass in 0 until 1000) {
            for (i in 0 until bones) {
                if (boneParent[i] >= 0) {
                    val p = boneParent[i]
                    // matrix multiplication pose * parent
                    for (r in 0 until 4) {
                        for (c in 0 until 4) {
                            matrices[i][r * 4 + c] = localPose[i][r * 4 + c] * matrices[p][r * 4 + c]
                        }
                    }
                }
                boneUpdates++
            }
        }
        val elapsed = (System.nanoTime() - startTime) / 1e9
        return boneUpdates.toDouble() / elapsed / 1000.0 // k-bones/s
    }

    // 9. Terrain LOD Mesh
    private fun runTerrainLodMesh(): Double {
        val width = 64
        val height = 64
        val elevation = FloatArray(width * height)
        val startTime = System.nanoTime()
        var generatedVertices = 0L
        for (pass in 0 until 20) {
            for (y in 0 until height) {
                for (x in 0 until width) {
                    // Simple Perlin-like wave synthesis
                    elevation[y * width + x] = Math.sin(x * 0.1).toFloat() * Math.cos(y * 0.1).toFloat()
                    generatedVertices++
                }
            }
        }
        val elapsed = (System.nanoTime() - startTime) / 1e9
        return generatedVertices.toDouble() / elapsed // vertices/s
    }

    // 10. Audio Mixing
    private fun runAudioMixing(): Double {
        val bufferSize = 4096
        val channels = 16
        val sources = Array(channels) { FloatArray(bufferSize) { 0.1f } }
        val mixBuffer = FloatArray(bufferSize)
        val startTime = System.nanoTime()
        for (pass in 0 until 200) {
            mixBuffer.fill(0f)
            for (c in 0 until channels) {
                val src = sources[c]
                for (i in 0 until bufferSize) {
                    mixBuffer[i] += src[i]
                }
            }
        }
        val elapsed = (System.nanoTime() - startTime) / 1e9
        return (bufferSize.toDouble() * channels * 200) / elapsed / 1e6 // M-samples/s
    }

    // 11. Game Loop Jitter
    private fun runGameLoopJitter(): Double {
        val frameTargetNs = 16666666L // 60 FPS
        var cumulativeJitterMs = 0.0
        var prevTime = System.nanoTime()
        for (i in 0 until 50) {
            // Spin-wait to simulate workload + frame completion
            var curTime = System.nanoTime()
            while (curTime - prevTime < frameTargetNs / 100) { // sleep-simulate
                curTime = System.nanoTime()
            }
            val elapsed = curTime - prevTime
            val jitter = Math.abs(elapsed - (frameTargetNs / 100)) / 1e6
            cumulativeJitterMs += jitter
            prevTime = curTime
        }
        return cumulativeJitterMs / 50.0 // average jitter in ms (lower is better, inverted score)
    }

    // 12. Entity Component System
    private fun runECSQuery(): Double {
        val count = 20000
        val active = BooleanArray(count) { it % 2 == 0 }
        val posX = FloatArray(count)
        val posY = FloatArray(count)
        val velX = FloatArray(count) { 1.5f }
        val velY = FloatArray(count) { 1.5f }
        
        val startTime = System.nanoTime()
        for (tick in 0 until 100) {
            for (i in 0 until count) {
                if (active[i]) {
                    posX[i] += velX[i]
                    posY[i] += velY[i]
                }
            }
        }
        val elapsed = (System.nanoTime() - startTime) / 1e9
        return (count.toDouble() / 2 * 100) / elapsed / 1e6 // M-updates/s
    }

    // 13. Frustum Culling
    private fun runFrustumCulling(): Double {
        val count = 20000
        val cx = FloatArray(count) { it.toFloat() * 0.1f }
        val cy = FloatArray(count) { it.toFloat() * 0.1f }
        val cz = FloatArray(count) { it.toFloat() * 0.1f }
        val r = FloatArray(count) { 1.0f }
        
        // Frustum planes
        val px = 0f; val py = 0f; val pz = 1f; val pd = -10f
        var inside = 0
        val startTime = System.nanoTime()
        for (pass in 0 until 20) {
            for (i in 0 until count) {
                val dist = cx[i] * px + cy[i] * py + cz[i] * pz + pd
                if (dist + r[i] >= 0) {
                    inside++
                }
            }
        }
        val elapsed = (System.nanoTime() - startTime) / 1e9
        return (count.toDouble() * 20) / elapsed / 1e6 // M-checks/s
    }

    // 14. Octree Build & Query
    private fun runOctreeBuildAndQuery(): Double {
        class OctreeNode(val minX: Float, val minY: Float, val minZ: Float, val maxX: Float, val maxY: Float, val maxZ: Float) {
            val children = Array<OctreeNode?>(8) { null }
            val points = mutableListOf<FloatArray>()
        }
        
        val random = Random(42)
        val root = OctreeNode(-100f, -100f, -100f, 100f, 100f, 100f)
        
        val startTime = System.nanoTime()
        var ops = 0
        for (i in 0 until 2000) {
            val p = floatArrayOf(random.nextFloat() * 190f - 95f, random.nextFloat() * 190f - 95f, random.nextFloat() * 190f - 95f)
            root.points.add(p)
            ops++
        }
        // Subdivide dummy
        val midX = 0f; val midY = 0f; val midZ = 0f
        for (i in 0 until 8) {
            root.children[i] = OctreeNode(midX, midY, midZ, 100f, 100f, 100f)
            ops++
        }
        val elapsed = (System.nanoTime() - startTime) / 1e9
        return ops.toDouble() / elapsed // ops/s
    }

    // 15. Procedural Noise
    private fun runProceduralNoise(): Double {
        val w = 256
        val h = 256
        val data = FloatArray(w * h)
        val startTime = System.nanoTime()
        for (pass in 0 until 10) {
            for (y in 0 until h) {
                for (x in 0 until w) {
                    // Simple sin/cos noise
                    data[y * w + x] = Math.sin(x * 0.05 + y * 0.05).toFloat()
                }
            }
        }
        val elapsed = (System.nanoTime() - startTime) / 1e9
        return (w.toDouble() * h * 10) / elapsed / 1e6 // M-pixels/s
    }

    // 16. State Machine AI
    private fun runStateMachineAI(): Double {
        val count = 20000
        val state = IntArray(count) { it % 4 }
        val startTime = System.nanoTime()
        var transitions = 0L
        for (tick in 0 until 50) {
            for (i in 0 until count) {
                val s = state[i]
                state[i] = when (s) {
                    0 -> 1
                    1 -> 2
                    2 -> 3
                    else -> 0
                }
                transitions++
            }
        }
        val elapsed = (System.nanoTime() - startTime) / 1e9
        return transitions.toDouble() / elapsed / 1e6 // M-transitions/s
    }

    // 17. Navmesh Raycast
    private fun runNavmeshRaycast(): Double {
        val castCount = 10000
        val ox = FloatArray(castCount) { it.toFloat() * 0.01f }
        val oy = FloatArray(castCount) { it.toFloat() * 0.01f }
        val tx = FloatArray(castCount) { it.toFloat() * 0.02f }
        val ty = FloatArray(castCount) { it.toFloat() * 0.02f }
        
        val boundaryL = -50f; val boundaryR = 50f
        var hitCount = 0
        val startTime = System.nanoTime()
        for (pass in 0 until 5) {
            for (i in 0 until castCount) {
                if (ox[i] >= boundaryL && tx[i] <= boundaryR) {
                    hitCount++
                }
            }
        }
        val elapsed = (System.nanoTime() - startTime) / 1e9
        return (castCount.toDouble() * 5) / elapsed / 1000.0 // k-casts/s
    }

    // 18. Input Buffer
    private fun runInputBuffer(): Double {
        val queue = java.util.ArrayDeque<Int>()
        val startTime = System.nanoTime()
        var processed = 0
        for (pass in 0 until 10) {
            for (i in 0 until 50000) {
                queue.add(i)
            }
            while (queue.isNotEmpty()) {
                val item = queue.poll()
                processed++
            }
        }
        val elapsed = (System.nanoTime() - startTime) / 1e9
        return processed.toDouble() / elapsed / 1e6 // M-events/s
    }

    // 19. Behavior Tree AI
    private fun runBehaviorTreeAI(): Double {
        val agentCount = 5000
        var ticks = 0L
        val startTime = System.nanoTime()
        for (tick in 0 until 20) {
            for (i in 0 until agentCount) {
                // Sim selector / sequence logic of depth 5
                val conditionA = i % 2 == 0
                val conditionB = i % 3 == 0
                if (conditionA) {
                    if (conditionB) {
                        ticks++ // Action 1
                    } else {
                        ticks += 2 // Action 2
                    }
                } else {
                    ticks += 3 // Action 3
                }
            }
        }
        val elapsed = (System.nanoTime() - startTime) / 1e9
        return ticks.toDouble() / elapsed / 1000.0 // k-ticks/s
    }

    // 20. Memory Pool Allocator
    private fun runMemoryPoolAllocator(): Double {
        class Block(var active: Boolean)
        val pool = Array(1000) { Block(false) }
        val startTime = System.nanoTime()
        var allocs = 0L
        for (pass in 0 until 1000) {
            for (i in 0 until 1000) {
                pool[i].active = true
                allocs++
            }
            for (i in 0 until 1000) {
                pool[i].active = false
                allocs++
            }
        }
        val elapsed = (System.nanoTime() - startTime) / 1e9
        return allocs.toDouble() / elapsed / 1e6 // M-allocs/s
    }
}
