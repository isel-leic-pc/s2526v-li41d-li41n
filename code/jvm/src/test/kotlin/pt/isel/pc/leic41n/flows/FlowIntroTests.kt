package pt.isel.pc.leic41n.flows

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import java.time.Instant
import kotlin.test.Test

class FlowIntroTests {

    companion object {
        private val logger = LoggerFactory.getLogger(FlowIntroTests::class.java)
    }

    @Test
    fun `creating and consuming a flow`() {
        val f: Flow<Int> = flow {
            logger.info("started")
            delay(1000)
            logger.info("producing first element")
            emit(0)
            logger.info("after first emit")
            delay(1000)
            logger.info("producing second element")
            emit(1)
            logger.info("after second emit")
        }
        val f2: Flow<String> = f.map {
            logger.info("mapping")
            delay(500)
            it.toString()
        }

        val f3: Flow<String> = f2.take(1)

        runBlocking {
            f3.collect {
                delay(100)
                logger.info("Collecting {}", it)
            }
            logger.info("Second collect")
            f3.collect {
                delay(100)
                logger.info("Collecting {}", it)
            }
        }
    }

    @Test
    fun `creating, transforming, and collection a flow`() {
        // Flow building/creation
        val f0 = flow {
            repeat(5) {
                delay(100)
                logger.info("Producing {}.", it)
                emit(it)
            }
        }
        // Flow transformation
        val f1 = f0.map {
            delay(200)
            logger.info("Mapping {}.", it)
            it.toString()
        }
        // Flow consumption/collecting
        runBlocking {
            f1.collect {
                delay(300)
                logger.info("Collecting {}.", it)
            }
        }
    }

    @Test
    fun `creating, transforming, and collection a flow - using transform`() {
        // Flow building/creation
        val f0: Flow<Int> = flow {
            repeat(5) {
                delay(100)
                logger.info("Producing {}.", it)
                emit(it)
            }
        }
        // Flow transformation
        val f1: Flow<String> = f0.map {
            delay(200)
            logger.info("Mapping {}.", it)
            it.toString()
        }
        val f2 = f1.transform {
            emit("($it)")
            delay(200)
            emit("[$it]")
        }
        // Flow consumption/collecting
        runBlocking {
            f2.collect {
                delay(300)
                logger.info("Collecting {}.", it)
            }
        }
    }

    @Test
    fun `creating, transforming, and collection a flow - with buffer`() {
        // Flow building/creation
        val f = flow {
            repeat(5) {
                delay(100)
                logger.info("Producing {}.", it)
                emit(it)
            }
        }.buffer()
            .map {
                delay(200)
                logger.info("Mapping {}.", it)
                it.toString()
            }.buffer()

        // Flow consumption/collecting
        runBlocking {
            f.collect {
                delay(300)
                logger.info("Collecting {}.", it)
            }
        }
    }

    @Test
    fun `StateFlow example`() {
        val mutableState: MutableStateFlow<Long> = MutableStateFlow(0L)
        val f0: Flow<Long> = mutableState.asStateFlow()
        val updatePeriodInMs = 100
        Thread.ofPlatform().start {
            repeat(10_000 / updatePeriodInMs) {
                Thread.sleep(updatePeriodInMs.toLong())
                mutableState.value = Instant.now().epochSecond
            }
            logger.info("Mutating thread ending.")
        }
        val f1: Flow<Instant> = f0
            .map {
                logger.info("Mapping {}.", it)
                Instant.ofEpochSecond(it)
            }.map {
                logger.info("Second mapping {}.", it)
                it.plusSeconds(3600)
            }
        runBlocking {
            delay(3000)
            val c2 = launch {
                f1.collect {
                    logger.info("C2: collecting {}.", it)
                }
            }
            val c3 = launch {
                f1.collect {
                    logger.info("C3: collecting {}.", it)
                }
            }
            delay(4000)
            logger.info("Cancelling consuming coroutines.")
            c2.cancel()
            c3.cancel()
        }
        logger.info("Test ending.")
    }
}
