package hu.bendi.lobby

import net.kyori.adventure.key.Key
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.sound.Sound.Source
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.title.Title
import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.GameMode
import net.minestom.server.event.player.PlayerDisconnectEvent
import net.minestom.server.event.player.PlayerLoginEvent
import net.minestom.server.event.player.PlayerMoveEvent
import net.minestom.server.extensions.Extension
import net.minestom.server.instance.block.Block
import world.cepi.kstom.Manager
import world.cepi.kstom.command.kommand.Kommand
import world.cepi.kstom.event.listen
import world.cepi.kstom.event.listenOnly
import java.util.*
import kotlin.math.max

class ParkourLeaveCommand: Kommand({
    onlyPlayers()

    syntax {
        if (parkour.containsKey(player.uuid)) {
            parkour.remove(player.uuid)
            player.sendMessage("Kiléptél a parkourból!")
            player.teleport(lobbyPos)
            player.playSound(tpSound)
        } else {
            player.sendMessage("Nem vagy a parkourban!")
        }
    }
}, "leave")

val parkour = HashMap<UUID, Int>()
val lobbyPos = Pos(80.5, 123.0, 75.5, -90.0f, 0.0f)
val parkourStartPos = Pos(66.5, 122.0, 45.5, -145.0f, 0.0f)
val tpSound = Sound.sound(Key.key("minecraft:entity.enderman.teleport"), Source.PLAYER, 1.0f, 2.0f)
val winSound = Sound.sound(Key.key("minecraft:ui.toast.challenge_complete"), Source.PLAYER, 1.0f, 1.0f)
val clickSound = Sound.sound(Key.key("minecraft:block.dispenser.dispense"), Source.PLAYER, 1.0f, 1.0f)

class LobbyExtension : Extension() {

    private val checkpoints = arrayOf(
        122,
        126,
        130,
        134,
        140,
        144,
    )

    private val checkpointPos = arrayOf(
        Pos(66.5, 122.0, 45.5, -150.0f, 0.0f),
        Pos(81.5, 126.0, 34.5, -90.0f, 0.0f),
        Pos(102.5, 130.0, 40.5, -15.0f, 0.0f),
        Pos(105.5, 134.0, 54.5, -30.0f, 0.0f),
        Pos(110.5, 140.0, 73.5, 20.0f, 0.0f),
        Pos(103.5, 144.0, 81.5, 90.0f, 0.0f)
    )

    override fun initialize() {
        Manager.command.register(ParkourLeaveCommand().command)
        Manager.globalEvent.listen<PlayerMoveEvent> {
            filters += {
                player.position.y < 119 && !parkour.containsKey(player.uuid)
            }
            handler {
                player.teleport(lobbyPos)
                player.playSound(tpSound)
            }
        }
        Manager.globalEvent.listen<PlayerMoveEvent> {
            filters += {
                parkour.containsKey(player.uuid) && player.position.y < max(checkpoints[max(parkour[player.uuid]!!-1, 0)]-2, 120)
            }
            handler {
                player.teleport(checkpointPos[max(0, parkour[player.uuid]!!-1)])
                player.playSound(tpSound)
            }
        }
        Manager.globalEvent.listen<PlayerLoginEvent> {
            handler {
                player.respawnPoint = lobbyPos
                player.gameMode = GameMode.ADVENTURE
            }
        }
        Manager.globalEvent.listenOnly<PlayerDisconnectEvent> {
            parkour.remove(player.uuid)
        }
        Manager.globalEvent.listen<PlayerMoveEvent> {
            filters += {
                player.instance?.getBlock(
                    player.position,
                    Block.Getter.Condition.TYPE
                ) == Block.HEAVY_WEIGHTED_PRESSURE_PLATE
            }

            handler {
                if (parkour.containsKey(player.uuid)) {
                    if (player.position.blockY() == checkpoints[parkour[player.uuid]!!]) {
                        parkour.replace(player.uuid, parkour[player.uuid]!!+1)
                        player.showTitle(Title.title(
                            Component.text("Checkpoint #${parkour[player.uuid]!!}")
                                .color(TextColor.color(0xe27e20))
                                .decorate(TextDecoration.BOLD),
                            Component.empty()
                        ))
                        if (parkour[player.uuid]!! == checkpoints.size) {
                            parkour.remove(player.uuid)
                            player.showTitle(Title.title(
                                Component.text("GG!")
                                    .color(TextColor.color(0xe27e20))
                                    .decorate(TextDecoration.BOLD),
                                Component.text("Parkour teljesítve!")
                                    .color(TextColor.color(0x337bac))
                            ))
                            player.playSound(winSound)
                        } else player.playSound(clickSound)
                    }
                } else if (player.position.blockY() == checkpoints[0]) {
                    parkour[player.uuid] = 1

                    player.showTitle(
                        Title.title(
                        Component.text("Parkour")
                            .color(TextColor.color(0xe27e20))
                            .decorate(TextDecoration.BOLD),
                        Component.text("Érj el a célig!")
                            .color(TextColor.color(0x337bac))
                    ))
                    player.sendMessage(Component.text("A kilépéshez használd a /leave parancsot!").color(TextColor.color(0xFF3128)))
                    player.playSound(clickSound)
                }
            }
        }
        Manager.instance.instances.forEach {
            it.time = 18000
            it.timeRate = 0
        }
    }

    override fun terminate() {

    }
}