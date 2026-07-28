import React from 'react';
import {
  View,
  Text,
  TouchableOpacity,
  StyleSheet,
  ActivityIndicator,
  Platform,
} from 'react-native';
import { Feather, MaterialCommunityIcons } from '@expo/vector-icons';
import Animated, {
  useAnimatedStyle,
  useSharedValue,
  withSpring,
  withRepeat,
  withTiming,
  Easing,
} from 'react-native-reanimated';
import { useColors } from '@/hooks/useColors';
import type { Server } from '@workspace/api-client-react';
import { useVpn } from '@/contexts/VpnContext';
import { router } from 'expo-router';

interface ServerCardProps {
  server: Server;
  onDelete: (id: number) => void;
}

const TYPE_INFO: Record<string, { label: string; color: string; icon: string }> = {
  v2ray:       { label: 'V2Ray',       color: '#00C9FF', icon: 'server-network' },
  vmess:       { label: 'VMess',       color: '#7B5CF0', icon: 'server-network' },
  vless:       { label: 'VLess',       color: '#9F7AEA', icon: 'server-network' },
  trojan:      { label: 'Trojan',      color: '#F6AD55', icon: 'shield-lock' },
  shadowsocks: { label: 'SS',          color: '#68D391', icon: 'server-security' },
  proxy:       { label: 'Proxy',       color: '#FC8181', icon: 'transit-connection-variant' },
};

function PingDot({ ping }: { ping: number | null | undefined }) {
  const colors = useColors();
  if (!ping) return <View style={[styles.pingDot, { backgroundColor: colors.mutedForeground }]} />;
  const color = ping < 100 ? colors.success : ping < 200 ? colors.warning : colors.danger;
  return <View style={[styles.pingDot, { backgroundColor: color }]} />;
}

export function ServerCard({ server, onDelete }: ServerCardProps) {
  const colors = useColors();
  const { connect, connectingId } = useVpn();
  const isConnecting = connectingId === server.id;
  const isActive = server.isActive;
  const typeInfo = TYPE_INFO[server.type] ?? TYPE_INFO.proxy;

  const scale = useSharedValue(1);
  const pulseOpacity = useSharedValue(isActive ? 0.8 : 0);

  React.useEffect(() => {
    if (isActive) {
      pulseOpacity.value = withRepeat(
        withTiming(0.2, { duration: 1200, easing: Easing.inOut(Easing.sin) }),
        -1,
        true
      );
    } else {
      pulseOpacity.value = withTiming(0, { duration: 300 });
    }
  }, [isActive]);

  const pulseStyle = useAnimatedStyle(() => ({
    opacity: pulseOpacity.value,
  }));

  const cardStyle = useAnimatedStyle(() => ({
    transform: [{ scale: scale.value }],
  }));

  const handlePressIn = () => {
    scale.value = withSpring(0.98, { damping: 15, stiffness: 300 });
  };
  const handlePressOut = () => {
    scale.value = withSpring(1, { damping: 15, stiffness: 300 });
  };

  const handleConnect = () => {
    if (!isConnecting) connect(server);
  };

  const displayHost = server.host
    ? `${server.host}${server.port ? `:${server.port}` : ''}`
    : server.config
    ? server.config.length > 40 ? server.config.substring(0, 40) + '…' : server.config
    : 'No config';

  return (
    <Animated.View style={[cardStyle, { marginHorizontal: 16, marginBottom: 12 }]}>
      {/* Glow pulse when active */}
      {isActive && (
        <Animated.View
          style={[
            StyleSheet.absoluteFill,
            {
              borderRadius: colors.radius ?? 14,
              backgroundColor: colors.successGlow,
              borderWidth: 1,
              borderColor: colors.success,
            },
            pulseStyle,
          ]}
        />
      )}

      <View
        style={[
          styles.card,
          {
            backgroundColor: colors.card,
            borderColor: isActive ? colors.success : colors.border,
            borderRadius: 14,
          },
        ]}
      >
        {/* Active badge */}
        {isActive && (
          <View style={[styles.activeBadge, { backgroundColor: colors.success }]}>
            <Text style={[styles.activeBadgeText, { color: colors.successForeground }]}>
              ACTIVE
            </Text>
          </View>
        )}

        <View style={styles.cardContent}>
          {/* Left: flag + info */}
          <View style={styles.leftSection}>
            <View style={styles.flagRow}>
              <Text style={styles.flag}>{server.flag ?? '🌐'}</Text>
              <View
                style={[
                  styles.typeBadge,
                  { backgroundColor: typeInfo.color + '22', borderColor: typeInfo.color + '55' },
                ]}
              >
                <MaterialCommunityIcons
                  name={typeInfo.icon as any}
                  size={10}
                  color={typeInfo.color}
                />
                <Text style={[styles.typeLabel, { color: typeInfo.color }]}>
                  {typeInfo.label}
                </Text>
              </View>
            </View>

            <Text
              style={[styles.serverName, { color: colors.foreground }]}
              numberOfLines={1}
            >
              {server.name}
            </Text>

            <View style={styles.hostRow}>
              <PingDot ping={server.ping} />
              <Text
                style={[styles.hostText, { color: colors.mutedForeground }]}
                numberOfLines={1}
              >
                {displayHost}
              </Text>
            </View>
          </View>

          {/* Right: action buttons */}
          <View style={styles.rightSection}>
            {/* Edit button */}
            <TouchableOpacity
              style={[styles.iconBtn, { backgroundColor: colors.surface }]}
              onPress={() => router.push({ pathname: '/edit-server/[id]', params: { id: server.id.toString() } })}
              hitSlop={{ top: 8, bottom: 8, left: 8, right: 8 }}
            >
              <Feather name="edit-2" size={14} color={colors.mutedForeground} />
            </TouchableOpacity>

            {/* Delete button */}
            <TouchableOpacity
              style={[styles.iconBtn, { backgroundColor: colors.surface }]}
              onPress={() => onDelete(server.id)}
              hitSlop={{ top: 8, bottom: 8, left: 8, right: 8 }}
            >
              <Feather name="trash-2" size={14} color={colors.danger} />
            </TouchableOpacity>

            {/* Connect button */}
            <TouchableOpacity
              style={[
                styles.connectBtn,
                {
                  backgroundColor: isActive
                    ? colors.successGlow
                    : colors.primaryGlow,
                  borderColor: isActive ? colors.success : colors.primary,
                },
              ]}
              onPress={handleConnect}
              onPressIn={handlePressIn}
              onPressOut={handlePressOut}
              disabled={isConnecting}
              activeOpacity={0.8}
            >
              {isConnecting ? (
                <ActivityIndicator size="small" color={colors.primary} />
              ) : (
                <Feather
                  name={isActive ? 'wifi' : 'play'}
                  size={16}
                  color={isActive ? colors.success : colors.primary}
                />
              )}
            </TouchableOpacity>
          </View>
        </View>
      </View>
    </Animated.View>
  );
}

const styles = StyleSheet.create({
  card: {
    borderWidth: 1,
    overflow: 'hidden',
  },
  cardContent: {
    flexDirection: 'row',
    alignItems: 'center',
    padding: 16,
    gap: 12,
  },
  leftSection: {
    flex: 1,
    gap: 6,
  },
  rightSection: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
  },
  flagRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
  },
  flag: {
    fontSize: 22,
  },
  typeBadge: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 4,
    paddingHorizontal: 7,
    paddingVertical: 3,
    borderRadius: 6,
    borderWidth: 1,
  },
  typeLabel: {
    fontSize: 10,
    fontFamily: 'Inter_600SemiBold',
    letterSpacing: 0.5,
  },
  serverName: {
    fontSize: 15,
    fontFamily: 'Inter_600SemiBold',
  },
  hostRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
  },
  pingDot: {
    width: 7,
    height: 7,
    borderRadius: 4,
  },
  hostText: {
    fontSize: 12,
    fontFamily: 'Inter_400Regular',
    flex: 1,
  },
  iconBtn: {
    width: 32,
    height: 32,
    borderRadius: 8,
    justifyContent: 'center',
    alignItems: 'center',
  },
  connectBtn: {
    width: 40,
    height: 40,
    borderRadius: 10,
    borderWidth: 1.5,
    justifyContent: 'center',
    alignItems: 'center',
  },
  activeBadge: {
    position: 'absolute',
    top: 0,
    right: 0,
    paddingHorizontal: 8,
    paddingVertical: 3,
    borderBottomLeftRadius: 10,
    borderTopRightRadius: 14,
  },
  activeBadgeText: {
    fontSize: 9,
    fontFamily: 'Inter_700Bold',
    letterSpacing: 1,
  },
});
