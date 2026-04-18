package pt.isel.pc.leic41d.coroutines

import org.slf4j.LoggerFactory
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.test.Test

class SuspendFunctionTests {

    companion object {
        private val logger = LoggerFactory.getLogger(SuspendFunctionTests::class.java)
    }

    var storedContinuation: Continuation<Unit>? = null

    suspend fun ourFirstSuspendFunction(
        x: Int,
        y: Int,
    ): String {
        logger.info("ourFirstSuspendFunction: started")
        suspendCoroutine { continuation ->
            logger.info("ourFirstSuspendFunction: inside block passed to suspendCoroutine")
            storedContinuation = continuation
        }
        logger.info("ourFirstSuspendFunction: after suspendCoroutine")
        return (x + y).toString()
    }

    @Test
    fun `calling a suspend fun`() {
        // ourFirstSuspendFunction(2, 3)
        // The Kotlin compiler uses the CPS technique when compiling suspend functions
        // CPS - Continuation Passing Style
        // A Continuation represents another complete/partial computation
        val cpsView = ::ourFirstSuspendFunction as (Int, Int, Continuation<String>) -> Any
        val completionContinuation = object : Continuation<String> {
            override fun resumeWith(result: Result<String>) {
                logger.info("resumeWith: called with {}.", result)
            }

            override val context = EmptyCoroutineContext
        }
        val res = cpsView(2, 3, completionContinuation)
        logger.info("res: {}.", res)
        storedContinuation?.resume(Unit)
        logger.info("After resuming continuation.")
    }

    val continuations = mutableListOf<Continuation<Unit>>()

    suspend fun yield() {
        suspendCoroutine { cont ->
            continuations.addLast(cont)
        }
    }

    suspend fun sf1(): String {
        logger.info("sf1: started")
        repeat(2) {
            logger.info("sf1: iteration {}, before suspend", it)
            yield()
            logger.info("sf1: iteration {}, after suspend", it)
        }
        logger.info("sf1: ending")
        return "sf1 result"
    }

    suspend fun sf2(): String {
        logger.info("sf2: started")
        repeat(3) {
            logger.info("sf2: iteration {}, before suspend", it)
            yield()
            logger.info("sf2: iteration {}, after suspend", it)
        }
        logger.info("sf2: ending")
        return "sf2 result"
    }

    @Test
    fun `calling a suspend fun - 2`() {
        val cps1 = ::sf1 as (Continuation<String>) -> Any
        val cps2 = ::sf2 as (Continuation<String>) -> Any
        val completionContinuation = object : Continuation<String> {
            override fun resumeWith(result: Result<String>) {
                logger.info("resumeWith: called with {}.", result)
            }

            override val context = EmptyCoroutineContext
        }
        cps1(completionContinuation)
        cps2(completionContinuation)
        while (continuations.isNotEmpty()) {
            logger.info("scheduler: calling continuation")
            continuations.removeFirst().resume(Unit)
            logger.info("scheduler: after calling continuation")
        }
        logger.info("scheduler: ended")
    }
}
