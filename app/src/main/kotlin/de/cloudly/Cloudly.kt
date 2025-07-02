package de.cloudly

import com.google.inject.Inject
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.plugin.Plugin
import org.slf4j.Logger

@Plugin(
    id = "cloudly",
    name = "Cloudly",
    version = BuildConstants.VERSION,
    authors = ["Cloudly"],
    description = "A plugin for Velocity and Paper servers"
)
class Cloudly @Inject constructor(
    private val logger: Logger
) {

    @Subscribe
    fun onProxyInitialization(event: ProxyInitializeEvent) {
        logger.info("Cloudly Plugin enabled on Velocity!")
    }
}
