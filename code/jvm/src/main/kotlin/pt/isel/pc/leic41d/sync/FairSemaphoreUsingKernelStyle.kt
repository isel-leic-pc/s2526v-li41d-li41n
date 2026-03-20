package pt.isel.pc.leic41d.sync

import java.time.Duration
import java.util.LinkedList
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class FairSemaphoreUsingKernelStyle(
    initialUnits: Int,
) {
    init {
        require(initialUnits > 0)
    }

    class AcquireRequest(
        val condition: Condition,
    ) {
        var isDone: Boolean = false
    }

    private val mutex = ReentrantLock()
    private var units = initialUnits
    private val acquireRequests = LinkedList<AcquireRequest>()

    @Throws(InterruptedException::class)
    fun acquire(timeout: Duration): Boolean {
        mutex.withLock {
            // fast-path
            if (units > 0 && acquireRequests.isEmpty()) {
                units -= 1
                return true
            }
            var remainingNanos = timeout.toNanos()
            if (remainingNanos <= 0) {
                return false
            }
            // wait-path
            val selfRequest = AcquireRequest(mutex.newCondition())
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
                    throw ex
                }
                if (selfRequest.isDone) {
                    return true
                }
                if (remainingNanos <= 0) {
                    acquireRequests.remove(selfRequest)
                    return false
                }
            }
        }
    }

    fun release() {
        mutex.withLock {
            if (acquireRequests.isNotEmpty()) {
                val firstRequest = acquireRequests.removeFirst()
                firstRequest.isDone = true
                firstRequest.condition.signal()
            } else {
                units += 1
            }
        }
    }
}
