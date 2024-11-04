# Quill
[![](https://jitpack.io/v/xLevitate/Quill.svg)](https://jitpack.io/#xLevitate/Quill)

Quill is a comprehensive utility library for Paper plugins, providing a wide range of tools and systems to simplify plugin development. It offers everything from configuration management to advanced item creation, all with a clean and intuitive API.

## Features

- 🔧 **Configuration System** - Annotation-based configuration with automatic updating
- 💬 **Chat System** - Advanced message handling with MiniMessage support
- 💾 **JSON Storage** - Flexible and type-safe data storage system
- 📦 **Item Management** - Powerful item creation and manipulation
- 📋 **Scoreboard System** - Simple yet flexible scoreboard creation
- 🎮 **Events System** - Streamlined event handling with filtering
- 🛠️ **Utility Classes** - Comprehensive collection of utility methods
- 🔌 **Plugin Hooks** - Built-in integration with PlaceholderAPI and Vault

## Installation

### 1. Server Installation
First, download and install the Quill plugin on your server:
1. Download the latest version from [Releases](https://github.com/YourUsername/Quill/releases)
2. Place the JAR file in your server's `plugins` folder
3. Restart your server

### 2. Development Integration

#### Gradle (Kotlin DSL)
```kotlin
repositories {
    maven("https://jitpack.io")
}

dependencies {
    compileOnly("com.github.YourUsername:Quill:VERSION")
}
```

#### Gradle (Groovy)
```groovy
repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    compileOnly 'com.github.YourUsername:Quill:VERSION'
}
```

#### Maven
```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>com.github.YourUsername</groupId>
        <artifactId>Quill</artifactId>
        <version>VERSION</version>
        <scope>provided</scope>
    </dependency>
</dependencies>
```

## Quick Start

### Chat System
```java
// Send formatted message
Chat.sendMessage(player, "<gradient:green:blue>Welcome to the server!</gradient>");

// Send title
Chat.sendTitle(player, "<gold>Welcome!", "<gray>Enjoy your stay");
```

### Configuration
```java
@Configuration("config.yml")
public class MyConfig {
    @Comment("Enable or disable the plugin")
    private boolean enabled = true;

    @Comment("Custom message")
    private String message = "<green>Hello!</green>";
}
```

### Item Creation
```java
ItemStack item = new ItemWrapper()
    .plugin(plugin)
    .material(Material.DIAMOND_SWORD)
    .name("<gradient:blue:purple>Mystic Blade</gradient>")
    .lore(Arrays.asList(
        "<gray>A legendary weapon",
        "<blue>Damage: +50</blue>"
    ))
    .build();
```

### Event Handling
```java
Events.listen(plugin, PlayerJoinEvent.class)
    .filter(event -> !event.getPlayer().hasPlayedBefore())
    .handle(event -> {
        Player player = event.getPlayer();
        Chat.sendMessage(player, "<green>Welcome to the server!");
    });
```

## Documentation

For detailed documentation, please visit our [Wiki](https://github.com/YourUsername/Quill/wiki).

## Requirements

- Java 17 or higher
- Paper 1.20.4 or higher
- (Optional) PlaceholderAPI for placeholder support
- (Optional) Vault for economy support

## Support

If you encounter any issues or have questions:
1. Check the [Wiki](https://github.com/YourUsername/Quill/wiki)
2. Open an [Issue](https://github.com/YourUsername/Quill/issues)

## Contributing

Contributions are welcome! Feel free to:
1. Fork the repository
2. Create a feature branch
3. Submit a Pull Request

## License

This project is licensed under the GNU General Public License v3.0 - see the LICENSE file for details.
