Markwolf
========

Markdown extended plugin for JetBrains IDEs and owl🦉

Features
--------

* Format Table

For developers
--------------

### Run on development instance

```console
gradle runIde
```

### Build distribution

```console
gradle buildPlugin
```

### Release

- [ ] Update `build.gradle`
    - [ ] version
    - [ ] changeNotes
- [ ] Build distribution
- [ ] Commit
- [ ] Tagged
- [ ] Push
    - [ ] With `--tags`
    - [ ] Without `--tags`
- [ ] Upload `build/distributions/markwolf-${version}.zip` to [JetBrains market]

[JetBrains market]: https://plugins.jetbrains.com/plugin/edit?pluginId=14116
