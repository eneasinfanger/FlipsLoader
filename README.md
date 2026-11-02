
# FlipsLoader

CDI-like features for minecraft paper-based plugin development.

## Features:

* Dependency Injection (fields & constructor, but also programmatic)
* [CommandAPI](https://github.com/CommandAPI/CommandAPI)-Integration (automatic initialization & easy command registration, also supports automatic registration of CommandAPI-annotated commands)
* Automatic services with startup and shutdown hooks
* Automatic registration of bukkit event listeners

## How to use:

### Dependency

FlipsLoader is available through [JitPack](https://jitpack.io/#eneasinfanger/FlipsLoader). Add the repository and the dependency to your `pom.xml`:

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>com.github.eneasinfanger</groupId>
        <artifactId>FlipsLoader</artifactId>
        <version>1.0.1</version>
    </dependency>
</dependencies>
```

Gradle:

```kotlin
repositories { maven("https://jitpack.io") }
dependencies { implementation("com.github.eneasinfanger:FlipsLoader:1.0.1") }
```

Replace `1.0.1` with the [latest release tag](https://github.com/eneasinfanger/FlipsLoader/releases).
The library is meant to be shaded into your plugin.

To build it yourself instead: clone the repository and run `mvn clean install`.

### Init class scanning / registration

Call `ch.eneas.flips.loader.api.FlipsLoader#initPlugin` in your `JavaPlugin#onEnable` method:

```
@Override
public void onEnable() {
    FlipsLoader.initPlugin(this);
}
```

And don't forget to call `ch.eneas.flips.loader.api.FlipsLoader#deinitPlugin` in your `JavaPlugin#onDisable` method:

```
@Override
public void onDisable() {
    FlipsLoader.deinitPlugin(this);
}
```

See `FlipsLoader` and `FlipsLoaderConfig` javadocs for more configuration options.
Configuration options include:
* Customize CommandAPI config
* Disable starting CommandAPI
* Enable FlipsLoader debug logs
* Ignore packages for class scanning (can be useful for shading)

### Dependency injection

1. Annotate you "bean" with `@Injectable`.
2. Annotate the constructor or fields to inject with `@Inject` (`ch.eneas.flips.loader.api.Inject`, not `javax.inject.Inject` which is also added by some bukkit dependency).