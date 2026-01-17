# Async Write Queue

## Amac

- Disk/DB yazimlarini main thread'den ayirmak.
- Backpressure ve overload durumlarini kontrol etmek.

## Keyed Task

- WriteTask key'i varsa ayni key'e ait yazimlar seri hale gelir.
- Bu sayede ayni kayit icin sira korunur.

## On-Full Stratejileri

- drop: kuyruk dolunca yazim atilir.
- block: belirli sure bekler, timeout olursa sync fallback.
- sync: kuyruk doluysa hemen ayni thread'de calistirir.

## Shutdown Akisi

1) Accepting kapatilir, worker durdurulur.
2) Timeout kadar drain denenir.
3) Bosta kalmayan task'lar inline calistirilir (force-drain).

## Ayarlar

- storage.queue.max-size
- storage.queue.on-full
- storage.queue.full-timeout-ms
