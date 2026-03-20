package pt.isel.pc.leic41n.sync

import java.time.Duration
import java.util.LinkedList
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * N-Ary
 */
class FairSemaphore2(
    initialUnits: Int,
) {
    private val mutex = ReentrantLock()
    private val acquireRequests = LinkedList<AcquireRequest>()
    private var units = initialUnits

    private class AcquireRequest(
        val requestedUnits: Int,
        val condition: Condition,
    ) {
        // No request specific data
        var isDone: Boolean = false
    }

    @Throws(InterruptedException::class)
    fun tryAcquire(
        requestedUnits: Int,
        timeout: Duration,
    ): Boolean {
        require(requestedUnits > 0)
        mutex.withLock {
            // fast-path
            if (units >= requestedUnits && acquireRequests.isEmpty()) {
                units -= requestedUnits
                return true
            }
            var remainingNanos = timeout.toNanos()
            if (remainingNanos <= 0) {
                return false
            }
            // wait-path
            val request = AcquireRequest(
                requestedUnits,
                mutex.newCondition(),
            )
            acquireRequests.addLast(request)
            while (true) {
                try {
                    remainingNanos = request.condition.awaitNanos(remainingNanos)
                } catch (ie: InterruptedException) {
                    if (request.isDone) {
                        Thread.currentThread().interrupt()
                        return true
                    }
                    // giving-up
                    acquireRequests.remove(request)
                    completeAllPossible()
                    throw ie
                }
                if (request.isDone) {
                    return true
                }
                if (remainingNanos <= 0) {
                    // giving-up
                    acquireRequests.remove(request)
                    completeAllPossible()
                    return false
                }
            }
        }
    }

    fun release(releaseUnits: Int) {
        require(releaseUnits > 0)
        mutex.withLock {
            units += releaseUnits
            completeAllPossible()
        }
    }

    private fun completeAllPossible() {
        while (true) {
            val headRequest = acquireRequests.peekFirst()
                ?: return
            if (units < headRequest.requestedUnits) {
                return
            }
            acquireRequests.removeFirst()
            units -= headRequest.requestedUnits
            headRequest.isDone = true
            headRequest.condition.signal()
        }
    }
}
