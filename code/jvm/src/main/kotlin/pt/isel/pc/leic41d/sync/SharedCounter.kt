package pt.isel.pc.leic41d.sync

import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class SharedCounter {
    private var counter: Long = 0
    private val mutex: Lock = ReentrantLock()

    fun inc() {
        mutex.withLock {
            counter += 1
        }
    }

    fun dec() {
        mutex.withLock {
            counter -= 1
        }
    }

    fun read(): Long =
        mutex.withLock {
            counter
        }
}
