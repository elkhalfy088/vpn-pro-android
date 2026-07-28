# VPN Pro — Android App v2.0

تطبيق VPN حقيقي يعمل بـ WireGuard + Firebase للمشاركة الفورية للسيرفرات بين جميع المستخدمين.
يمرّر **جميع ترافيك الإنترنت** (صور، فيديو، أي موقع أو تطبيق) عبر السيرفر.

---

## 🆕 الجديد في v2.0

| الميزة | الوصف |
|--------|-------|
| 🗑️ **حذف السيرفرات** | احذف أي سيرفر بضغطة واحدة مع تأكيد |
| ✏️ **تعديل السيرفرات** | عدّل الاسم والموقع والبيانات الأساسية |
| 📋 **استيراد Config** | الصق إعدادات WireGuard مباشرة من النص أو الحافظة |
| 🔍 **بحث في السيرفرات** | ابحث عن سيرفر بالاسم أو الموقع |
| 🛡️ **Kill Switch** | يقطع الإنترنت تلقائياً إذا انقطع VPN |
| 👥 **عداد الاستخدام** | يظهر كم مستخدم اتصل بكل سيرفر |
| 🌐 **DNS متعدد** | اختر: Cloudflare / Google / Quad9 / AdGuard |
| 🎨 **واجهة محسّنة** | تصميم جديد مع تحريكات وألوان أفضل |
| ⚠️ **رسائل خطأ واضحة** | تظهر سبب فشل الاتصال بوضوح |
| ⚙️ **MTU قابل للتخصيص** | لحل مشاكل الاتصال على بعض الشبكات |

---

## 🛠️ المتطلبات

- Android Studio Hedgehog (2023.1.1) أو أحدث
- JDK 17
- Android SDK 34
- حساب Firebase

---

## ⚡ خطوات الإعداد السريع

### الخطوة 1 — إنشاء مشروع Firebase

1. افتح [Firebase Console](https://console.firebase.google.com)
2. أنشئ مشروعاً جديداً (مثلاً `vpn-pro`)
3. اذهب إلى **Project Settings → General → Your apps**
4. اضغط **Add app → Android**
5. أدخل `com.vpnpro` كـ Package name
6. حمّل ملف `google-services.json`
7. **ضعه في مجلد `app/`** (استبدل الملف القالب الموجود)

### الخطوة 2 — إعداد Firebase Realtime Database

1. في Firebase Console → **Build → Realtime Database → Create database**
2. اختر منطقة قريبة → ابدأ بـ **Test mode**
3. اذهب إلى **Rules** والصق محتوى `firebase-rules.json` ثم اضغط **Publish**

### الخطوة 3 — بناء التطبيق

```bash
# افتح Android Studio → Open → اختر هذا المجلد
# انتظر Gradle sync (~2 دقائق)

# APK الناتج:
# app/build/outputs/apk/debug/app-debug.apk
```

---

## 🌐 إعداد سيرفر WireGuard (مطلوب لتجاوز القيود)

لكي يشتغل الاتصال الحقيقي ويمر **كل الترافيك** (صور + فيديو + أي موقع)، تحتاج سيرفر VPS:

```bash
# تثبيت WireGuard (Ubuntu/Debian)
sudo apt update && sudo apt install -y wireguard

# توليد مفاتيح السيرفر
wg genkey | tee /etc/wireguard/server_private.key | wg pubkey > /etc/wireguard/server_public.key

# إنشاء config السيرفر
sudo nano /etc/wireguard/wg0.conf
```

محتوى `/etc/wireguard/wg0.conf`:
```ini
[Interface]
Address = 10.0.0.1/24
ListenPort = 51820
PrivateKey = <SERVER_PRIVATE_KEY>
PostUp   = iptables -A FORWARD -i wg0 -j ACCEPT; iptables -t nat -A POSTROUTING -o eth0 -j MASQUERADE
PostDown = iptables -D FORWARD -i wg0 -j ACCEPT; iptables -t nat -D POSTROUTING -o eth0 -j MASQUERADE

[Peer]
PublicKey = <CLIENT_PUBLIC_KEY>
AllowedIPs = 10.0.0.2/32
```

```bash
# تفعيل IP Forwarding
echo "net.ipv4.ip_forward=1" | sudo tee -a /etc/sysctl.conf
sudo sysctl -p

# تشغيل WireGuard
sudo systemctl enable --now wg-quick@wg0
```

---

## 📱 إضافة سيرفر من داخل التطبيق

**طريقة 1 — يدوي (Servers → Add):**

| الحقل | القيمة |
|-------|--------|
| Server Name | اسم تعريفي مثل Germany-01 |
| Flag Emoji | 🇩🇪 |
| Location | Frankfurt |
| Endpoint | `YOUR_VPS_IP:51820` |
| Server Public Key | محتوى `server_public.key` |
| Client Private Key | محتوى `client.key` |
| Client Address | `10.0.0.2/32` |
| DNS | `1.1.1.1, 1.0.0.1` |
| Allowed IPs | `0.0.0.0/0, ::/0` ← **مهم: لتمرير كل الترافيك** |

**طريقة 2 — استيراد Config (Servers → Add → Import):**
- الصق محتوى ملف `.conf` مباشرة في حقل الاستيراد

---

## ⚙️ إعدادات مهمة

- **Allowed IPs = `0.0.0.0/0, ::/0`** → يمرّر كل الترافيك (صور + فيديو + كل موقع)
- **Kill Switch** → يقطع الإنترنت إذا انقطع VPN (يمنع تسريب IP)
- **DNS** → اختر Cloudflare (1.1.1.1) أو Quad9 (9.9.9.9) لأسرع DNS وأكثر أمناً

---

## 🏗️ هيكل المشروع

```
app/src/main/kotlin/com/vpnpro/
├── VpnProApp.kt
├── MainActivity.kt
├── data/
│   ├── model/Server.kt           ← نموذج السيرفر + import/export WireGuard config
│   └── firebase/FirebaseRepository.kt  ← CRUD كامل (add/update/delete/usage)
├── vpn/
│   └── VpnProService.kt          ← VPN Service (WireGuard GoBackend)
├── ui/
│   ├── theme/                    ← ألوان وـ Theme
│   ├── viewmodel/MainViewModel.kt ← منطق كامل مع delete/edit/import
│   ├── navigation/NavGraph.kt
│   └── screens/
│       ├── HomeScreen.kt         ← زر Connect مع animation + error display
│       ├── ServersScreen.kt      ← قائمة مع بحث + حذف + تعديل
│       ├── AddServerScreen.kt    ← إضافة + استيراد config
│       └── SettingsScreen.kt     ← Kill Switch + DNS + Auto-connect
└── utils/
    ├── BootReceiver.kt
    └── FormatUtils.kt
```

---

## 🔥 كيف يعمل تجاوز القيود؟

```
هاتفك → WireGuard VPN (تشفير ChaCha20) → سيرفرك → الإنترنت الكامل
```

بدل:
```
هاتفك → شبكة المشغل (مقيدة) → Facebook فقط (نص فقط)
```

بـ VPN:
```
هاتفك → WireGuard (مشفر) → سيرفر VPS → أي موقع + كل صور + كل فيديو
```

---

## ⚠️ ملاحظات

1. التطبيق يطلب **VPN Permission** عند أول اتصال — هذا سلوك طبيعي من Android
2. اختبر على جهاز حقيقي (VPN لا يشتغل على المحاكيات)
3. **كل مستخدم يحتاج Client Private Key خاص به** — للإنتاج الجاد أنشئ مفتاح لكل مستخدم
