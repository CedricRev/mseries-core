# mseries-core

Shared library for MSeries plugins. Every MSeries plugin shades it in; it is never installed on a server by itself.

Version 1.0.0 covers:

- **Platform detection:** `Platform.current().core()` returns `SPIGOT`, `PAPER`, `PURPUR` or `FOLIA`, detected once at startup.
- **Commands:** `CommandService.create(this)` sets up Incendo Cloud v2 for the server. Paper, Purpur and Folia get native Brigadier. Spigot gets standard Bukkit registration.

## How to use it

**1. Add it** in `build.gradle.kts`:

```kotlin
repositories {
    maven("https://jitpack.io")
    maven("https://libraries.minecraft.net/")
}

dependencies {
    compileOnly("org.spigotmc:spigot-api:26.3-R0.1-SNAPSHOT")
    compileOnly("com.mojang:brigadier:1.3.10")
    implementation("com.github.CedricRev:mseries-core:v1.0.0")
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(21)
    options.compilerArgs.add("-parameters")
}
```

**2. Shade and relocate** it with Shadow, replacing `<plugin>` with the plugin's package name:

```kotlin
tasks.shadowJar {
    val lib = "me.cedricrev.mseries.<plugin>.lib"
    relocate("me.cedricrev.mseries.core", "$lib.core")
    relocate("org.incendo.cloud", "$lib.cloud")
    relocate("io.leangen.geantyref", "$lib.geantyref")
    exclude("META-INF/services/javax.annotation.processing.Processor")
}
```

**3. Register commands** in `onEnable`:

```java
public final class MExamplePlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        CommandService commands = CommandService.create(this);
        commands.register(new ExampleCommands());
    }
}

public final class ExampleCommands {

    @Command("mexample|mex ping")
    @Permission("mseries.mexample.ping")
    public void ping(CommandActor actor) {
        actor.reply("pong");
    }
}
```

Ship both `plugin.yml` and `paper-plugin.yml` with `folia-supported: true`, and don't list commands in either file.

## License

[MIT](LICENSE)
