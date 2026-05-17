package pt.isel.pc.leic41d.ksync

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume

class KSemaphore(
    initialUnits: Int,
) {
    init {
        require(initialUnits >= 0)
    }

    // state
    private val mutex = ReentrantLock()
    private var units = initialUnits
    private val acquireContinuations = mutableListOf<Continuation<Unit>>()

    // can throw CancellationException
    suspend fun acquire() {
        // can throw CancellationException
        var maybeContinuation: Continuation<Unit>? = null
        var completedOnFastPath = false
        try {
            suspendCancellableCoroutine<Unit> { continuation ->
                maybeContinuation = continuation
                mutex.withLock {
                    if (units > 0) {
                        units -= 1
                        completedOnFastPath = true
                        continuation.resume(Unit)
                    } else {
                        acquireContinuations.addLast(continuation)
                    }
                }
            }
        } catch (ex: CancellationException) {
            // Check if continuation is in the list
            // If so, remove it and throw
            // Otherwise, return success
            if (completedOnFastPath) {
                return
            }
            if (maybeContinuation == null) {
                throw ex
            }
            mutex.withLock {
                if (acquireContinuations.contains(maybeContinuation)) {
                    acquireContinuations.remove(maybeContinuation)
                    throw ex
                } else {
                    return
                }
            }
        }
    }

    fun release() {
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
