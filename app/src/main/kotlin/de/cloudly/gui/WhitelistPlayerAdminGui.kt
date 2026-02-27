package de.cloudly.gui

import de.cloudly.CloudlyPaper
import org.bukkit.entity.Player
import java.util.UUID

/**
 * Detail view for a single whitelist entry, providing moderation tools.
 */
class WhitelistPlayerAdminGui(
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
        WhitelistPlayerAdminGui(plugin, viewer, targetUuid, updatedName, page).open()
    },
    reopenParent = { page ->
        plugin.getWhitelistGuiManager().openWhitelistGui(viewer, page)
    }
)
