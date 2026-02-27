package de.cloudly.gui

import de.cloudly.CloudlyPaper
import org.bukkit.entity.Player
import java.util.UUID

/**
 * Detail view for a tracked player with moderation shortcuts.
 */
class PlayerAdminGui(
    plugin: CloudlyPaper,
    viewer: Player,
    targetUuid: UUID,
    targetName: String,
    parentPage: Int
) : PlayerToolGuiBase(
    plugin = plugin,
    viewer = viewer,
    targetUuid = targetUuid,
    initialTargetName = targetName,
    parentPage = parentPage,
    reopenSelf = { updatedName, page ->
        PlayerAdminGui(plugin, viewer, targetUuid, updatedName, page).open()
    },
    reopenParent = { page ->
        plugin.getAdminGuiManager().openAdminGui(viewer, page)
    }
)
