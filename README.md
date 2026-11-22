<div align="center">
<p>
    <img width="200" src="https://raw.githubusercontent.com/CCBlueX/LiquidCloud/master/LiquidBounce/liquidbounceLogo.svg">
</p>

[Website](https://liquidbounce.net) |
[Forum](https://forums.ccbluex.net) |
[Discord](https://liquidbounce.net/discord) |
[YouTube](https://youtube.com/CCBlueX) |
[X](https://x.com/CCBlueX)
</div>

LiquidBounce is a free and open-source mixin-based injection hacked client using the Fabric API for Minecraft.

This build is a LiquidBounce fork that replaces the browser-based interface with
[OneConfig](https://github.com/Polyfrost/OneConfig). The module list and the in-game HUD are rendered by OneConfig, so
there is no embedded browser, no web server and no bundled front end.

## What is different

- **OneConfig instead of the WebUI.** The ClickGUI key opens the OneConfig screen, where a `LiquidBounce` card lists
  every module grouped by its category. Toggling a module there is the same as toggling it in game.
- **HUD through OneConfig.** `Player Info` is registered with OneConfig's HUD system, so it can be moved, scaled and
  restyled in OneConfig's HUD editor. The `HUD` module stays as the master switch.
- **Removed:** the CEF browser backend, the Ktor interop server, the Svelte theme, the task/loading screens, the deep
  learning engine and the AI angle smooth rotation mode, the browser based Microsoft login and the local OAuth callback
  server.
- **No Node.js** is required to build, the theme is no longer compiled during `build`.

## Requirements

- Minecraft 26.3 with Fabric Loader and Fabric Language Kotlin.
- [OneConfig](https://github.com/Polyfrost/OneConfig) 1.9.1 or newer must be installed, it provides the settings screen
  and the HUD renderer.

## Setting up a Workspace

LiquidBounce uses Gradle; to make sure that it is installed properly, you can
check [Gradle's website](https://gradle.org/install/).

1. Clone the repository (`git clone https://github.com/lovelisy/LiquidBounceOneConfig`).
2. CD into the local repository (`cd LiquidBounceOneConfig`).
3. Run `./gradlew genSources` for better development experience (Optional).
4. Open the folder as a Gradle project in your preferred IDE.
5. Run the client (`./gradlew runClient`), with OneConfig in the `mods` folder.

## Additional libraries

### Mixins

Mixins can be used to modify classes at runtime before they are loaded. LiquidBounce uses it to inject its code into the
Minecraft client. This way, none of Mojang's copyrighted code is shipped. If you want to learn more about it, check out
its [Documentation](https://docs.spongepowered.org/5.1.0/en/plugin/internals/mixins.html).

### OneConfig

OneConfig is licensed under the GNU Lesser General Public License v3 together with the Polyfrost additional terms. It is
a required mod at runtime and is not bundled into this jar.

## License

This project is subject to the [GNU General Public License v3.0](https://www.gnu.org/licenses/gpl-3.0.en.html). This
does only apply for source code located directly in this clean repository. During the development and compilation
process, additional source code may be used to which we have obtained no rights. Such code is not covered by the GPL
license.

For those who are unfamiliar with the license, here is a summary of its main points. This is by no means legal advice
nor legally binding.

*Actions that you are allowed to do:*

- Use
- Share
- Modify

*If you do decide to use ANY code from the source:*

- **You must disclose the source code of your modified work and the source code you took from this project. This means
  you are not allowed to use code from this project (even partially) in a closed-source (or even obfuscated)
  application.**
- **Your modified application must also be licensed under the GPL**

## Contributing

We appreciate contributions. So if you want to support us, feel free to make changes to LiquidBounce's source code and
submit a pull request.

## Imprint

**CCBlueX**  
Vahrenwalder Str. 269A  
30179 Hanover  
Germany

**Owner and responsible for the content:** Marco Beyer
