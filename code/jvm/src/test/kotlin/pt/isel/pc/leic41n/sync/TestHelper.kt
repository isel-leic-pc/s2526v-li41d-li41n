package pt.isel.pc.leic41n.sync

import java.util.concurrent.ConcurrentLinkedQueue

class TestHelper(
    private val threadBuilder: Thread.Builder,
) {
    private val capturedExceptions = ConcurrentLinkedQueue<Throwable>()
    private val threads = ConcurrentLinkedQueue<Thread>()

    fun runTest(block: (TestHelper) -> Unit) {
        try {
            block(this)
        } finally {
            threads.forEach { it.join() }
            if (capturedExceptions.isNotEmpty()) {
                val firstException = capturedExceptions.peek()
                throw firstException
            }
        }
    }

    fun startThread(runnable: Runnable): Thread {
        val thread = threadBuilder.start {
            try {
                runnable.run()
            } catch (ex: Throwable) {
                capturedExceptions.add(ex)
            }
        }
        threads.add(thread)
        return thread
    }
}
