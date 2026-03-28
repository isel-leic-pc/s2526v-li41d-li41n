package pt.isel.pc.leic41n.sync

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.assertThrows
import java.time.Duration
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SemaphoreTests {

    @Test
    fun `simple single-thread acquisition with units`() {
        // given: a semaphore with one unit
        val semaphore = FairSemaphore1(1)

        // when: acquiring a unit without waiting
        val acquired = semaphore.tryAcquire(Duration.ZERO)

        // then: the acquisition is successful
        assertTrue(acquired)
    }

    @Test
    fun `simple single-thread acquisition without units`() {
        // given: a semaphore with zero units
        val semaphore = FairSemaphore1(0)

        // when: acquiring a unit without waiting
        val acquired = semaphore.tryAcquire(Duration.ZERO)

        // then: the acquisition is NOT successful
        assertFalse(acquired)
    }

    @Test
    fun `can acquire unit after a release when starting without units`() {
        // given: a semaphore with zero units
        val semaphore = FairSemaphore1(0)

        // when: adding a unit
        semaphore.release()

        // and: acquiring a unit
        val acquired = semaphore.tryAcquire(Duration.ZERO)

        // then: the acquisition is successful
        assertTrue(acquired)
    }

    @Test
    fun `multi-threaded test, version 0`() {
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
                val firstException = capturedExceptions.peek()
                throw firstException
            }
        }
    }

    @Test
    fun `multi-threaded test, version 1`() {
        assertThrows<AssertionError> {
            TestHelper(Thread.ofPlatform()).runTest { testHelper ->
                testHelper.startThread {
                    assertEquals(1, 0)
                }
            }

        }
    }

    @Test
    fun `can acquire and release in different threads`() {
        // given: a semaphore without units
        val semaphore = FairSemaphore1(0)

        // and: a test helper
        TestHelper(Thread.ofPlatform()).runTest { testHelper ->
            testHelper.startThread {
                // and (when): there is an acquisition
                val acquired = semaphore.tryAcquire(Duration.ofMinutes(5))

                // then: the acquisition is successful
                assertTrue(acquired)
            }

            testHelper.startThread {
                // when: there is a release
                semaphore.release()
            }
        }
    }

    @Test
    fun `semaphore stress test`() {
        // given: some testing constants
        val nOfThreads = 16
        val nOfInitialUnits = nOfThreads / 2
        val nOfReps = 100_000

        // and: a semaphore with nOfInitialUnis units
        val semaphore = FairSemaphore1(nOfInitialUnits)

        // and: a counter to track the provided units
        val counter = AtomicInteger(nOfInitialUnits)

        // and: a test helper
        TestHelper(Thread.ofPlatform()).runTest { testHelper ->
            repeat(nOfThreads) {
                testHelper.startThread {
                    repeat(nOfReps) {
                        // when: acquiring a unit
                        val acquired = semaphore.tryAcquire(Duration.ofMinutes(1))
                        assertTrue(acquired)
                        // and: decrementing a counter
                        var observedCounter = counter.decrementAndGet()

                        // then: the counter is not negative, i.e., the initial units were not exceeded
                        assertTrue(observedCounter >= 0)

                        Thread.yield()
                        observedCounter = counter.incrementAndGet()
                        assertTrue(observedCounter <= nOfInitialUnits)
                        semaphore.release()
                    }
                }
            }
        }
    }
}
