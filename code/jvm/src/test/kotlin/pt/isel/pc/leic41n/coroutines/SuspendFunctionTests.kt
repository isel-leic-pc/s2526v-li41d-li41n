package pt.isel.pc.leic41n.coroutines

import org.slf4j.LoggerFactory
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.createCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.test.Test
import kotlin.test.assertNotNull

class SuspendFunctionTests {

    companion object {
        private val logger = LoggerFactory.getLogger(SuspendFunctionTests::class.java)
    }

    var savedContinuation: Continuation<Unit>? = null

    suspend fun myFirstSuspendFunction(
        x: Int,
        y: Int,
    ): String {
        logger.info("[suspend]myFirstSuspendFunction: starting.")
        suspendCoroutine<Unit> { cont ->
            logger.info("[suspend]Block passed to suspendCoroutine is called.")
            savedContinuation = cont
        }
        logger.info("[suspend]In the middle")
        suspendCoroutine<Unit> { cont ->
            logger.info("[suspend]Second block passed to suspendCoroutine is called.")
            savedContinuation = cont
        }
        logger.info("[suspend]myFirstSuspendFunction: ending.")
        return (x + y).toString()
    }

    @Test
    fun `calling suspend functions`() {
        // myFirstSuspendFunction(2,3)
        // Compiler uses CPS when compiling a suspend function
        // CPS - Continuation Passing Style
        val cpsView = ::myFirstSuspendFunction as (Int, Int, Continuation<String>) -> Any
        val completionContinuation = object : Continuation<String> {
            override val context = EmptyCoroutineContext

            override fun resumeWith(result: Result<String>) {
                logger.info("completionContinuation: result:{}", result)
            }
        }
        val res = cpsView(2, 3, completionContinuation)
        assertNotNull(savedContinuation)
        logger.info("[test]res: {}", res)
        logger.info("[test]Before resuming saved continuation.")
        savedContinuation?.resume(Unit)
        logger.info("[test]After resuming saved continuation.")

        logger.info("[test]Before resuming saved continuation - 2.")
        savedContinuation?.resume(Unit)
        logger.info("[test]After resuming saved continuation - 2.")
    }

    val continuations = mutableListOf<Continuation<Unit>>()

    suspend fun sf2(): String {
        logger.info("sf2: starting.")
        repeat(2) {
            logger.info("sf2: starting iteration {}", it)
            suspendCoroutine { cont ->
                continuations.addLast(cont)
            }
            logger.info("sf2: resuming iteration {}", it)
        }
        logger.info("sf2: ending.")
        return "sf2"
    }

    suspend fun sf3(): String {
        logger.info("sf3: starting.")
        repeat(3) {
            logger.info("sf3: starting iteration {}", it)
            suspendCoroutine { cont ->
                continuations.addLast(cont)
            }
            logger.info("sf3: resuming iteration {}", it)
        }
        logger.info("sf3: ending.")
        return "sf3"
    }

    @Test
    fun `round-robin scheduling`() {
        val completionContinuation = object : Continuation<String> {
            override val context = EmptyCoroutineContext

            override fun resumeWith(result: Result<String>) {
                logger.info("completionContinuation: result:{}", result)
            }
        }
        val startContinuation2 = ::sf2.createCoroutine(completionContinuation)
        val startContinuation3 = ::sf3.createCoroutine(completionContinuation)
        continuations.addLast(startContinuation2)
        continuations.addLast(startContinuation3)
        logger.info("scheduler: starting.")
        while (continuations.isNotEmpty()) {
            logger.info("scheduler: calling continuation - start.")
            continuations.removeFirst().resume(Unit)
            logger.info("scheduler: calling continuation - end.")
        }
    }
}
