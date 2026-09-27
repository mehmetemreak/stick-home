# Stick Home

Xiaomi Mi TV Stick için hafif, root gerektirmeyen, özel bir Android TV ana ekranı (launcher).

1 GB RAM'li eski bir TV çubuğunda akıcı çalışacak şekilde yazıldı: arka planda çalışan servis yok, sürekli internet sorgusu yok, APK ~360 KB.

> *A lightweight, root-free custom Android TV launcher for the Xiaomi Mi TV Stick. The UI is Turkish-only for now.*

![Stick Home ana ekranı](docs/screenshots/home.png)

## Claude Code ile kurulum (önerilen)

Komutlarla uğraşmak istemiyorsan kurulumu [Claude Code](https://claude.com/claude-code) senin için adım adım yapabilir: TV'ye bağlanma, APK kurulumu, ana ekran değişimi, izinler ve istersen cihaz sadeleştirme. Her komuttan önce ne yapacağını, nasıl geri alınacağını anlatır ve onayını bekler; root kullanmaz, hiçbir şeyi silmez.

1. Bilgisayarına Claude Code'u kur ve boş bir klasörde başlat.
2. TV'de Geliştirici seçeneklerini aç (Claude da nasıl yapılacağını anlatır).
3. Claude'a şunu yaz:

   ```
   https://raw.githubusercontent.com/mehmetemreak/stick-home/main/CLAUDE-GUIDE.md dosyasını oku ve Android TV'me Stick Home kurmama adım adım yardım et.
   ```

Claude'a verdiğin talimatların tamamı [CLAUDE-GUIDE.md](CLAUDE-GUIDE.md) dosyasında; kurulumdan önce okuyabilirsin.

## Özellikler

- **Sade raf:** Sık kullandığın uygulamalar tek sırada. Basılı tut → taşı, kaldır, uygulama bilgisi.
- **Tüm Uygulamalar ekranı:** Seç aç, basılı tut ana ekrana ekle/çıkar.
- **Temalar:** İki fotoğraflı tema, koyu/açık gradyanlar ve düz siyah "Karanlık".
- **Saat, selamlama, hava durumu:** Şehri ayarlardan bir kere seçersin, konum izni istemez.
- **Takvim:** Cihazdaki Google hesabının önümüzdeki 3 gün / 1 hafta / 1 ay içindeki etkinlikleri (yerel okuma, ek giriş yok). Google Takvim senkronizasyonunun açık olması gerekir.
- **Yaklaşan maçlar:** Seçtiğin liglerden önümüzdeki 1 saat / 1 gün / 3 gün içindeki maçlar.
- **Kumanda tuşu yönlendirme:** Netflix ve Prime Video tuşlarını istediğin uygulamaya bağla (varsayılan: değiştirme).
- **Arka plan temizliği:** Ana ekrana dönünce son kullanılan uygulamalar bellekten atılır. Müzik çalan uygulamalara dokunulmaz.

## Ekran görüntüleri

| Ana ekran, aşağı ok ile açılan kısayollar | Tüm Uygulamalar |
|---|---|
| ![Kısayol çipleri açık ana ekran](docs/screenshots/home-chips.png) | ![Tüm Uygulamalar ekranı](docs/screenshots/all-apps.png) |

| Arayüz Ayarları |
|---|
| ![Arayüz Ayarları ekranı](docs/screenshots/settings.png) |

*Görsellerdeki takvim etkinlikleri ve maçlar örnek verilerdir.*

## Uyumluluk

Xiaomi Mi TV Stick 1080p (MiTV-AESP0), Android 10 üzerinde test edildi. Android 10+ çalıştıran başka Android TV cihazlarında da açılması beklenir. Ama kumanda tuşu kodları (Netflix `193`, Prime Video `194`, Tüm Uygulamalar `284`) Xiaomi kumandasına göre; başka kumandalarda bu tuşlar sadece yönlendirilmez, başka bir şey bozulmaz.

## Elle kurulum

Bilgisayarında [ADB](https://developer.android.com/tools/releases/platform-tools) kurulu olmalı ve TV'de Geliştirici seçenekleri → USB/Ağ hata ayıklama açık olmalı.

1. En son sürümü [buradan indir](https://github.com/mehmetemreak/stick-home/releases/latest/download/stick-home.apk) (tüm sürümler: [Releases](../../releases)) ve yükle:
   ```
   adb connect <TV-IP-ADRESİ>
   adb install stick-home.apk
   ```
2. Stick Home'u ana ekran yap:
   ```
   adb shell cmd package set-home-activity io.github.mehmetemreak.stickhome/.HomeActivity
   ```
   Kumandadaki Home tuşuna bas. Hâlâ eski ana ekran açılıyorsa [DEBLOAT.md → Stick Home'u ana ekran yapmak](DEBLOAT.md#stick-homeu-ana-ekran-yapmak) bölümüne bak.
3. **(İsteğe bağlı) Kumanda tuşları için:** TV'de Ayarlar → Cihaz Tercihleri → Erişilebilirlik → Stick Home → Aç. Sonra Arayüz Ayarları'ndan tuşların neyi açacağını seç.
4. **(İsteğe bağlı) Arka plan temizliği için:**
   ```
   adb shell appops set io.github.mehmetemreak.stickhome GET_USAGE_STATS allow
   ```

Ayarlara ana ekranda aşağı ok → **Arayüz Ayarları** ile ulaşılır.

### Geri alma

Orijinal ana ekrana dönmek için (Mi TV Stick'te Google TV launcher'ı). İlk satır, ana ekranı daha önce kapattıysan onu geri açar; kapatmadıysan zararsızdır:

```
adb shell pm enable --user 0 com.google.android.tvlauncher
adb shell cmd package set-home-activity com.google.android.tvlauncher/.MainActivity
adb uninstall io.github.mehmetemreak.stickhome
```

Başka bir cihazda orijinal launcher'ın adını `adb shell cmd package resolve-activity -a android.intent.action.MAIN -c android.intent.category.HOME` ile Stick Home'u kurmadan önce öğrenebilirsin.

## Stick Home neyi değiştirmez?

APK sadece bir ana ekran uygulaması kurar. Cihazındaki başka hiçbir uygulamayı kapatmaz, silmez, ayarlarını değiştirmez; normal bir Android uygulamasının bunu yapma yetkisi de yok. Stick Home'u silersen geriye bir şey kalmaz.

Cihazı sadeleştirmek (Xiaomi reklam/telemetri uygulamalarını kapatmak vb.) ayrı ve isteğe bağlı bir iş. Kendi cihazımda yaptıklarımı, geri alma komutlarıyla birlikte [DEBLOAT.md](DEBLOAT.md) dosyasında anlattım.

## Gizlilik

- Reklam, analitik, hesap yok. Hiçbir veri bir sunucuya gönderilmez.
- İnternete sadece iki yere çıkar, ikisi de ana ekrana dönüldüğünde en fazla 30 dakikada bir:
  - [Open-Meteo](https://open-meteo.com): hava durumu (sadece şehir seçtiysen) ve şehir araması.
  - [TheSportsDB](https://www.thesportsdb.com): maç fikstürü.
- **Erişilebilirlik izni** sadece kumanda tuşlarını yönlendirmek için kullanılır. Ekran içeriğini okumaz, yazdıklarını görmez, tuşları kaydetmez.
- **Takvim** cihazda yerel olarak okunur.
- **Kullanım erişimi** sadece hangi uygulamaların yakın zamanda kullanıldığını görüp onları bellekten atmak için kullanılır.

## Kaynaktan derleme

JDK 17 ve Android SDK (API 34) gerekir:

```
./gradlew assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`

## Teşekkür

Bu proje, [bevcko16'nın Kutu Android TV Guide](https://github.com/bevcko16/kutu-android-tv-guide) rehberinden ilham alarak başladı. Rehberdeki Claude Code + ADB iş akışıyla önce cihazı optimize ettim, sonra bu launcher'ı Mi TV Stick için sıfırdan yazdım. Kurulumu Claude'a bir rehber dosyasıyla yaptırma fikri de oradan geliyor.

## Lisans

[MIT](LICENSE). Tema fotoğrafları (`bg_photo_*.jpg`) da aynı lisansla paylaşılmıştır.
