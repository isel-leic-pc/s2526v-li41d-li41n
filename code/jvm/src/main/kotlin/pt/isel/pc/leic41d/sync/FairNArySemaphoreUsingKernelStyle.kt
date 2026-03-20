package pt.isel.pc.leic41d.sync

import java.time.Duration
import java.util.LinkedList
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class FairNArySemaphoreUsingKernelStyle(
    initialUnits: Int,
) {
    init {
        require(initialUnits > 0)
    }

    class AcquireRequest(
        val condition: Condition,
        val unitsToAcquire: Int,
    ) {
        var isDone: Boolean = false
    }

    private val mutex = ReentrantLock()
    private var units = initialUnits
    private val acquireRequests = LinkedList<AcquireRequest>()

    @Throws(InterruptedException::class)
    fun acquire(
        unitsToAcquire: Int,
        timeout: Duration,
    ): Boolean {
        mutex.withLock {
            // fast-path
            if (units >= unitsToAcquire && acquireRequests.isEmpty()) {
                units -= unitsToAcquire
                return true
            }
            var remainingNanos = timeout.toNanos()
            if (remainingNanos <= 0) {
                return false
            }
            // wait-path
            val selfRequest = AcquireRequest(
                mutex.newCondition(),
                unitsToAcquire,
            )
            acquireRequests.addLast(selfRequest)

            while (true) {
                try {
                    remainingNanos = selfRequest.condition.awaitNanos(remainingNanos)
                } catch (ex: InterruptedException) {
                    if (selfRequest.isDone) {
                        // re-set interrupt flag
                        Thread.currentThread().interrupt()
                        return true
                    }
                    acquireRequests.remove(selfRequest)
                    completeAllPossible()
                    throw ex
                }
                if (selfRequest.isDone) {
                    return true
                }
                if (remainingNanos <= 0) {
                    acquireRequests.remove(selfRequest)
                    completeAllPossible()
                    return false
                }
            }
        }
    }

    fun release(unitsToRelease: Int) {
        mutex.withLock {
            units += unitsToRelease
            completeAllPossible()
        }
    }

    private fun completeAllPossible() {
        while (acquireRequests.isNotEmpty() &&
            units >= acquireRequests.peekFirst().unitsToAcquire
        ) {
            val first = acquireRequests.removeFirst()
            units -= first.unitsToAcquire
            first.isDone = true
            first.condition.signal()
        }
    }
}
