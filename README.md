# VPN Pro v3.0 — WireGuard + V2Ray/Xray 🇲🇦

تطبيق VPN متكامل يدعم **WireGuard** و**V2Ray/Xray** (VMess/VLESS/Trojan) مع جلب تلقائي للسيرفرات المجانية.

---

## 🆕 الجديد في v3.0

| الميزة | الوصف |
|--------|-------|
| 🆓 **سيرفرات مجانية** | جلب تلقائي من barry-far، mahdibland، freefq |
| 🔗 **V2Ray/Xray** | دعم VMess / VLESS / Trojan كامل |
| 🇲🇦 **Bug Hosts مغربية** | قائمة Bug Hosts لـ Inwi، IAM، Orange |
| 📋 **لصق رابط** | الصق vmess:// أو vless:// أو trojan:// مباشرة |
| 📡 **WireGuard محافظ** | كل ميزات WireGuard v2.0 لا تزال موجودة |

---

## 📱 الاستخدام للإنترنت المجاني (بدون دفع — 0 درهم)

### الطريقة الأسرع:

1. ثبّت التطبيق
2. افتح **"سيرفرات مجانية"** (الزر الأخضر في الصفحة الرئيسية)
3. اضغط 🔄 Refresh لجلب أحدث السيرفرات
4. اضغط **Connect** على أي سيرفر
5. اقبل إذن VPN
6. إذا لم يشتغل السيرفر الأول، جرّب سيرفراً آخر

> **ملاحظة:** السيرفرات المجانية تتغير يومياً. اضغط Refresh لتحديثها.

---

## 🛠️ البناء (GitHub Actions — تلقائي)

عند كل push على `main`، يُبنى APK تلقائياً ويُنشر في Releases.

يمكنك تشغيل البناء يدوياً من: **Actions → Build APK → Run workflow**

---

## 🇲🇦 Bug Hosts للشرائح المغربية

| الشريحة | Bug Host | Port | TLS |
|---------|----------|------|-----|
| Inwi | graph.facebook.com | 443 | ✅ |
| Inwi | web.facebook.com | 80 | ❌ |
| Maroc Telecom (IAM) | graph.facebook.com | 443 | ✅ |
| Maroc Telecom (IAM) | free.facebook.com | 443 | ✅ |
| Orange Maroc | graph.facebook.com | 443 | ✅ |
| Orange Maroc | web.facebook.com | 443 | ✅ |
| الكل | zero.facebook.com | 443 | ✅ |
| الكل | internet.org | 443 | ✅ |

> **كيف تستخدم Bug Host:** تحتاج سيرفر VPS خاص مع V2Ray مُعَدّ بـ WebSocket. ضع Host Header = Bug Host.

---

## 🔗 مصادر السيرفرات المجانية

| المصدر | الرابط |
|--------|--------|
| barry-far | https://github.com/barry-far/V2ray-Configs |
| mahdibland | https://github.com/mahdibland/V2RayAggregator |
| freefq | https://freefq.com/v2ray/ |
| yebekhe | https://github.com/yebekhe/TelegramV2rayCollector |
| تيليجرام | @v2rayng_config |

---

## 🏗️ هيكل المشروع

```
app/src/main/kotlin/com/vpnpro/
├── data/
│   ├── model/Server.kt           ← نموذج يدعم WireGuard + V2Ray
│   └── firebase/FirebaseRepository.kt
├── vpn/
│   ├── VpnProService.kt          ← خدمة WireGuard
│   ├── XrayVpnService.kt         ← خدمة V2Ray/Xray ← جديد
│   ├── XrayController.kt         ← تحكم Xray core ← جديد
│   ├── ConfigParser.kt           ← تحليل VMess/VLESS/Trojan ← جديد
│   └── FreeConfigFetcher.kt      ← جلب سيرفرات مجانية ← جديد
├── ui/
│   ├── viewmodel/MainViewModel.kt ← يدعم كلا البروتوكولين
│   └── screens/
│       ├── HomeScreen.kt
│       ├── ServersScreen.kt
│       ├── AddServerScreen.kt    ← تبويب WireGuard + تبويب V2Ray
│       ├── FreeServersScreen.kt  ← سيرفرات مجانية + Bug Hosts ← جديد
│       └── SettingsScreen.kt
└── utils/
```

---

## ⚙️ المتطلبات للبناء المحلي

- Android Studio Hedgehog أو أحدث
- JDK 17
- Android SDK 36
- حساب Firebase (اختياري — للمشاركة المركزية للسيرفرات)
