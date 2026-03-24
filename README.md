# Properties Plugin for IntelliJ

Browse all `.properties` files across your entire project in a single tool window.

## Features

- **Live search** — filter properties by key name or value as you type
- **Duplicates detection** — find properties defined in multiple files with the same key
- **Sortable columns** — click File, Name, or Value headers to sort; sorting by Name or Value shows the file path on every row
- **Quick navigation** — double-click any property to open its file in the editor

## Installation

1. Download the latest release from the [releases page](https://github.com/ignaciotcrespo/propertiesplugin/releases) or build from source
2. In IntelliJ, go to **Settings > Plugins > Install Plugin from Disk**
3. Select the `.zip` file and restart

## Usage

Open the **Properties!** tool window from the right sidebar. All `.properties` files in your project are scanned and displayed in a table.

- **Refresh** — click to rescan the project
- **Duplicates only** — check to show only keys that appear in more than one file
- **Search** — type to filter by key name or value (case-insensitive)
- **Sort** — click any column header to sort; click again to reverse

## Building from source

```bash
./gradlew build
```

The plugin zip will be in `build/distributions/`.

## Requirements

- IntelliJ IDEA 2023.3+
- Java 17+

## License

This project is licensed under the [MIT License](LICENSE).

## Links

- [Repository](https://github.com/ignaciotcrespo/propertiesplugin)
- [Donate](https://github.com/sponsors/ignaciotcrespo)
