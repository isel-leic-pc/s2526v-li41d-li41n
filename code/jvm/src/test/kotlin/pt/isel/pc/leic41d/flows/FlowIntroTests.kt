package pt.isel.pc.leic41d.flows

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import kotlin.test.Test

class FlowIntroTests {

    companion object {
        private val logger = LoggerFactory.getLogger(FlowIntroTests::class.java)
    }

    @Test
    fun `flow intro`() {
        val f0: Flow<Int> = flow {
            logger.info("Starting first computation")
            delay(1000) // simulate a computation
            logger.info("Producing first value")
            emit(0)

            logger.info("Starting second computation")
            delay(1000) // simulate a computation
            logger.info("Producing second value")
            emit(1)
            logger.info("After last emit")
        }
        val f1: Flow<String> = f0.map {
            delay(1000)
            logger.info("Applying toString")
            it.toString()
        }
        runBlocking {
            f1.collect {
                logger.info("Receiving {}", it)
            }
        }
    }

    @Test
    fun `by default, everything runs in the same coroutine`() {
        val f1 = flow {
            repeat(5) {
                delay(100)
                logger.info("Producing {}.", it)
                emit(it)
            }
        }
        val f2 = f1.map {
            delay(200)
            logger.info("Mapping {}.", it)
            it.toString()
        }
        runBlocking {
            f2.collect {
                delay(300)
                logger.info("Collecting: {}.", it)
            }
        }
    }

    @Test
    fun `using buffer`() {
        val f1 = flow {
            repeat(5) {
                delay(100)
                logger.info("Producing {}.", it)
                emit(it)
            }
        }
        val f2 = f1.buffer()
        val f3 = f2.map {
            delay(200)
            logger.info("Mapping {}.", it)
            it.toString()
        }
        runBlocking {
            f3.collect {
                delay(300)
                logger.info("Collecting: {}.", it)
            }
        }
    }

    @Test
    fun `using buffer - 2`() {
        val f = flow {
            repeat(5) {
                delay(100)
                logger.info("Producing {}.", it)
                emit(it)
            }
        }.flowOn(Dispatchers.Default)
            .map {
                delay(200)
                logger.info("Mapping {}.", it)
                it.toString()
            }

        runBlocking {
            f.collect {
                delay(300)
                logger.info("Collecting: {}.", it)
            }
        }
    }

    @Test
    fun `using transform`() {
        val f0 = flow {
            repeat(5) {
                delay(100)
                logger.info("Producing {}.", it)
                emit(it)
            }
        }
        val f1 = f0.transform {
            emit(it)
            emit("[$it]")
        }
        runBlocking {
            f1.collect {
                delay(300)
                logger.info("Collecting: {}.", it)
            }
        }
    }
}
