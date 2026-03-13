
# FlipsLoader

CDI-like features for minecraft paper-based plugin development.

## Features:

* Dependency Injection (fields & constructor, but also programmatic)
* [CommandAPI](https://github.com/CommandAPI/CommandAPI)-Integration (automatic initialization & easy command registration, also supports automatic registration of CommandAPI-annotated commands)
* Automatic services with startup and shutdown hooks
* Automatic registration of bukkit event listeners

## How to use:

### Dependency

Currently not on mavencentral or any public maven repository, I think it's necessary to clone and install to local maven repository:

- `git clone https://github.com/eneasinfanger/FlipsLoader.git`
- `cd FlipsLoader`
- `mvn clean install`

Then add the following to your project's dependencies:

```
<dependency>
    <groupId>ch.eneas</groupId>
    <artifactId>FlipsLoader</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

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