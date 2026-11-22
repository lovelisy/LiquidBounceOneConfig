<div align="center">
<p>
    <img width="200" src="https://raw.githubusercontent.com/CCBlueX/LiquidCloud/master/LiquidBounce/liquidbounceLogo.svg">
</p>
</div>

LiquidBounce is a free and open-source mixin-based injection hacked client using the Fabric API for Minecraft.

This build is a LiquidBounce fork that replaces the browser-based interface with
[OneConfig](https://github.com/Polyfrost/OneConfig). The module list and the in-game HUD are rendered by OneConfig, so
there is no embedded browser, no web server and no bundled front end.

## What is different

- **OneConfig instead of the WebUI.** The ClickGUI key opens the OneConfig screen, where a `LiquidBounce` card lists
  every module grouped by its category. Toggling a module there is the same as toggling it in game.
- **HUD through OneConfig.** The native HUD elements (target HUD, array list and notifications) are registered with
  OneConfig's HUD system, so they can be moved, scaled and restyled in OneConfig's HUD editor.
- **Removed:** the CEF browser backend, the Ktor interop server, the Svelte theme, the task/loading screens, the deep
  learning engine and the AI angle smooth rotation mode, the browser based Microsoft login and the local OAuth callback
  server.
- **No Node.js** is required to build, the theme is no longer compiled during `build`.

## Requirements

- Minecraft 26.3 with Fabric Loader and Fabric Language Kotlin.
- [OneConfig](https://github.com/Polyfrost/OneConfig) 1.9.1 or newer must be installed, it provides the settings screen
  and the HUD renderer.
- [PolyCompose](https://github.com/Polyfrost/PolyCompose) 3.0.0 or newer must be installed. OneConfig ships it as a
  dependency of its own, so it is normally already present; LiquidBounce uses its immediate mode renderer to draw the
  HUD with OneConfig's font engine.
