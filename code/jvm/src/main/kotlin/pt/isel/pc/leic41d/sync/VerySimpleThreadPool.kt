package pt.isel.pc.leic41d.sync

import org.slf4j.LoggerFactory
import java.util.LinkedList
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class VerySimpleThreadPool(
    private val maxThreads: Int,
) {

    init {
        require(maxThreads > 0)
    }

    // state
    private var nOfThreads = 0
    private val workItemQueue = LinkedList<Runnable>()

    private val mutex = ReentrantLock()

    fun execute(workItem: Runnable) {
        mutex.withLock {
            if (nOfThreads < maxThreads) {
                Thread.ofPlatform().start {
                    workerLoop(workItem)
                }
                nOfThreads += 1
            } else {
                workItemQueue.addLast(workItem)
            }
        }
    }

    private fun getNextWorkItem(): Runnable? {
        mutex.withLock {
            val maybeFirstWorkItem = workItemQueue.pollFirst()
            if (maybeFirstWorkItem != null) {
                return maybeFirstWorkItem
            } else {
                nOfThreads -= 1
                return null
            }
        }
    }

    private fun workerLoop(initialWorkItem: Runnable) {
        var workItem: Runnable? = initialWorkItem
        while (workItem != null) {
            try {
                // Make sure the interrupt status is cleared
                // before executing the new runnable
                Thread.interrupted()
                workItem.run()
            } catch (ex: Throwable) {
                logger.warn(
                    "Uncaught exception thrown by work item: {}",
                    ex.message,
                )
                // We don't want to terminate the thread,
                // even if the Runnable ended with an exception.
            }
            workItem = getNextWorkItem()
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(VerySimpleThreadPool::class.java)
    }
}
