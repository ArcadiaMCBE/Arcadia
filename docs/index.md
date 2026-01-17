# Arcadia Dokumantasyon

Bu klasor Arcadia altyapisi ve sistemlerini Turkce olarak anlatir.
Her sistem kendi klasorunde detayli ve pratik sekilde dokumante edilir.

## Icerik

- core/core-sistemi.md: Core yasam dongusu, modul sistemi ve boot akisi.
- config/config-sistemi.md: Config yapisi, validasyonlar ve reload mantigi.
- logging/logging-sistemi.md: Log seviyeleri, kategori bazli ayar, debug kurallari.
- storage/storage-sistemi.md: JSON/PostgreSQL storage mimarisi, migration ve retry.
- cache/cache-sistemi.md: Cache davranisi, policy, metrics ve flush akisi.
- queue/async-write-queue.md: Async yazim kuyrugu ve overflow stratejileri.
- maintenance/storage-maintenance.md: Health check, seeding ve veri migrasyonu.
- commands/komutlar-ve-permissions.md: Arcadia komutlari ve permission yapisi.
- i18n/i18n-sistemi.md: Dil sistemi, lang keys ve mesaj isleme.

## Dokumantasyon Kurallari

- Her yeni sistem icin ayri dosya ac.
- Her yeni ayar eklenirse ilgili config dokumanini guncelle.
- Her yeni log/mesaj icin i18n dokumani guncelle.
- Degisiklikten sonra ornek config veya akisi yaz.
