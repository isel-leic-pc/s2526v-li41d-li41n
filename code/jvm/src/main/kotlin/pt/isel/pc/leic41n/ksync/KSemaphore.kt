package pt.isel.pc.leic41n.ksync

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class KSemaphore(
    initialUnits: Int,
) {
    init {
        require(initialUnits >= 0)
    }

    private class AcquireRequest(
        val continuation: Continuation<Unit>,
    )

    // shared state
    private var units = initialUnits
    private val mutex = ReentrantLock()
    private val acquireRequests = mutableListOf<AcquireRequest>()

    suspend fun acquire() {
        mutex.lock()
        // fast-path
        if (units > 0) {
            units -= 1
            mutex.unlock()
            return
        }

        // suspend-path
        // can end by throwing CancelledException
        var myRequest: AcquireRequest? = null
        try {
            suspendCancellableCoroutine<Unit> { continuation ->
                myRequest = AcquireRequest(continuation)
                acquireRequests.addLast(
                    myRequest,
                )
                mutex.unlock()
            }
        } catch (ex: CancellationException) {
            if (myRequest == null) {
                mutex.unlock()
            }
            mutex.withLock {
                val removed = acquireRequests.remove(myRequest)
                if (removed) {
                    // Request was not already fulfilled
                    // Possible to give-up
                    throw ex
                } else {
                    // Request was already fulfilled
                    // *Not* possible to give-up
                    return
                }
            }
        }
    }

    // Incomplete!!!
    suspend fun acquireAlternative() {
        suspendCoroutine<Unit> { continuation ->
            mutex.withLock {
                // fast-path
                if (units > 0) {
                    units -= 1
                    continuation.resume(Unit)
                } else {
                    acquireRequests.addLast(
                        AcquireRequest(
                            continuation,
                        ),
                    )
                }
            }
        }
    }

    fun release() {
        val maybeContinuation: Continuation<Unit>? = mutex.withLock {
            if (acquireRequests.isNotEmpty()) {
                val firstRequest = acquireRequests.removeFirst()
                return@withLock firstRequest.continuation
            } else {
                units += 1
                return@withLock null
            }
        }
        maybeContinuation?.resume(Unit)
    }
}
