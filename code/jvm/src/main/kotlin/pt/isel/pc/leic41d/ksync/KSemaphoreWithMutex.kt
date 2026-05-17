package pt.isel.pc.leic41d.ksync

import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume

class KSemaphoreWithMutex(
    initialUnits: Int,
) {
    init {
        require(initialUnits >= 0)
    }

    // state
    private val mutex = Mutex()
    private var units = initialUnits
    private val acquireContinuations = mutableListOf<Continuation<Unit>>()

    // can throw CancellationException
    suspend fun acquire() {
        mutex.lock()
        // fast-path
        if (units > 0) {
            units -= 1
            mutex.unlock()
        }
        // resume-path
        var myContinuation: Continuation<Unit>? = null
        try {
            suspendCancellableCoroutine<Unit> { continuation ->
                myContinuation = continuation
                acquireContinuations.addLast(continuation)
                // IMPORTANT: assumes that the block passed to `suspendCancellableCoroutine`
                // is *always* called, even if the coroutine is already canceled.
                mutex.unlock()
            }
        } catch (ex: Exception) {
            withContext(NonCancellable) {
                mutex.withLock {
                    val removed = acquireContinuations.remove(myContinuation)
                    if (removed) {
                        // means that the unit was not yet granted
                        // not too late to give-up
                        throw ex
                    } else {
                        // means that the unit was already granted
                        // too late to give-up
                        return@withLock
                    }
                }
            }
        }
    }

    suspend fun release() {
        val maybeContinuation: Continuation<Unit>? = mutex.withLock {
            units += 1
            if (acquireContinuations.isNotEmpty()) {
                val firstContinuation = acquireContinuations.removeFirst()
                units -= 1
                return@withLock firstContinuation
            } else {
                return@withLock null
            }
        }
        maybeContinuation?.resume(Unit)
    }
}
