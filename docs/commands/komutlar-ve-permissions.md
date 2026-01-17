# Komutlar ve Permission

## Arcadia Komutu

Komutlar SystemModule uzerinden register edilir.
Permission node: arcadia.command

### /arcadia health

- Core, runtime, storage, queue, cache bolumleri raporlanir.
- Storage tipi, queue doluluk ve cache metrikleri gorunur.

### /arcadia health full

- /health raporuna ek olarak repo bazli cache metrikleri ekler.

### /arcadia health check

- JSON ve Postgres icin aktif saglik kontrolleri calistirir.
- Islem async calistigi icin once "calisiyor" mesaji gelir.

## Permission Notlari

- arcadia.command: tum arcadia komutlari icin gerekli.
- Ileride role bazli permission altyapisi ayri bir katmanda planlanacak.
