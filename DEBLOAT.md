# Mi TV Stick sadeleştirme (debloat) rehberi

Bu rehber, Stick Home'u geliştirirken kendi Xiaomi Mi TV Stick'imde yaptığım sadeleştirmeyi anlatır. **Stick Home APK'sı bunların hiçbirini yapmaz** — aşağıdaki her adım isteğe bağlıdır ve senin ADB ile elle çalıştırman gerekir.

> **Uyarı:** Kendi sorumluluğundadır. Liste yalnızca **Xiaomi Mi TV Stick 1080p (MiTV-AESP0), Android 10** üzerinde test edildi. Paket adları cihaza, bölgeye ve yazılım sürümüne göre değişir; başka bir cihazda listeyi körü körüne kopyalama. Başka cihazlar için [Kutu Android TV Guide](https://github.com/bevcko16/kutu-android-tv-guide) daha uygun: Claude'a senin cihazını inceletip ona özel, güvenli bir liste çıkarttırıyor. Buradaki kurallar da oradan esinlendi.

## Kurallar

1. **Root yok, silme yok.** Sadece `pm disable-user --user 0` kullan. `pm uninstall` kullanma; sistem paketini silmek fabrika ayarına dönmeden geri alınamayabilir.
2. **Her kapattığın paketin geri alma komutunu not et:** `pm enable --user 0 <paket>`.
3. **Az az ilerle.** Birkaç paket kapat, cihazı yeniden başlat, kumanda, ses, Wi-Fi, YouTube/Netflix gibi temel şeyleri dene; sorun yoksa devam et.
4. **Uygulama verisini/önbelleğini "optimizasyon" diye silme.**
5. **Sistemle kimlik paylaşan paketlere dokunma.** Şununla kontrol et:
   ```
   adb shell dumpsys package <paket> | findstr sharedUser
   ```
   `android.uid.system` görürsen o paketi kapatma (örneğin bu cihazda `com.xiaomi.statistic` ve `com.mitv.download.service` böyle).

### Asla kapatma

`com.android.providers.tv` (TV kanal veritabanı), Google Play Hizmetleri / Play Store / `com.google.android.gsf`, Chromecast (`com.google.android.apps.mediashell`), Google Asistan ve sesli arama (`com.google.android.katniss`), klavye (Gboard), Bluetooth, Wi-Fi, HDMI-CEC, DRM/Widevine ile ilgili paketler, bir de kullandığın yayın uygulamaları.

## Kapattığım paketler

Kapatmadan önce "bunu kullanıyor muyum?" diye kendine sor. **Not** sütununda, kullanıyorsan kapatmaman gerekenleri belirttim.

### Xiaomi reklam, içerik ve telemetri

| Paket | Ne işe yarıyor | Not |
|---|---|---|
| `com.miui.tv.analytics` | Xiaomi kullanım verisi toplama | |
| `com.mitv.tvhome.atv` | Xiaomi PatchWall içerik uygulaması | |
| `com.mitv.tvhome.michannel` | Xiaomi kanal akışı | |
| `com.mitv.tvhome.mitvplus` | Xiaomi "TV Plus" içerik servisi | |
| `com.mitv.videoplayer` | Xiaomi'nin kendi video oynatıcısı | Yukarıdaki üçü kapanınca işlevsiz kalıyor |
| `com.xm.webcontent` | Xiaomi tanıtım içeriği gösterici | |
| `com.xiaomo.tv.milegal` | Tek seferlik yasal bildirim ekranı | |
| `android.autoinstalls.config.xioami.mibox3` | Eski Mi Box 3'ten kalma otomatik kurulum ayarı | |

### Google hata raporu ve geri bildirim

| Paket | Ne işe yarıyor | Not |
|---|---|---|
| `com.google.android.tv.bugreportsender` | Hata raporu gönderici | |
| `com.google.android.feedback` | Google geri bildirim servisi | |

### Kullanmadığım Google uygulamaları

| Paket | Ne işe yarıyor | Not |
|---|---|---|
| `com.google.android.play.games` | Play Oyunlar | Oyun oynuyorsan kapatma |
| `com.google.android.music` | Eski Play Müzik | |
| `com.google.android.videos` | Play Filmler ve TV | Film kiralıyorsan kapatma |
| `com.google.android.tvrecommendations` | Ana ekrandaki öneri rafları | Stick Home öneri göstermez; Google'ın ana ekranında önerileri istiyorsan kapatma |
| `com.google.android.marvin.talkback` | TalkBack ekran okuyucu | Görme desteği kullanıyorsan **kesinlikle kapatma** |
| `com.google.android.syncadapters.calendar` | Google Takvim senkronizasyonu | **Stick Home'daki takvimi kullanacaksan kapatma** — kapalıyken yeni etkinlikler gelmez |
| `com.google.android.syncadapters.contacts` | Kişi senkronizasyonu | |

### TV'de gereksiz Android parçaları

| Paket | Ne işe yarıyor | Not |
|---|---|---|
| `com.android.printspooler` | Yazdırma servisi | |
| `com.android.camera2` | Kamera uygulaması | Bu cihazda kamera yok |
| `com.android.settings.intelligence` | Ayarlar içinde akıllı arama | |
| `com.android.htmlviewer` | HTML dosya görüntüleyici | |

Tek paket kapatmak:
```
adb shell pm disable-user --user 0 com.miui.tv.analytics
```

Kapalı paketleri listelemek:
```
adb shell pm list packages -d
```

## Stick Home'u ana ekran yapmak

Bu cihazda `set-home-activity` tek başına yetmedi, Google TV ana ekranı öncelikli kalmaya devam etti. Önce Stick Home'u seçtim, işe yaramayınca Google'ın ana ekranını kapatıp tekrar seçtim:

```
adb shell cmd package set-home-activity io.github.mehmetemreak.stickhome/.HomeActivity
adb shell pm disable-user --user 0 com.google.android.tvlauncher
adb shell cmd package set-home-activity io.github.mehmetemreak.stickhome/.HomeActivity
```

Hangi ana ekranın seçili olduğunu görmek:
```
adb shell cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME
```

## Geri alma

Tek paket:
```
adb shell pm enable --user 0 <paket>
```

Bu rehberdeki her şeyi geri almak (PowerShell):
```powershell
"com.miui.tv.analytics","com.mitv.tvhome.atv","com.mitv.tvhome.michannel","com.mitv.tvhome.mitvplus","com.mitv.videoplayer","com.xm.webcontent","com.xiaomo.tv.milegal","android.autoinstalls.config.xioami.mibox3","com.google.android.tv.bugreportsender","com.google.android.feedback","com.google.android.play.games","com.google.android.music","com.google.android.videos","com.google.android.tvrecommendations","com.google.android.marvin.talkback","com.google.android.syncadapters.calendar","com.google.android.syncadapters.contacts","com.android.printspooler","com.android.camera2","com.android.settings.intelligence","com.android.htmlviewer","com.google.android.tvlauncher" | ForEach-Object { adb shell pm enable --user 0 $_ }
adb shell cmd package set-home-activity com.google.android.tvlauncher/.MainActivity
```

Sonra cihazı yeniden başlat.
