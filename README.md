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
1. Download the latest version from [Releases](https://github.com/xLevitate/Quill/releases)
2. Place the JAR file in your server's `plugins` folder
3. Restart your server

### 2. Development Integration
Make sure you use the latest version, which can be found at the top of this readme.

#### Gradle (Kotlin DSL)
```kotlin
repositories {
    maven("https://jitpack.io")
}

dependencies {
    compileOnly("com.github.xLevitate:Quill:VERSION")
}
```

#### Gradle (Groovy)
```groovy
repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    compileOnly 'com.github.xLevitate:Quill:VERSION'
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
        <groupId>com.github.xLevitate</groupId>
        <artifactId>Quill</artifactId>
        <version>VERSION</version>
        <scope>provided</scope>
    </dependency>
</dependencies>
```

## Documentation

For detailed documentation, please visit our [Wiki](https://github.com/xLevitate/Quill/wiki).

## Requirements

- Java 17 or higher
- Paper 1.20.4 or higher
- (Optional) PlaceholderAPI for placeholder support
- (Optional) Vault for economy support

## Support

If you encounter any issues or have questions:
1. Check the [Wiki](https://github.com/xLevitate/Quill/wiki)
2. Open an [Issue](https://github.com/xLevitate/Quill/issues)

## Contributing

Contributions are welcome! Feel free to:
1. Fork the repository
2. Create a feature branch
3. Submit a Pull Request

## License

This project is licensed under the GNU General Public License v3.0 - see the LICENSE file for details.
