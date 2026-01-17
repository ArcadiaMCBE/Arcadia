# I18n Sistemi

## Dosyalar

- LangKeys: tum anahtarlarin merkezi listesi.
- MessageService: oyuncu diline gore prefix + ceviri uygular.
- I18nUtil: CommandSender icin dil secimini yapar.
- assets/lang/en_US.json ve tr_TR.json: ceviri metinleri.

## Kullanım

- Log mesajlari LangKeys ile I18n.get().tr kullanir.
- Komut mesajlari I18nUtil.tr ile sender'in diline gore cevrilir.
- MessageService prefix ile renkli mesaj basar.

## Yeni Mesaj Eklerken

1) LangKeys'e yeni key ekle.
2) en_US.json ve tr_TR.json'a yeni metni ekle.
3) MessageService veya I18nUtil ile kullan.
