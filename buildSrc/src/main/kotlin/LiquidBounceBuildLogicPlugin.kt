package net.ccbluex

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Marker plugin so buildSrc produces a valid plugin descriptor while only exposing the script helper functions.
 */
class LiquidBounceBuildLogicPlugin : Plugin<Project> {

    override fun apply(target: Project) = Unit

}
