package pt.isel.pc.leic41n.sync

import java.time.Duration
import java.util.LinkedList
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * A synchronous queue without capacity ("rendezvous" queue).
 */
class SynchronousQueue<T> {

    class ReadRequest<T>(
        val condition: Condition,
    ) {
        var message: T? = null
    }

    class WriteRequest<T>(
        val condition: Condition,
        val message: T,
    ) {
        var isDone: Boolean = false
    }

    private val mutex = ReentrantLock()
    private val writeRequests = LinkedList<WriteRequest<T>>()
    private val readRequests = LinkedList<ReadRequest<T>>()

    @Throws(InterruptedException::class)
    fun write(
        value: T,
        timeout: Duration,
    ): Boolean {
        mutex.withLock {
            // fast-path
            if (readRequests.isNotEmpty()) {
                val readRequest = readRequests.removeFirst()
                readRequest.message = value
                readRequest.condition.signal()
                return true
            }
            var remainingNanos = timeout.toNanos()
            if (remainingNanos < 0) {
                return false
            }
            // wait-path
            val selfRequest = WriteRequest(
                mutex.newCondition(),
                value,
            )
            writeRequests.addLast(selfRequest)
            while (true) {
                try {
                    remainingNanos = selfRequest.condition.awaitNanos(remainingNanos)
                } catch (ex: InterruptedException) {
                    if (selfRequest.isDone) {
                        Thread.currentThread().interrupt()
                        return true
                    }
                    writeRequests.remove(selfRequest)
                    throw ex
                }
                if (selfRequest.isDone) {
                    return true
                }
                if (remainingNanos < 0) {
                    writeRequests.remove(selfRequest)
                    return false
                }
            }
        }
    }

    @Throws(InterruptedException::class)
    fun read(timeout: Duration): T? {
        mutex.withLock {
            // fast-path
            if (writeRequests.isNotEmpty()) {
                val writeRequest = writeRequests.removeFirst()
                writeRequest.isDone
                writeRequest.condition.signal()
                return writeRequest.message
            }
            var remainingNanos = timeout.toNanos()
            if (remainingNanos < 0) {
                return null
            }
            // wait-path
            val selfRequest = ReadRequest<T>(
                mutex.newCondition(),
            )
            readRequests.addLast(selfRequest)
            while (true) {
                try {
                    remainingNanos = selfRequest.condition.awaitNanos(remainingNanos)
                } catch (ex: InterruptedException) {
                    if (selfRequest.message != null) {
                        Thread.currentThread().interrupt()
                        return selfRequest.message
                    }
                    readRequests.remove(selfRequest)
                    throw ex
                }
                if (selfRequest.message != null) {
                    return selfRequest.message
                }
                if (remainingNanos < 0) {
                    readRequests.remove(selfRequest)
                    return null
                }
            }
        }
    }
}
