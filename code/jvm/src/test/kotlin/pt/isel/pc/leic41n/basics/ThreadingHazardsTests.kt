package pt.isel.pc.leic41n.basics

import java.util.LinkedList
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.test.Test
import kotlin.test.assertNotEquals

class ThreadingHazardsTests {

    @Test
    fun `missing updates`() {
        var counter = 0
        val nOfThreads = 16
        val nOfReps = 100_000
        val threads = List(nOfThreads) {
            Thread.ofPlatform().start {
                repeat(nOfReps) {
                    counter += 1
                }
            }
        }
        threads.forEach {
            it.join()
        }
        assertNotEquals(nOfThreads * nOfReps, counter)
    }

    @Test
    fun `concurrency on a list`() {
        val list = LinkedList<Int>()
        val nOfThreads = 2
        val nOfReps = 100_000
        val threads = List(nOfThreads) {
            Thread.ofPlatform().start {
                repeat(nOfReps) {
                    list.add(1)
                }
            }
        }
        threads.forEach {
            it.join()
        }
        val sum = list.reduce { acc, i -> acc + i }
        assertNotEquals(nOfThreads * nOfReps, sum)
    }

    @Test
    fun `check then act`() {
        val map = ConcurrentHashMap<Int, ConcurrentLinkedQueue<Int>>()
        val nOfThreads = 2
        val nOfReps = 100_000
        val threads = List(nOfThreads) {
            Thread.ofPlatform().start {
                repeat(nOfReps) { index ->
                    val queue = map[index]
                    if (queue == null) {
                        val queue = ConcurrentLinkedQueue<Int>()
                        queue.add(1)
                        map[index] = queue
                    } else {
                        queue.add(1)
                    }
                }
            }
        }
        threads.forEach {
            it.join()
        }
        val sum = map.entries.fold(0) { acc, entry ->
            acc + entry.value.size
        }
        assertNotEquals(nOfThreads * nOfReps, sum)
    }
}
