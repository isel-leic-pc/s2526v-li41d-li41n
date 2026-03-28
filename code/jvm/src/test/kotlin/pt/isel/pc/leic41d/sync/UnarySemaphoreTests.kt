package pt.isel.pc.leic41d.sync

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.assertThrows
import java.time.Duration
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UnarySemaphoreTests {

    @Test
    fun `can acquire from semaphore with unit`() {
        // given: a semaphore with one unit
        val semaphore = FairSemaphoreUsingKernelStyle(1)

        // when: acquiring a unit without waiting
        val res = semaphore.acquire(Duration.ZERO)

        // then: the unit was acquired
        assertTrue(res)
    }

    @Test
    fun `cannot acquire from semaphore without units`() {
        // given: a semaphore without units
        val semaphore = FairSemaphoreUsingKernelStyle(0)

        // when: acquiring a unit without waiting
        val res = semaphore.acquire(Duration.ZERO)

        // then: the unit is not acquired
        assertFalse(res)
    }

    @Test
    fun `can add and acquire a unit, even without waiting`() {
        // given: a semaphore without units
        val semaphore = FairSemaphoreUsingKernelStyle(0)

        // when: adding a unit
        semaphore.release()

        // and: acquiring a unit
        val res = semaphore.acquire(Duration.ZERO)

        // then: acquire is successfully
        assertTrue(res)
    }

    @Test
    fun `capturing exceptions thrown on created threads, version 0`() {
        assertThrows<AssertionError> {
            val capturedExceptions = ConcurrentLinkedQueue<Throwable>()
            val thread = Thread.ofPlatform().start {
                try {
                    assertEquals(1, 0)
                } catch (ex: Throwable) {
                    capturedExceptions.add(ex)
                }
            }
            thread.join()
            if (capturedExceptions.isNotEmpty()) {
                val first = capturedExceptions.peek()
                throw first
            }

        }
    }

    @Test
    fun `capturing exceptions thrown on created threads, version 1`() {
        assertThrows<AssertionError> {
            TestHelper(Thread.ofPlatform()).runTest {
                it.newThread {
                    assertEquals(1, 0)
                }
            }
        }
    }

    @Test
    fun `two threads can release and acquire`() {
        TestHelper(Thread.ofPlatform()).runTest {
            // given: a semaphore without units
            val semaphore = FairSemaphoreUsingKernelStyle(0)

            // when: starting a thread that acquires
            it.newThread {
                val res = semaphore.acquire(Duration.ofSeconds(3600))
                assertTrue(res)
            }
            // and: starting a thread that releases
            it.newThread {
                semaphore.release()
            }

            // then: the test complete without error
        }
    }

    @Test
    fun `stress test`() {
        val nOfThreads = 16
        val maxUnits = nOfThreads / 2
        val nOfIterations = 100_000
        val counter = AtomicInteger(maxUnits)
        val semaphore = FairSemaphoreUsingKernelStyle(maxUnits)
        TestHelper(Thread.ofPlatform()).runTest { testHelper ->
            repeat(nOfThreads) {
                testHelper.newThread {
                    repeat(nOfIterations) {
                        val res = semaphore.acquire(Duration.ofSeconds(5))
                        assertTrue(res)
                        var observedCounter = counter.decrementAndGet()
                        assertTrue(observedCounter >= 0)
                        Thread.yield()
                        observedCounter = counter.incrementAndGet()
                        assertTrue(observedCounter <= maxUnits)
                        semaphore.release()
                    }
                }
            }
        }
    }
}
