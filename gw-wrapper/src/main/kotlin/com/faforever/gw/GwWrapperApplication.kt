package com.faforever.gw

import io.github.oshai.kotlinlogging.KotlinLogging
import picocli.CommandLine
import java.util.concurrent.Callable
import kotlin.system.exitProcess

private val logger = KotlinLogging.logger {}

@CommandLine.Command(
    name = "gw-wrapper",
    mixinStandardHelpOptions = true,
    usageHelpAutoWidth = true,
    description = ["The galactic war cli wrapper"],
)
class GwWrapperApplication : Callable<Int> {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val exitCode = CommandLine(GwWrapperApplication()).execute(*args)
            exitProcess(exitCode)
        }
    }

    override fun call(): Int {
        return 0
    }
}
