package pt.isel.pc.leic41n.coroutinesx

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import pt.isel.pc.leic41n.coroutines.SuspendFunctionTests
import kotlin.test.Test
import kotlin.test.assertFails

class CoroutinesCancellationTests {

    companion object {
        private val logger = LoggerFactory.getLogger(SuspendFunctionTests::class.java)
    }

    @Test
    fun `first cancellation test`() {
        runBlocking(Dispatchers.Default) {
            logger.info("c0: starting.")
            val job1 = launch {
                logger.info("c1: starting.")
                launch {
                    logger.info("c2: starting.")
                    try {
                        delay(1000)
                    } catch (e: Exception) {
                        logger.info("c2: caught: {}-{}", e.javaClass.simpleName, e.message)
                    }
                }
                try {
                    Thread.sleep(1000)
                } catch (e: Exception) {
                    logger.info("c1: caught: {}-{}", e.javaClass.simpleName, e.message)
                }
            }
            delay(500)
            logger.info("c0: cancelling c1.")
            job1.cancel()
            logger.info("c0: is c1 cancelled? {} is c1 completed? {}", job1.isCancelled, job1.isCompleted)
        }
        logger.info("After runBlocking")
    }

    @Test
    fun `cancellation and async-await`() {
        runBlocking(Dispatchers.Default) {
            logger.info("c0: starting")
            val deferred: Deferred<Int> = async {
                logger.info("c1: starting.")
                try {
                    Thread.sleep(1000)
                } catch (e: Exception) {
                    logger.info("c1: caught: {}-{}", e.javaClass.simpleName, e.message)
                }
                logger.info("Returning 42")
                42
            }
            launch {
                logger.info("c2: starting.")
                try {
                    val res = deferred.await()
                    logger.info("c2: res = {}", res)
                } catch (e: Exception) {
                    logger.info("c2: caught: {}-{}", e.javaClass.simpleName, e.message)
                }
                logger.info("c2: ending.")
            }

            delay(500)
            logger.info("c0: Cancelling c1")
            deferred.cancel()
        }
    }

    @Test
    fun `error propagation`() {
        assertFails {
            runBlocking(Dispatchers.Default) {
                logger.info("c0: starting")
                launch {
                    logger.info("c1: starting")
                    launch {
                        logger.info("c2: starting")
                        delay(500)
                        logger.info("c2: ending with exception")
                        throw Exception("c2 error!!!")
                    }
                    try {
                        delay(1000)
                    } catch (e: Exception) {
                        logger.info("c1: caught: {}-{}", e.javaClass.simpleName, e.message)
                    }
                    logger.info("c1: ending normally")
                }
            }
        }
    }
}
