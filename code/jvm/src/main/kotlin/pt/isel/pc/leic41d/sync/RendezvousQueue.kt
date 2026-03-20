package pt.isel.pc.leic41d.sync

import java.time.Duration
import java.util.LinkedList
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class RendezvousQueue<T> {

    private class PutRequest<T>(
        val condition: Condition,
        val message: T,
    ) {
        var isDone: Boolean = false
    }

    private class GetRequest<T>(
        val condition: Condition,
    ) {
        var message: T? = null
    }

    private val mutex = ReentrantLock()
    private val putRequests = LinkedList<PutRequest<T>>()
    private val getRequests = LinkedList<GetRequest<T>>()

    @Throws(InterruptedException::class)
    fun put(
        message: T,
        timeout: Duration,
    ): Boolean {
        mutex.withLock {
            // fast-path
            if (getRequests.isNotEmpty()) {
                val first = getRequests.removeFirst()
                first.message = message
                first.condition.signal()
                return true
            }
            var remainingNanos = timeout.toNanos()
            if (remainingNanos <= 0) {
                return false
            }
            // wait-path
            val selfRequest = PutRequest(
                condition = mutex.newCondition(),
                message = message,
            )
            putRequests.addLast(selfRequest)
            while (true) {
                try {
                    remainingNanos = selfRequest.condition.awaitNanos(remainingNanos)
                } catch (ie: InterruptedException) {
                    if (selfRequest.isDone) {
                        Thread.currentThread().interrupt()
                        return true
                    }
                    putRequests.remove(selfRequest)
                    throw ie
                }
                if (selfRequest.isDone) {
                    return true
                }
                if (remainingNanos <= 0) {
                    putRequests.remove(selfRequest)
                    return false
                }
            }
        }
    }

    @Throws(InterruptedException::class)
    fun get(timeout: Duration): T? {
        mutex.withLock {
            // fast-path
            if (putRequests.isNotEmpty()) {
                val first = putRequests.removeFirst()
                first.isDone = true
                first.condition.signal()
                return first.message
            }
            var remainingNanos = timeout.toNanos()
            if (remainingNanos <= 0) {
                return null
            }
            // wait-path
            val selfRequest = GetRequest<T>(condition = mutex.newCondition())
            getRequests.addLast(selfRequest)
            while (true) {
                try {
                    remainingNanos = selfRequest.condition.awaitNanos(remainingNanos)
                } catch (ie: InterruptedException) {
                    if (selfRequest.message != null) {
                        Thread.currentThread().interrupt()
                        return selfRequest.message
                    }
                    getRequests.remove(selfRequest)
                    throw ie
                }
                if (selfRequest.message != null) {
                    return selfRequest.message
                }
                if (remainingNanos <= 0) {
                    getRequests.remove(selfRequest)
                    return null
                }
            }
        }
    }
}
