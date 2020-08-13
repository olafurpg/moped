---
id: configuration
title: Configuration
---

| Syntax  | Dependency                | Extension      | Syntax errors | Type errors |
| ------- | ------------------------- | -------------- | ------------- | ----------- |
| JSON    | `com.lihaoyi::ujson`      | `*.json`       | ✅            | ✅          |
| HOCON   | `org.ekrich::sconfig`     | `*.conf`       | ✅            | ✅          |
| YAML    | `org.yaml:snakeyaml`      | `*.{yml,yaml}` | ✅            | ✅          |
| TOML    | `tech.sparse::toml-scala` | `*.toml`       | ✅            | 🍠          |
| Dhall   | `org.dhallj::dhall-scala` | `*.dhall`      | ✅            | 🍠          |
| Jsonnet | `com.lihaoyi::sjsonnet`   | `*.jsonnet`    | ✅            | 🍠          |

**🍠**: error messages are not reported with source positions.
