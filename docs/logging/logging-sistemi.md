# Logging Sistemi

## Amac

- Loglari kategori bazli ayirmak.
- Debug seviyesini merkezi sekilde kontrol etmek.
- Gerekli durumlarda log yogunlugunu azaltmak.

## Yapilandirma

```
logging:
  default-level: "info"  # debug|info|warn|error
  use-core-debug: true
  include-category-prefix: true
  categories:
    core: "info"
    storage: "debug"
```

## Oncelik Kurali

1) Kategori override varsa onu kullanir.
2) override yoksa ve use-core-debug=true ve core.debug=true ise DEBUG olur.
3) aksi halde default-level kullanilir.

## Kategoriler

- core
- config
- module
- storage
- cache
- queue
- migration
- maintenance

## Pratik Ornekler

### Tum loglari debug yapmak

```
core:
  debug: true
logging:
  use-core-debug: true
```

### Sadece queue debug yapmak

```
core:
  debug: false
logging:
  categories:
    queue: "debug"
```

## Prefix

- include-category-prefix true ise log basina [kategori] eklenir.
- LogService bu prefix'i otomatik uygular.
