package ru.kode.pomskykt.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import ru.kode.pomskykt.Expr

/**
 * Root CLI command for the Pomsky compiler.
 */
class PomskyCliApp : CliktCommand(name = "pomsky") {
    override fun run() {
        // Only print version when no subcommand is invoked
    }
}

fun createCli(): PomskyCliApp {
    return PomskyCliApp().subcommands(
        CompileCommand(),
        MigrateCommand(),
        ExplainCommand(),
        ComplexityCommand(),
        TestCommand(),
    )
}
