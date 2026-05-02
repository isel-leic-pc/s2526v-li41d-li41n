package pt.isel.pc.leic41d.coroutinesx

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import pt.isel.pc.leic41d.coroutines.CoroutinesIntroTests
import kotlin.test.Test
import kotlin.test.assertFails

class CancellationTests {

    companion object {
        private val logger = LoggerFactory.getLogger(CoroutinesIntroTests::class.java)

        private fun log(
            template: String,
            vararg args: Any,
        ) = logger.info(template, *args)
    }

    @Test
    fun `what are the consequences of cancellation`() {
        assertFails {
            runBlocking {
                val c1 = launch {
                    try {
                        delay(1000)
                    } catch (e: Exception) {
                        logger.info("c1: delay thrown exception: {}", e.message)
                        throw Exception("Error!")
                    }
                }

                launch {
                    try {
                        delay(1000)
                    } catch (e: Exception) {
                        logger.info("c2: delay thrown exception: {}", e.message)
                    }
                }

                delay(500)
                c1.cancel()
            }
        }
    }
}
