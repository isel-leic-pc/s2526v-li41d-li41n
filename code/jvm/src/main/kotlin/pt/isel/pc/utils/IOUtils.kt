package pt.isel.pc.utils

import java.io.BufferedWriter

fun BufferedWriter.writeLine(line: String) {
    write(line)
    newLine()
    flush()
}
