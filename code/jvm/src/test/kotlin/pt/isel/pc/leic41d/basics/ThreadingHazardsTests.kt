package pt.isel.pc.leic41d.basics

import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.BeforeAll
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertNotEquals

class ThreadingHazardsTests {

    @Test
    fun `missing increments`() {
        var sharedCounter = 0
        val nOfThreads = 8
        val nOfReps = 100_000
        val threads = List(nOfThreads) {
            Thread.ofPlatform().start {
                repeat(nOfReps) {
                    sharedCounter += 1
                }
            }
        }
        threads.forEach { thread ->
            thread.join()
        }
        assertNotEquals(nOfThreads * nOfReps, sharedCounter)
    }

    @Test
    fun `check-then-act`() {
        val map = ConcurrentHashMap<Int, AtomicInteger>()
        val nOfThreads = 2
        val nOfReps = 100_000
        val threads = List(nOfThreads) {
            Thread.ofPlatform().start {
                repeat(nOfReps) { index ->
                    val counter = map[index]
                    if (counter == null) {
                        map[index] = AtomicInteger(1)
                    } else {
                        counter.incrementAndGet()
                    }
                }
            }
        }
        threads.forEach { thread ->
            thread.join()
        }
        val sum = map.values.fold(0) { acc, ai ->
            acc + ai.get()
        }
        assertNotEquals(nOfThreads * nOfReps, sum)
    }

    companion object {

        private val logger = LoggerFactory.getLogger(ThreadingHazardsTests::class.java)

        @BeforeAll
        @JvmStatic
        fun checkRequirements() {
            // These tests fail more frequently if running on system with only 1 processor (e.g. CI)
            val nOfProcessors = Runtime.getRuntime().availableProcessors()
            logger.info("Available processors: {}", nOfProcessors)
            assumeTrue(
                nOfProcessors > 1,
                "Requires a minimum number of processors, otherwise the failure rate is high",
            )
            logger.info("Requirements are fulfilled")
        }
    }
}
