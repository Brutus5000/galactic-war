package com.faforever.fa

import io.github.oshai.kotlinlogging.KotlinLogging
import picocli.CommandLine
import picocli.CommandLine.ArgGroup
import picocli.CommandLine.Option
import java.lang.IllegalArgumentException
import java.util.concurrent.Callable
import kotlin.system.exitProcess

private val log = KotlinLogging.logger {}

@CommandLine.Command(
    name = "fa-game-mock",
    mixinStandardHelpOptions = true,
    usageHelpAutoWidth = true,
    description = ["A developer tool that emulates ForgedAlliance.exe behavior"],
)
class FaGameMockApplication : Callable<Int> {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val exitCode = CommandLine(FaGameMockApplication()).execute(*args)
            exitProcess(exitCode)
        }
    }

    @Option(names = ["/init"], required = true, description = ["Set the path to the init.lua"])
    private var initLuaPath: String = ""

    @Option(names = ["/nobugreport"], required = false, description = ["Disable (broken) GPGnet bug reporting"])
    private var noBugReport: Boolean = false

    class FactionOption {
        @Option(names = ["/aeon"])
        var aeon: Boolean = false

        @Option(names = ["/cybran"])
        var cybran: Boolean = false

        @Option(names = ["/seraphim"])
        var seraphim: Boolean = false

        @Option(names = ["/uef"])
        var uef: Boolean = false
    }

    @ArgGroup(exclusive = true, multiplicity = "0..1")
    private var factionOption: FactionOption? = null

    @Option(names = ["/log"], required = false, description = ["Set the path to the log file"])
    private var logPath: String? = null

    @Option(names = ["/gpgnet"], required = false, description = ["Set ip:port for the gpgnet server"])
    private var gpgnetHostPort: String? = null

    @Option(names = ["/mean"], required = false, description = ["Truskill mean of the current player"])
    private var mean: Double? = null

    @Option(names = ["/deviation"], required = false, description = ["Truskill deviation of the current player"])
    private var deviation: Double? = null

    @Option(names = ["/division"], required = false, description = ["FAF division of the current player"])
    private var division: String? = null

    @Option(names = ["/subdivision"], required = false, description = ["FAF subdivision of the current player"])
    private var subdivision: String? = null

    @Option(names = ["/replay"], required = false, description = ["Path of a replay file that should be run"])
    private var replayPath: String? = null

    @Option(
        names = ["/savereplay"],
        required = false,
        description = ["URI of gpgnet:// protocol pointing to the replay server"],
    )
    private var replayServerUri: String? = null

    @Option(
        names = ["/country"],
        required = false,
        description = ["Country of the current player (for showing a flag)"],
    )
    private var country: String? = null

    @Option(names = ["/clan"], required = false, description = ["Clan tag of the current player"])
    private var clanTag: String? = null

    @Option(names = ["/replayid"], required = false, description = ["ID of the replay that is being played"])
    private var replayId: Long? = null

    @Option(names = ["/numgames"], required = false, description = ["Number of rated games current player has played"])
    private var numberOfRatedGames: Int? = null

    @Option(names = ["/team"], required = false, description = ["FA team id the current player will play for"])
    private var teamId: Int? = null

    @Option(
        names = ["/player"],
        required = false,
        description = ["Number of expected players to join the game before launch (for autolobby)"],
    )
    private var expectedPlayers: Int? = null

    @Option(names = ["/startspot"], required = false, description = ["Map position the current player will start on"])
    private var mapPosition: Int? = null

    @Option(names = ["/map"], required = false, description = ["Map the game will start on"])
    private var mapPath: String? = null

    @Option(names = ["/gameoptions"], required = false, description = ["List of game options list(key:value)"])
    private var gameoptions: List<String> = emptyList()

    private fun parseGpgnetEndpoint(gpgnetHostPort: String): Endpoint {
        val components = gpgnetHostPort.split(":", limit = 2)

        return try {
            Endpoint(
                host = components[0],
                port = components[1].toInt(),
            )
        } catch (e: Exception) {
            throw IllegalArgumentException("Could not parse gpgnet endpoint")
        }
    }

    override fun call(): Int {
        // Shadow variables as immutable
        val replayPath = replayPath

        val gpgnetEndpoint = gpgnetHostPort?.let { parseGpgnetEndpoint(it) }

        val launchOptions: LaunchOptions =
            when {
                gpgnetEndpoint != null ->
                    LaunchOptions.Gpgnet(
                        lobbyServer = gpgnetEndpoint,
                        replayServer = null,
                        expectedPlayers = expectedPlayers,
                    )
                replayPath != null ->
                    LaunchOptions.Replay(
                        replayId = replayId ?: 0L,
                        replayPath = replayPath,
                    )
                else -> LaunchOptions.Offline
            }

        val gameProcessOptions =
            GameProcessOptions(
                initLuaPath = initLuaPath,
                noBugReport = noBugReport,
                logPath = logPath,
            )

        val userOptions =
            UserOptions(
                mean = mean,
                deviation = deviation,
                division = division,
                subdivision = subdivision,
                country = country,
                clanTag = clanTag,
                numberOfRatedGames = numberOfRatedGames,
            )

        val faction = factionOption
        val commanderOptions =
            CommanderOptions(
                faction =
                    when {
                        faction == null -> null
                        faction.aeon -> Faction.AEON
                        faction.cybran -> Faction.CYBRAN
                        faction.seraphim -> Faction.SERAPHIM
                        faction.uef -> Faction.UEF
                        else -> throw UnsupportedOperationException("Unreachable code")
                    },
                teamId = teamId,
                mapPosition = mapPosition,
            )

        log.info { launchOptions }
        log.info { gameProcessOptions }
        log.info { userOptions }
        log.info { commanderOptions }

        when (launchOptions) {
            is LaunchOptions.Gpgnet ->
                GpgnetProcess(
                    launchOptions,
                    userOptions,
                    commanderOptions,
                    gameProcessOptions,
                ).runLoop()
            is LaunchOptions.Offline -> log.error { "Offline launching is not supported." }
            is LaunchOptions.Replay -> log.error { "Replay launching is not supported." }
        }

        return 0
    }
}
