# Arcadia Core

AllayMC uzerinde Skyblock odakli bir sunucu altyapisidir. Moduler tasarimla
ozellikler ayri parcalar halinde gelistirilir ve tek bir proje icinde
yonetilir.

## Ozellikler

- Moduler mimari (ozellikler ayri moduller halinde).
- Coklu dil destegi (oyuncunun oyun dili algilanir).
- YML tabanli ayar sistemi.
- Java 21 uyumlu yapi.

## Teknik Ozet

- Java 21
- AllayMC (latest)
- Konfig: YAML

## Proje Yapisi

- `src/main/java/Arcadia/ClexaGod/arcadia`: ana paket
- `src/main/java/Arcadia/ClexaGod/arcadia/module`: moduller
- `src/main/java/Arcadia/ClexaGod/arcadia/i18n`: dil sistemi
- `src/main/resources/assets/lang`: dil dosyalari

## Konfigurasyon

`config.yml` icinde temel ayarlar yer alir:

- `core.owner`
- `core.server-name`
- `core.default-lang` (ornek: `tr_TR`)
- `storage.type`
- `modules.*` (modul ac/kapa)

## Dil Sistemi

- Dil dosyalari `assets/lang/<LangCode>.json` formatindadir.
- Anahtarlar `LangKeys` icinde tutulur.
- `MessageService` oyuncunun dilini kullanir, bulunamazsa `core.default-lang` geri dusus olur.

## Build

```bash
./gradlew build
```

Cikan jar dosyalari: `build/libs`

## Gelistirme Notlari

- Moduller `Module` arayuzunu uygular.
- Log ve mesajlar `I18n` uzerinden uretilir.
- Dil anahtarlari merkezi bir listede toplanir.

## Durum

Proje su anda altyapi ve iskelet asamasindadir. Moduller ve oyun sistemleri
kademeli olarak eklenecektir.
