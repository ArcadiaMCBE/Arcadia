# Allay Notlari

Bu dosya, Allay uzerinden ogrendigimiz altyapi bilgilerini kaydetmek icin
kullanilir.

## Paralel ve Async Yapi

- Allay, `virtual thread pool` kullanir ve async isler bu havuza atilabilir.
  - Kaynak: `Allay/server/src/main/java/org/allaymc/server/AllayServer.java`
- `compute thread pool` (ForkJoin) CPU agir isler icin kullanilir.
  - Kaynak: `Allay/server/src/main/java/org/allaymc/server/AllayServer.java`
- Dimension tick islemleri `tickDimensionInParallel` ayari aciksa paralel calisir.
  - Kaynak: `Allay/server/src/main/java/org/allaymc/server/world/AllayWorld.java`

## Scheduler

- Scheduler async calistirilabilen task’lari `virtual thread pool` uzerinden yurutur.
  - Kaynak: `Allay/server/src/main/java/org/allaymc/server/scheduler/AllayScheduler.java`

## Event Bus

- EventBus, `virtual thread pool` ile olusur; handler’lar async davranabilir.
  - Kaynak: `Allay/server/src/main/java/org/allaymc/server/AllayServer.java`

## Altyapi Ic in Kullanim Notlari

- DB/JSON yazimlari gibi I/O agir isler: `virtual thread pool` uzerinde.
- CPU agir islemler: `compute thread pool`.
- World state degisimi: ilgili world scheduler’a geri donulmeli.
