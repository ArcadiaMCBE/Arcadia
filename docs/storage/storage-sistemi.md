# Storage Sistemi

## Mimari Bilesenler

- StorageManager: secim, init, fallback ve cache sarmalama.
- StorageProvider: aktif storage tipini temsil eder (JSON/PG).
- StorageRepository: veri okuma/yazma arayuzu.

## Storage Tipleri

### JSON

- Dosya bazli saklama.
- AtomicFileWriter ile guvenli yazim + file lock.
- Path: storage.json.path
- storage.json.shard ile alt klasor sharding aktif edilebilir.

### PostgreSQL

- HikariCP pool kullanir.
- Pool auto sizing (storage.postgresql.pool.auto) ile cekirdek/oyuncu sayisina gore max/min idle hesaplanir.
- PostgresConfig ile JDBC URL olusur.
- MigrationManager schema versiyonlarini uygular.

#### Pool Auto Sizing

- Hedef hesap: max(usableCores * coresMultiplier, ceil(expectedPlayers / playersPerConnection)).
- max-size/min-size clamp ile sinirlanir.
- min-idle-percent max pool uzerinden hesaplanir.
- 300-500 oyuncu icin expected-players degerini gercek hedefe gore ayarlayin.

## Secim ve Fallback

- storage.type=postgresql ise Postgres init denenir.
- Basarisiz olursa otomatik json fallback.
- LOG_STORAGE_FALLBACK ile loglanir.

## Retry/Backoff

- storage.retry.* ayarlarina gore yazim tekrar denenir.
- RetryExecutor her denemeyi loglar.

## Migration

- Schema migration: PostgresMigrations listesi uzerinden.
- Data migration (JSON <-> PG): StorageMaintenanceService ile opsiyonel.

## Repository API

- load, save, delete, exists
- loadAll, count, findByFilter

## Notlar

- Postgres kullanirken dogru pool ayarlari performans icin kritiktir.
- JSON dosya sayisi buyukse shard/alt klasor yapisi gerekli olabilir.
