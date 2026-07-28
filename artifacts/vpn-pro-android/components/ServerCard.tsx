import React from 'react';
import {
  View,
  Text,
  TouchableOpacity,
  StyleSheet,
  Platform,
} from 'react-native';
import { Feather, MaterialCommunityIcons } from '@expo/vector-icons';
import * as Haptics from 'expo-haptics';
import { useColors } from '@/hooks/useColors';
import type { VpnServer } from '@workspace/api-client-react';

const SERVER_TYPE_ICONS: Record<string, { icon: string; lib: 'feather' | 'mci' }> = {
  v2ray: { icon: 'shield', lib: 'mci' },
  vless: { icon: 'shield-half-full', lib: 'mci' },
  proxy: { icon: 'swap-horizontal', lib: 'mci' },
  socks5: { icon: 'lan', lib: 'mci' },
  shadowsocks: { icon: 'security', lib: 'mci' },
  trojan: { icon: 'sword', lib: 'mci' },
  hysteria2: { icon: 'lightning-bolt', lib: 'mci' },
  custom: { icon: 'server', lib: 'feather' },
};

interface ServerCardProps {
  server: VpnServer;
  isActive: boolean;
  isConnected: boolean;
  onPress: (server: VpnServer) => void;
  onEdit: (server: VpnServer) => void;
  onDelete: (id: string) => void;
}

export function ServerCard({
  server,
  isActive,
  isConnected,
  onPress,
  onEdit,
  onDelete,
}: ServerCardProps) {
  const colors = useColors();

  const handlePress = () => {
    if (Platform.OS !== 'web') Haptics.impactAsync(Haptics.ImpactFeedbackStyle.Light);
    onPress(server);
  };

  const renderIcon = () => {
    const iconInfo = SERVER_TYPE_ICONS[server.type] ?? SERVER_TYPE_ICONS.custom!;
    if (iconInfo.lib === 'mci') {
      return (
        <MaterialCommunityIcons
          name={iconInfo.icon as any}
          size={20}
          color={isActive && isConnected ? colors.connected : colors.primary}
        />
      );
    }
    return (
      <Feather
        name={iconInfo.icon as any}
        size={20}
        color={isActive && isConnected ? colors.connected : colors.primary}
      />
    );
  };

  const s = styles(colors);

  return (
    <TouchableOpacity
      onPress={handlePress}
      activeOpacity={0.75}
      style={[s.card, isActive && s.activeCard]}
    >
      {isActive && isConnected && <View style={s.activePulse} />}
      <View style={s.left}>
        <View style={[s.iconBox, isActive && isConnected && s.iconBoxActive]}>
          {renderIcon()}
        </View>
        <View style={s.info}>
          <View style={s.nameRow}>
            <Text style={s.name} numberOfLines={1}>{server.name}</Text>
            {isActive && isConnected && (
              <View style={s.connectedBadge}>
                <Text style={s.connectedBadgeText}>ACTIVE</Text>
              </View>
            )}
          </View>
          <View style={s.metaRow}>
            <Text style={s.flag}>{server.flag}</Text>
            <Text style={s.country}>{server.country}</Text>
            <Text style={s.dot}>·</Text>
            <Text style={s.type}>{server.type.toUpperCase()}</Text>
            {server.ping != null && (
              <>
                <Text style={s.dot}>·</Text>
                <Text style={[s.ping, server.ping < 100 ? s.pingGood : server.ping < 200 ? s.pingOk : s.pingBad]}>
                  {Math.round(server.ping)}ms
                </Text>
              </>
            )}
          </View>
        </View>
      </View>
      <View style={s.actions}>
        <TouchableOpacity
          onPress={() => onEdit(server)}
          hitSlop={{ top: 8, bottom: 8, left: 8, right: 8 }}
          style={s.actionBtn}
        >
          <Feather name="edit-2" size={15} color={colors.mutedForeground} />
        </TouchableOpacity>
        <TouchableOpacity
          onPress={() => onDelete(server.id)}
          hitSlop={{ top: 8, bottom: 8, left: 8, right: 8 }}
          style={s.actionBtn}
        >
          <Feather name="trash-2" size={15} color={colors.destructive} />
        </TouchableOpacity>
      </View>
    </TouchableOpacity>
  );
}

const styles = (colors: ReturnType<typeof useColors>) =>
  StyleSheet.create({
    card: {
      backgroundColor: colors.serverCard,
      borderRadius: 14,
      borderWidth: 1,
      borderColor: colors.border,
      flexDirection: 'row',
      alignItems: 'center',
      justifyContent: 'space-between',
      paddingHorizontal: 16,
      paddingVertical: 14,
      marginBottom: 10,
      overflow: 'hidden',
    },
    activeCard: {
      borderColor: colors.activeBorder,
      backgroundColor: colors.activeServer,
    },
    activePulse: {
      position: 'absolute',
      top: 0,
      left: 0,
      right: 0,
      bottom: 0,
      borderRadius: 14,
      borderWidth: 1,
      borderColor: colors.connected,
      opacity: 0.3,
    },
    left: {
      flexDirection: 'row',
      alignItems: 'center',
      flex: 1,
    },
    iconBox: {
      width: 40,
      height: 40,
      borderRadius: 10,
      backgroundColor: colors.glowCyan,
      alignItems: 'center',
      justifyContent: 'center',
      marginRight: 12,
    },
    iconBoxActive: {
      backgroundColor: colors.glowGreen,
    },
    info: {
      flex: 1,
    },
    nameRow: {
      flexDirection: 'row',
      alignItems: 'center',
      gap: 8,
    },
    name: {
      color: colors.foreground,
      fontSize: 15,
      fontFamily: 'Inter_600SemiBold',
      flex: 1,
    },
    connectedBadge: {
      backgroundColor: colors.connected + '33',
      paddingHorizontal: 7,
      paddingVertical: 2,
      borderRadius: 6,
    },
    connectedBadgeText: {
      color: colors.connected,
      fontSize: 10,
      fontFamily: 'Inter_700Bold',
      letterSpacing: 0.5,
    },
    metaRow: {
      flexDirection: 'row',
      alignItems: 'center',
      marginTop: 4,
      gap: 5,
    },
    flag: {
      fontSize: 13,
    },
    country: {
      color: colors.mutedForeground,
      fontSize: 12,
      fontFamily: 'Inter_400Regular',
    },
    dot: {
      color: colors.mutedForeground,
      fontSize: 12,
    },
    type: {
      color: colors.primary,
      fontSize: 11,
      fontFamily: 'Inter_600SemiBold',
      letterSpacing: 0.5,
    },
    ping: {
      fontSize: 11,
      fontFamily: 'Inter_500Medium',
    },
    pingGood: { color: '#22c55e' },
    pingOk: { color: '#f59e0b' },
    pingBad: { color: '#ef4444' },
    actions: {
      flexDirection: 'row',
      alignItems: 'center',
      gap: 12,
      marginLeft: 8,
    },
    actionBtn: {
      padding: 4,
    },
  });
