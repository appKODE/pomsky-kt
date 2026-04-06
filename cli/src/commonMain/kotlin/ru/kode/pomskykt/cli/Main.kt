package ru.kode.pomskykt.cli

import com.github.ajalt.clikt.core.main

fun main(args: Array<String>) {
    // If first arg is not "compile", treat as compile command
    val effectiveArgs = if (args.isNotEmpty() && args[0] != "compile" && args[0] != "--help" && args[0] != "-h" && args[0] != "--version" && args[0] != "-V") {
        arrayOf("compile") + args
    } else if (args.isEmpty()) {
        // No args — show version
        args
    } else {
        args
    }
    createCli().main(effectiveArgs)
}
