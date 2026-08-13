# Better Rune Loadouts

A RuneLite plugin. (TODO: describe what it does.)

Scaffolded from the same structure as [Boss Tracker](https://github.com/camjewell11/BossingInfo) —
package `com.camjewell.betterruneloadouts`, Java 11 target

Credit to SheeplerSN on reddit for the mockup.

## Building

Requires a JDK compatible with RuneLite's `example-plugin` template (Java 11 target).

```bash
./gradlew shadowJar   # Build the plugin JAR
./gradlew run         # Launch a RuneLite dev client with the plugin loaded
./gradlew test        # Run tests
```

`./gradlew run` launches an unauthenticated development client — log in via a
[Jagex account](https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts) to test in-game.

## License

BSD-2-Clause — see [LICENSE](LICENSE).
