# TODO

## Altyapi

- DB ayar/yonetimi
  - Postgres icin connection test/health check komutu
  - Basit DB seeding (ilk kurulum tablolar + temel meta kayitlar)
  - JSON ↔ Postgres veri migrasyon araci (gecis kolayligi)
- Cache politikalari
  - Kritik kayitlarda “flush-on-save” opsiyonu
  - Oyuncu cikisi/ada kapanisi gibi event’lerde manuel flush
  - Cache metrikleri (hit/miss, queue size, flush sayisi)
- Error handling + retry
  - DB/JSON write hatalarinda retry/backoff
  - Queue doluysa alternatif strateji (drop, sync write, etc.)
- I/O performans
  - JSON yazimlarinda shard/alt klasor yapisi (binlerce dosyada fs yavaslamasin)
  - Postgres connection pool limitlerini otomatik ayarlama rehberi
- Log/izleme
  - Altyapi log seviyeleri (debug/info) ve log kategori ayarlari
  - Plugin icin basit health raporu komutu
- Configuration management
  - Config reload akisi (safe reload: cache + storage yeniden baslatma)
  - Config uzerinden cache/queue parametrelerini runtime degistirme
- Testing / validation
  - Storage + cache unit testleri (mock)
  - Integration test senaryolari (JSON/PG, crash sim, queue overflow)
- Security / data integrity
  - JSON dosya yazim lock (ayni kayida esit anda yazim)
  - Postgres transaction kullanimi (kritik kayitlar icin)
- Versioning
  - StorageRecord.getDataVersion() icin veri migrasyon mantigi (record-level)
  - Schema + record version uyumu raporu
