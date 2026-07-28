# Vpn Pro — دليل التثبيت الكامل

تطبيق VPN مع تمويه الترافيك — المزود يرى فقط اتصالاً بـ `www.facebook.com`

---

## 🏗️ بناء APK عبر GitHub Actions (أسهل طريقة)

1. ارفع المشروع على GitHub
2. اذهب إلى **Actions** → **Build APK** → **Run workflow**
3. بعد انتهاء البناء (5-10 دقائق) → **Artifacts** → حمّل `VpnPro-Debug.apk`

---

## 🖥️ إعداد سيرفر VPS (يجب تنفيذه مرة واحدة)

### الخطوة 1 — تثبيت Node.js على السيرفر

```bash
apt update && apt install -y nodejs npm
# أو على CentOS:
yum install -y nodejs npm
```

### الخطوة 2 — إنشاء WebSocket Proxy Server

أنشئ ملف `/opt/vpnpro/server.js`:

```javascript
const https = require('https');
const { WebSocketServer } = require('ws');
const net = require('net');
const fs = require('fs');
const tls = require('tls');

// إنشاء شهادة TLS ذاتية (self-signed)
// افعل ذلك مرة واحدة:
// openssl req -x509 -newkey rsa:4096 -keyout key.pem -out cert.pem -days 3650 -nodes -subj "/CN=vpnpro"

const server = https.createServer({
  key: fs.readFileSync('/opt/vpnpro/key.pem'),
  cert: fs.readFileSync('/opt/vpnpro/cert.pem'),
});

const wss = new WebSocketServer({ server, path: '/tunnel' });

wss.on('connection', (ws, req) => {
  console.log(`Client connected from ${req.socket.remoteAddress}`);

  // Simple TCP proxy: forward all data
  let targetSocket = null;

  ws.on('message', (data, isBinary) => {
    if (!isBinary) {
      // First message: JSON with target host/port
      try {
        const cmd = JSON.parse(data.toString());
        if (cmd.type === 'connect') {
          targetSocket = net.createConnection(cmd.port, cmd.host, () => {
            ws.send(JSON.stringify({ type: 'connected' }));
          });
          targetSocket.on('data', d => ws.send(d));
          targetSocket.on('close', () => ws.close());
          targetSocket.on('error', e => ws.close());
        }
      } catch (_) {}
    } else if (targetSocket) {
      targetSocket.write(data);
    }
  });

  ws.on('close', () => targetSocket?.destroy());
});

server.listen(8443, () => {
  console.log('VpnPro proxy running on port 8443');
});
```

### الخطوة 3 — توليد شهادة TLS

```bash
mkdir -p /opt/vpnpro && cd /opt/vpnpro
openssl req -x509 -newkey rsa:4096 \
  -keyout key.pem -out cert.pem \
  -days 3650 -nodes \
  -subj "/CN=vpnpro"
```

### الخطوة 4 — تثبيت المتطلبات وتشغيل السيرفر

```bash
cd /opt/vpnpro
npm install ws
node server.js &

# تشغيل تلقائي عند إعادة التشغيل:
npm install -g pm2
pm2 start server.js --name vpnpro
pm2 startup && pm2 save
```

### الخطوة 5 — فتح المنفذ في جدار الحماية

```bash
# Ubuntu/Debian:
ufw allow 8443/tcp

# CentOS/RHEL:
firewall-cmd --permanent --add-port=8443/tcp
firewall-cmd --reload
```

---

## 📱 إعداد التطبيق (أول تشغيل)

عند فتح التطبيق لأول مرة ستظهر شاشة الإعداد:

| الحقل | القيمة |
|-------|--------|
| IP السيرفر | IP الـ VPS الخاص بك |
| المنفذ | 8443 |
| دومين التمويه | www.facebook.com |

بعد الحفظ، تُخزَّن البيانات في Firebase — أي شخص يفتح التطبيق بعدك سيدخل مباشرة بدون إعداد.

---

## 🔥 Firebase Security Rules

في Firebase Console → Realtime Database → Rules:

```json
{
  "rules": {
    "vpn_config": {
      ".read": true,
      ".write": true
    }
  }
}
```

> ⚠️ للاستخدام الشخصي فقط. للإنتاج أضف مصادقة.

---

## 🛡️ كيف يعمل التمويه

```
هاتفك  →  TUN interface  →  WSS إلى VPS (SNI=facebook.com)  →  الإنترنت
```

المزود يرى:
- اتصال HTTPS/WSS على منفذ 8443
- SNI في TLS = `www.facebook.com`
- لا يرى محتوى الاتصال (مشفر بالكامل)

---

## 📁 هيكل المشروع

```
app/src/main/java/akh/vpn/dd/
├── MainActivity.kt          ← نقطة البداية + التوجيه
├── data/
│   ├── VpnConfig.kt         ← نموذج البيانات
│   └── FirebaseManager.kt   ← قراءة/كتابة Firebase
├── service/
│   ├── VpnProService.kt     ← Android VPN Service
│   └── BootReceiver.kt      ← تشغيل عند الإقلاع
├── tunnel/
│   └── SniTunnel.kt         ← تمويه SNI + WebSocket
├── ui/
│   ├── theme/Theme.kt       ← الألوان والتصميم
│   └── screens/
│       ├── SetupScreen.kt   ← شاشة الإعداد الأولى
│       └── HomeScreen.kt    ← الشاشة الرئيسية
└── viewmodel/
    └── VpnViewModel.kt      ← إدارة الحالة
```
