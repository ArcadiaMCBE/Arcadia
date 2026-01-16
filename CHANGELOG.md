# Changelog

## 0.1.0

- Java 21 ve AllayMC icin Gradle yapisi kuruldu.
- Proje paket yapisi Arcadia.ClexaGod.arcadia olacak sekilde olusturuldu.
- ArcadiaCore giris sinifi eklendi; load/enable/disable akisi tanimlandi.
- Modul sistemi eklendi: Module arayuzu, ModuleRegistry ve SystemModule.
- YAML tabanli config servisi ve tipli CoreConfig modeli eklendi.
- Varsayilan config sablonu olusturuldu (core, storage, modules alanlari).
- I18n altyapisi eklendi: LangKeys ve MessageService.
- Dil dosyalari eklendi: en_US.json ve tr_TR.json.
- Varsayilan dil ayari ve fallback mantigi eklendi (core.default-lang).
- Config validasyonlari ve uyarilari eklendi (owner, server-name, default-lang, storage.type).
- Loglar I18n anahtarlarina tasindi ve standart hale getirildi.
- Resource kopyalama icin temel util eklendi.
- README ve .gitignore eklendi.
