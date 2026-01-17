# Core Sistemi

## Genel Mimari

ArcadiaCore, tum altyapinin giris noktasi ve yasam dongusunu yoneten siniftir.
Moduller, storage ve cache gibi sistemler bu katmanda birlestirilir.

## Yasam Dongusu

### onLoad

1) ConfigService config'i yukler.
2) LogService kurulur ve ConfigService'e baglanir.
3) StorageManager kurulur ve storage secimi yapilir.
4) ModuleRegistry olusur, SystemModule register edilir.

### onEnable

1) Moduller config'e gore enable edilir.
2) Cache warmup tetiklenir.
3) Core enable loglari basilir.

### onDisable

1) Moduller disable edilir.
2) Storage kapatilir.
3) Core disable loglari basilir.

### reload

1) Moduller disable edilir.
2) Storage kapatilir.
3) Config yeniden okunur.
4) LogService yeni config ile guncellenir.
5) Storage yeniden init edilir.
6) Moduller yeniden enable edilir.
7) Cache warmup tekrar calisir.

## Modul Sistemi

- ModuleRegistry, modulleri isme gore kaydeder.
- Modul isimleri normalize edilir (kucuk harf).
- Config'te modules altinda true/false ile ac/kapat.

### SystemModule

- Arcadia komutlarini register eder.
- Cache event listener kaydeder (quit/disconnect flush).
- Storage maintenance servislerini asenkron tetikler.

## Guvenlik ve Dayaniklilik

- Reload sirasinda storage ve cache yeniden baslatilir.
- Hatalar LogService ile kategorize edilir.
- Core singleton (ArcadiaCore.getInstance) servis erisimini kolaylastirir.

## Dikkat

- onLoad sirasinda config ve logging hazir olmadan storage baslatilmaz.
- Moduller enable olana kadar komutlar register edilmez.
