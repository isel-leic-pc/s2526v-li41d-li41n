package pt.isel.pc.leic41n.ksync

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume

class KSemaphore2(
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
    private val mutex = Mutex()
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
            withContext(NonCancellable) {
                mutex.withLock {
                    val removed = acquireRequests.remove(myRequest)
                    if (removed) {
                        // Request was not already fulfilled
                        // Possible to give-up
                        throw ex
                    } else {
                        // Request was already fulfilled
                        // *Not* possible to give-up
                        return@withLock
                    }
                }
            }
        }
    }

    suspend fun release() {
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
