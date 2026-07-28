import React, { useState } from 'react';
import {
  View,
  Text,
  StyleSheet,
  TouchableOpacity,
  Platform,
} from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { MaterialCommunityIcons } from '@expo/vector-icons';
import { useColors } from '@/hooks/useColors';
import { AddServerSheet } from '@/components/AddServerSheet';

const QUICK_TEMPLATES = [
  {
    type: 'v2ray' as const,
    icon: 'shield' as const,
    label: 'V2Ray / VMess',
    desc: 'Paste vmess:// or vless:// link',
    color: '#00d4ff',
  },
  {
    type: 'trojan' as const,
    icon: 'sword' as const,
    label: 'Trojan',
    desc: 'Paste trojan:// link',
    color: '#a78bfa',
  },
  {
    type: 'shadowsocks' as const,
    icon: 'security' as const,
    label: 'Shadowsocks',
    desc: 'Paste ss:// link',
    color: '#34d399',
  },
  {
    type: 'hysteria2' as const,
    icon: 'lightning-bolt' as const,
    label: 'Hysteria2',
    desc: 'Paste hy2:// link',
    color: '#fb923c',
  },
  {
    type: 'proxy' as const,
    icon: 'swap-horizontal' as const,
    label: 'HTTP Proxy',
    desc: 'ip:port@user:pass format',
    color: '#60a5fa',
  },
  {
    type: 'socks5' as const,
    icon: 'lan' as const,
    label: 'SOCKS5',
    desc: 'socks5://user:pass@ip:port',
    color: '#f472b6',
  },
];

export default function AddScreen() {
  const colors = useColors();
  const insets = useSafeAreaInsets();
  const [showSheet, setShowSheet] = useState(false);

  const topPad = Platform.OS === 'web' ? 67 : insets.top;
  const bottomPad = Platform.OS === 'web' ? 84 : insets.bottom + 60;

  const s = styles(colors);

  return (
    <View style={[s.container, { paddingTop: topPad, paddingBottom: bottomPad }]}>
      <View style={s.header}>
        <Text style={s.title}>Add Server</Text>
        <Text style={s.subtitle}>Choose a protocol type to get started</Text>
      </View>

      <View style={s.grid}>
        {QUICK_TEMPLATES.map(t => (
          <TouchableOpacity
            key={t.type}
            style={[s.card, { borderColor: t.color + '40' }]}
            onPress={() => setShowSheet(true)}
            activeOpacity={0.7}
          >
            <View style={[s.iconBox, { backgroundColor: t.color + '18' }]}>
              <MaterialCommunityIcons name={t.icon as any} size={28} color={t.color} />
            </View>
            <Text style={s.cardLabel}>{t.label}</Text>
            <Text style={s.cardDesc}>{t.desc}</Text>
          </TouchableOpacity>
        ))}
      </View>

      <AddServerSheet visible={showSheet} onClose={() => setShowSheet(false)} />
    </View>
  );
}

const styles = (colors: ReturnType<typeof useColors>) =>
  StyleSheet.create({
    container: {
      flex: 1,
      backgroundColor: colors.background,
    },
    header: {
      paddingHorizontal: 20,
      paddingVertical: 20,
      borderBottomWidth: 1,
      borderBottomColor: colors.border,
    },
    title: {
      color: colors.foreground,
      fontSize: 24,
      fontFamily: 'Inter_700Bold',
    },
    subtitle: {
      color: colors.mutedForeground,
      fontSize: 14,
      fontFamily: 'Inter_400Regular',
      marginTop: 4,
    },
    grid: {
      flexDirection: 'row',
      flexWrap: 'wrap',
      padding: 12,
      gap: 10,
    },
    card: {
      width: '47%',
      backgroundColor: colors.card,
      borderRadius: 16,
      borderWidth: 1,
      padding: 16,
      gap: 8,
    },
    iconBox: {
      width: 50,
      height: 50,
      borderRadius: 14,
      alignItems: 'center',
      justifyContent: 'center',
    },
    cardLabel: {
      color: colors.foreground,
      fontSize: 14,
      fontFamily: 'Inter_600SemiBold',
    },
    cardDesc: {
      color: colors.mutedForeground,
      fontSize: 11,
      fontFamily: 'Inter_400Regular',
      lineHeight: 15,
    },
  });
