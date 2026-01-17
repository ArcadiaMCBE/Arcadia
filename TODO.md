# TODO

## Altyapi

- DB ayar/yonetimi
  - Postgres icin connection test/health check komutu
  - Basit DB seeding (ilk kurulum tablolar + temel meta kayitlar)
  - JSON ↔ Postgres veri migrasyon araci (gecis kolayligi)
- Cache politikalari
  - Cache metrikleri (hit/miss, queue size, flush sayisi)
- I/O performans
  - JSON yazimlarinda shard/alt klasor yapisi (binlerce dosyada fs yavaslamasin)
  - Postgres connection pool limitlerini otomatik ayarlama rehberi
- Log/izleme
  - Altyapi log seviyeleri (debug/info) ve log kategori ayarlari
  - Debug modu kullanilmiyor: CoreConfig.isDebug() var ama log seviyesi kontrolu yok
  - Plugin icin basit health raporu komutu
- Configuration management
- Testing / validation
  - Storage + cache unit testleri (mock)
  - Integration test senaryolari (JSON/PG, crash sim, queue overflow)
- Security / data integrity
  - JSON dosya yazim lock (AtomicFileWriter atomic move yapiyor ama concurrent write korumasi yok; ayni dosyaya eszamanli yazimda sorun olabilir)
  - Postgres transaction kullanimi (kritik kayitlar icin)
- Migration rollbacks
  - Migration rollback destegi yok; hatali migration'i geri alma mekanizmasi eklenmeli
- Queue onceliklendirme
  - AsyncWriteQueue icin task onceligi yok; kritik yazimlar icin priority desteklenmeli
- Versioning
  - StorageRecord.getDataVersion() icin veri migrasyon mantigi (record-level)
  - Schema + record version uyumu raporu
