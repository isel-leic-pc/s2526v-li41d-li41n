package pt.isel.pc.leic41n.basics

import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.BeforeAll
import org.slf4j.LoggerFactory
import pt.isel.pc.leic41d.basics.ThreadingHazardsTests
import java.util.LinkedList
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.test.Test
import kotlin.test.assertNotEquals
import kotlin.test.fail

class ThreadingHazardsTests {

    @Test
    fun `missing updates`() {
        var counter = 0
        val nOfThreads = 16
        val nOfReps = 100_000
        val threads: List<Thread> = List(nOfThreads) {
            Thread.ofPlatform().start {
                repeat(nOfReps) {
                    counter += 1
                }
            }
        }
        threads.forEach { thread ->
            thread.join()
        }
        assertNotEquals(nOfThreads * nOfReps, counter)
    }

    @Test
    fun `concurrency on a list`() {
        val list = LinkedList<Int>()
        val nOfThreads = 2
        val nOfReps = 100_000
        val threads = List(nOfThreads) {
            Thread.ofPlatform().start {
                repeat(nOfReps) {
                    list.add(1)
                }
            }
        }

        threads.forEach {
            it.join()
        }

        try {
            val sum = list.reduce { acc, i -> acc + i }
            assertNotEquals(nOfThreads * nOfReps, sum)
            fail() // should not reach this point
        } catch (_: NullPointerException) {
            // May throw NPE because the list is not in a consistent state.
        }
    }

    @Test
    fun `check then act`() {
        val map = ConcurrentHashMap<Int, ConcurrentLinkedQueue<Int>>()
        val nOfThreads = 2
        val nOfReps = 100_000
        val threads = List(nOfThreads) {
            Thread.ofPlatform().start {
                repeat(nOfReps) { index ->
                    val queue = map[index]
                    if (queue == null) {
                        val queue = ConcurrentLinkedQueue<Int>()
                        queue.add(1)
                        map[index] = queue
                    } else {
                        queue.add(1)
                    }
                }
            }
        }
        threads.forEach {
            it.join()
        }
        val sum = map.entries.fold(0) { acc, entry ->
            acc + entry.value.size
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
