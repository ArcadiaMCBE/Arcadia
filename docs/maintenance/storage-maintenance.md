# Storage Maintenance

## Amaç

StorageMaintenanceService acilista storage sagligini kontrol eder ve opsiyonel bakim islerini yapar.

## Health Check

- JSON write testi (storage.health.json-write-test)
- PostgreSQL baglanti testi (storage.health.postgres-test)
- connection-timeout-ms ile baglanti timeout kontrol edilir.

## Seeding

- owner, server-name, created-at meta kayitlarini olusturur.
- storage.seed.enabled ile ac/kapat.

## Data Migration

- JSON <-> PostgreSQL arasinda meta veri gecisi yapar.
- storage.migration.enabled false ise calismaz.
- dry-run true ise yazim yapmadan raporlar.
- skip-existing true ise mevcut kayitlar atlanir.

## Calisma Sirasi

1) Health check
2) Data migration (opsiyonel)
3) Seeding

## Dikkat

- Data migration sadece meta repository icin varsayilan.
- Postgres ayarlari gecersizse migration skip edilir.
