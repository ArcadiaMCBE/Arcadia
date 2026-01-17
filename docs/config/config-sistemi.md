# Config Sistemi

## Dosya Konumu

- Ana dosya: plugins/Arcadia/config.yml
- Ilk acilista default config otomatik kopyalanir.

## Ana Basliklar

- core: owner, server-name, debug, default-lang
- logging: default-level, use-core-debug, include-category-prefix, categories
- storage: type, json, queue, retry, health, seed, migration, postgresql
- cache: ttl, max entries, flush interval, warmup, policies
- modules: modul enable/disable

## Ornek Yapilandirma

```
core:
  owner: "ClexaGod"
  server-name: "Arcadia Skyblock"
  debug: false
  default-lang: "tr_TR"

logging:
  default-level: "info"
  use-core-debug: true
  include-category-prefix: true
  categories:
    storage: "debug"

storage:
  type: "json"
  json:
    path: "data"
    shard:
      enabled: false
      strategy: "hash"
      depth: 2
      chars-per-level: 2
      migrate-legacy-on-read: false
  postgresql:
    pool:
      max-size: 10
      min-idle: 2
      auto:
        enabled: false
        min-size: 8
        max-size: 32
        min-idle-percent: 25
        cores-multiplier: 2
        reserve-cores: 1
        expected-players: 0
        players-per-connection: 50

cache:
  enabled: true
  policies:
    default:
      enabled: true
      flush-on-save: false
      flush-timeout-ms: 2000
    repos:
      meta:
        enabled: true
        flush-on-save: true
        flush-timeout-ms: 2000
```

## Validasyon Mantigi

- Her alan CoreConfig icinde kontrol edilir.
- Gecersiz degerlerde varsayilan kullanilir.
- Uyarilar i18n log key'leri ile basilir.

## Reload Davranisi

- ConfigService.reload ile config yeniden okunur.
- Storage ve cache guvenli sekilde yeniden init edilir.
- LogService yeni config'e gore seviye gunceller.

## Notlar

- logging.categories altinda gecersiz kategori yazilirsa ignore edilir.
- cache.policies.repos anahtari repository ismi ile eslesir.
- storage.json.shard.enabled true ise json dosyalari alt klasorlere dagitilir.
- storage.postgresql.pool.auto.enabled true ise max-size/min-idle otomatik hesaplanir.
