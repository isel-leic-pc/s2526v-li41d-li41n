package pt.isel.pc.leic41n.flows

import org.slf4j.LoggerFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SequenceTests {

    companion object {
        private val logger = LoggerFactory.getLogger(SequenceTests::class.java)
    }

    @Test
    fun `sequence intro`() {
        val s: Sequence<Int> = sequence {
            logger.info("started")
            Thread.sleep(1000)
            logger.info("producing first element")
            yield(0)
            logger.info("after first yield")
            Thread.sleep(1000)
            logger.info("producing second element")
            yield(1)
            logger.info("after second yield")
        }
        val s2: Sequence<String> = s.map {
            logger.info("mapping")
            it.toString()
        }
        val iter2 = s2.iterator()
        assertTrue(iter2.hasNext())
        assertEquals("0", iter2.next())
        logger.info("Asking if second element exists")
        assertTrue(iter2.hasNext())
        assertEquals("1", iter2.next())
        logger.info("Asking if third element exists")
        assertFalse(iter2.hasNext())
    }
}
