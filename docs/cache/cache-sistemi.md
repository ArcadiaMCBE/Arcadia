# Cache Sistemi

## Bilesenler

- StorageCacheManager: cache yasamini yonetir.
- RecordCache: LRU benzeri map + TTL mantigi.
- CachedRepository: storage repository'yi cache ile sarar.

## Akis

1) load: cache hit ise direkt doner, miss ise storage'dan okur ve cache'e ekler.
2) save: cache'e dirty olarak yazilir, queue ile storage'a gonderilir.
3) flush: dirty kayitlar storage'a yazilir, expired/overflow kayitlar atilir.

## Policy Sistemi

- cache.policies.default tum repo'lara uygulanir.
- cache.policies.repos.<repo> ile override yapilir.
- enabled=false ise cache bypass edilir.
- flush-on-save=true ise write-through davranisi olur.

## Warmup

- cache.warmup.enabled true ise acilista on-yukleme yapilir.
- max-entries-per-repo ile limitlenir.

## Metrics

- hit/miss
- flush sayisi
- write task sayisi
- evict expired/overflow
- queue size (last + max)

## Flush-on-player-quit

- PlayerQuit/Disconnect eventlerinde flush tetiklenir.
