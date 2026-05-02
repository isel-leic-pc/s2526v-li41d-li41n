package pt.isel.pc.leic41d.coroutines

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import pt.isel.pc.utils.JobState
import pt.isel.pc.utils.getState
import kotlin.concurrent.thread
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.test.Test

/**
 * Introductory learning tests to Kotlin Coroutines,
 * as provided by the `kotlinx.coroutines` library.
 */
class CoroutinesIntroTests {

    companion object {
        private val logger = LoggerFactory.getLogger(CoroutinesIntroTests::class.java)

        private fun log(
            template: String,
            vararg args: Any,
        ) = logger.info(template, *args)
    }

    @Test
    fun `intro test`() {
        log("test starting.")
        // runBlocking:
        // - scope builder
        runBlocking {
            // this: CoroutineScope ->
            val scope: CoroutineScope = this
            log("block starting.")
            // launch:
            // - coroutine builder
            // - receives a suspend fun that defines the coroutine body
            scope.launch {
                log("c2: starting.")
                // delay(1000)
                Thread.sleep(1000)
                log("c2: ending.")
            }
            scope.launch {
                log("c3: starting.")
                // delay(500)
                Thread.sleep(500)
                log("c3: ending.")
            }
            log("block ending.")
        }
        log("test ending.")
    }

    @Test
    fun `unconfined dispatcher`() {
        runBlocking(Dispatchers.Default) {
            launch {
                log("c1: starting")
                mydelay(1000)
                log("c2: ending")
            }
        }
    }

    @Test
    fun `different contexts`() {
        runBlocking {
            log("c1: starting.")
            launch(Dispatchers.IO) {
                log("c2: starting.")
                delay(1000)
                log("c2: ending.")
            }
            launch(Dispatchers.Default) {
                log("c3: starting.")
                delay(500)
                log("c3: ending.")
            }
            log("c1: ending.")
        }
    }

    @Test
    fun `changing context`() {
        runBlocking {
            launch {
                log("c2: starting.")
                withContext(Dispatchers.Default) {
                    log("c2: inside withContext.")
                    delay(1000)
                    log("c2: after delay.")
                }
                log("c2: ending.")
            }
        }
    }

    suspend fun mydelay(ms: Long) {
        log("Inside mydelay")
        suspendCoroutine { cont ->
            log("Inside block provided to suspendCoroutine")
            thread {
                Thread.sleep(ms)
                log("resuming coroutine")
                cont.resume(Unit)
            }
        }
        log("after suspendCoroutine")
    }

    @Test
    fun `nested coroutine scopes`() {
        runBlocking {
            this.launch {
                log("c2: starting.")
                createNestedCoroutines()
                log("c2: ending.")
            }
        }
    }

    suspend fun createNestedCoroutines() {
        log("Before coroutineScope.")
        coroutineScope {
            // this: CoroutineScope ->
            launch {
                log("c3: starting.")
                delay(1000)
                log("c3: ending.")
            }
        }
        log("After coroutineScope.")
    }

    @Test
    fun `intro to jobs`() {
        runBlocking {
            val job = launch {
                log("c2: starting.")
                coroutineScope {
                    launch {
                        log("c3: starting.")
                        delay(2000)
                        log("c3: ending")
                    }
                    delay(1000)
                }
                log("c2: ending.")
            }
            while (true) {
                val state = job.getState()
                log("job state: {}", state)
                if (state == JobState.COMPLETED) {
                    break
                }
                delay(100)
            }
        }
    }
}
