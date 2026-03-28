package pt.isel.pc.leic41d.sync

import java.util.concurrent.ConcurrentLinkedQueue

class TestHelper(
    val threadBuilder: Thread.Builder,
) {
    private val capturedExceptions = ConcurrentLinkedQueue<Throwable>()
    private val threads = ConcurrentLinkedQueue<Thread>()

    fun newThread(runnable: Runnable): Thread {
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

    fun waitForEnd() {
        threads.forEach { it.join() }
        if (capturedExceptions.isNotEmpty()) {
            val first = capturedExceptions.peek()
            throw first
        }
    }

    fun runTest(block: (TestHelper) -> Unit) {
        try {
            block(this)
        } finally {
            waitForEnd()
        }
    }
}
