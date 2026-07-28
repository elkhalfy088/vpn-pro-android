# VPN Pro — Android App

تطبيق VPN حقيقي يعمل بـ WireGuard + Firebase للمشاركة الفورية للسيرفرات بين جميع المستخدمين.

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
2. اختر منطقة قريبة → ابدأ بـ **Test mode** (أو انسخ قواعد الأمان من `firebase-rules.json`)
3. اذهب إلى **Rules** والصق محتوى `firebase-rules.json` ثم اضغط **Publish**

### الخطوة 3 — بناء التطبيق

```bash
# افتح Android Studio → Open → اختر هذا المجلد
# انتظر Gradle sync (~2 دقائق)

# أو من سطر الأوامر:
./gradlew assembleDebug

# APK الناتج:
# app/build/outputs/apk/debug/app-debug.apk
```

---

## 🌐 إعداد سيرفر WireGuard

لكي يشتغل الاتصال الحقيقي، تحتاج سيرفر VPS مع WireGuard:

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
# Client (generated from app)
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

## ➕ توليد مفاتيح العميل وإضافة سيرفر

```bash
# على السيرفر — توليد مفتاح للعميل
wg genkey | tee client.key | wg pubkey > client.pub

cat client.key  # ← هذا CLIENT_PRIVATE_KEY (يُدخل في التطبيق)
cat client.pub  # ← هذا CLIENT_PUBLIC_KEY (يُضاف لـ wg0.conf كـ [Peer])
cat /etc/wireguard/server_public.key  # ← SERVER_PUBLIC_KEY
```

أضف الـ [Peer] للسيرفر:
```ini
[Peer]
PublicKey = <CLIENT_PUBLIC_KEY>
AllowedIPs = 10.0.0.2/32
```

```bash
sudo wg syncconf wg0 <(sudo wg-quick strip wg0)
```

---

## 📱 إضافة سيرفر من داخل التطبيق

افتح التطبيق → **Servers → Add (+)**، أدخل:

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
| Allowed IPs | `0.0.0.0/0, ::/0` |

بعد الإضافة ← يظهر السيرفر **لجميع المستخدمين** فوراً.

---

## 🏗️ هيكل المشروع

```
app/src/main/kotlin/com/vpnpro/
├── VpnProApp.kt                  ← Application class (Hilt)
├── MainActivity.kt               ← نقطة الدخول
├── data/
│   ├── model/Server.kt           ← نموذج السيرفر + توليد WireGuard config
│   └── firebase/FirebaseRepository.kt  ← قراءة/كتابة Firebase
├── vpn/
│   └── VpnProService.kt          ← VPN Service الحقيقي (WireGuard GoBackend)
├── ui/
│   ├── theme/                    ← ألوان وـ Theme داكن
│   ├── viewmodel/MainViewModel.kt
│   ├── navigation/NavGraph.kt
│   └── screens/
│       ├── HomeScreen.kt         ← زر Connect الرئيسي
│       ├── ServersScreen.kt      ← قائمة السيرفرات
│       ├── AddServerScreen.kt    ← إضافة سيرفر جديد
│       └── SettingsScreen.kt
└── utils/
    ├── BootReceiver.kt
    └── FormatUtils.kt
```

---

## 🔥 كيف تشتغل المشاركة؟

```
مستخدم يضيف سيرفر
    ↓
Firebase Realtime Database
    ↓ (real-time)
جميع المستخدمين يرون السيرفر فوراً
    ↓
يختار مستخدم السيرفر → يضغط Start
    ↓
WireGuard GoBackend يفتح النفق الحقيقي
    ↓
كل الترافيك يمر عبر السيرفر (مشفر)
```

---

## ⚠️ ملاحظات

1. **كل مستخدم يستخدم نفس client private key** المخزن في Firebase للسيرفر المختار.
   للإنتاج الجاد: أنشئ مفتاح لكل مستخدم وسجّله كـ [Peer] منفصل على السيرفر.

2. **Firebase Rules**: في الإنتاج، قيّد الكتابة بـ Auth للحماية من الـ spam.

3. التطبيق يطلب **VPN Permission** عند أول اتصال — هذا سلوك إجباري من Android.

4. اختبر على جهاز حقيقي، ليس Emulator (VPN لا يشتغل على معظم المحاكيات).
