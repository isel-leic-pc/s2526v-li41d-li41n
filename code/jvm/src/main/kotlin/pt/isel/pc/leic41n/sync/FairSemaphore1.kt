package pt.isel.pc.leic41n.sync

import java.util.LinkedList
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class FairSemaphore1(
    initialUnits: Int,
) {
    private val mutex = ReentrantLock()
    private val acquireRequests = LinkedList<AcquireRequest>()
    private var units = initialUnits

    private class AcquireRequest(
        val condition: Condition,
    ) {
        // No request specific data
        var isDone: Boolean = false
    }

    fun acquire() {
        mutex.withLock {
            // fast-path
            if (units > 0) {
                units -= 1
                return
            }
            val request = AcquireRequest(mutex.newCondition())
            acquireRequests.addLast(request)
            while (true) {
                try {
                    request.condition.await()
                } catch (ie: InterruptedException) {
                    if (request.isDone) {
                        Thread.currentThread().interrupt()
                        return
                    }
                    // giving-up
                    acquireRequests.remove(request)
                    throw ie
                }
                if (request.isDone) {
                    return
                }
            }
        }
    }

    fun release() {
        mutex.withLock {
            if (acquireRequests.isNotEmpty()) {
                val headRequest = acquireRequests.pollFirst()
                headRequest.isDone = true
                headRequest.condition.signal()
            } else {
                units += 1
            }
        }
    }
}
