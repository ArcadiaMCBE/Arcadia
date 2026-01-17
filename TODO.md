# TODO

## Altyapi
- Log/izleme
- Configuration management
- Komut ve permission altyapisi (komut kaydi/izin kontrolu/role bazli planlama)
- Testing / validation
  - Storage + cache unit testleri (mock)
  - Integration test senaryolari (JSON/PG, crash sim, queue overflow)
- Security / data integrity
  - Postgres transaction kullanimi (kritik kayitlar icin)
- Migration rollbacks
  - Migration rollback destegi yok; hatali migration'i geri alma mekanizmasi eklenmeli
- Queue onceliklendirme
  - AsyncWriteQueue icin task onceligi yok; kritik yazimlar icin priority desteklenmeli
- Versioning
  - StorageRecord.getDataVersion() icin veri migrasyon mantigi (record-level)
  - Schema + record version uyumu raporu
