package pt.isel.pc.leic41n.sync

import java.util.LinkedList
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class FairSemaphore0(
    initialUnits: Int,
) {
    private val mutex = ReentrantLock()
    private val condition = mutex.newCondition()
    private val awaitingThreads = LinkedList<Thread>()
    private var units = initialUnits

    fun acquire() {
        mutex.withLock {
            // fast-path
            if (units > 0 && awaitingThreads.isEmpty()) {
                units -= 1
                return
            }
            // wait-path
            awaitingThreads.addLast(Thread.currentThread())
            while (true) {
                condition.await()
                if (units > 0 && Thread.currentThread() == awaitingThreads.peekFirst()) {
                    units -= 1
                    awaitingThreads.remove(Thread.currentThread())
                    if (units > 0 && awaitingThreads.isNotEmpty()) {
                        condition.signalAll()
                    }
                    return
                }
            }
        }
    }

    fun release() {
        mutex.withLock {
            units += 1
            if (awaitingThreads.isNotEmpty()) {
                condition.signalAll()
            }
        }
    }
}
