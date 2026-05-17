package pt.isel.pc.leic41d.flows

import org.slf4j.LoggerFactory
import kotlin.test.Test

class SequenceTests {

    companion object {
        private val logger = LoggerFactory.getLogger(SequenceTests::class.java)
    }

    @Test
    fun `sequence intro`() {
        val s: Sequence<Int> = sequence {
            logger.info("Starting first computation")
            Thread.sleep(1000) // simulate a computation
            logger.info("Producing first value")
            yield(0)

            logger.info("Starting second computation")
            Thread.sleep(1000) // simulate a computation
            logger.info("Producing second value")
            yield(1)
            logger.info("After last yield")
        }
        val s2: Sequence<String> = s.map {
            // delay(1000)
            logger.info("Applying toString()")
            it.toString()
        }
        val i2: Iterator<String> = s2.iterator()

        i2.hasNext()
        logger.info("First item: {}", i2.next())
        i2.hasNext()
        logger.info("Second item: {}", i2.next())
        logger.info("Before last hasNext")
        logger.info("hasNext(): {}", i2.hasNext())
        logger.info("hasNext(): {}", i2.hasNext())

        val i3: Iterator<String> = s2.iterator()
        i3.hasNext()
    }
}
