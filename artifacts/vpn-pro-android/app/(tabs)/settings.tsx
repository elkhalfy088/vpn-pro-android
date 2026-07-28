import React from 'react';
import {
  View,
  Text,
  StyleSheet,
  TouchableOpacity,
  Platform,
  Linking,
  Alert,
} from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { Feather, MaterialCommunityIcons } from '@expo/vector-icons';
import { useColors } from '@/hooks/useColors';
import { useServers } from '@/context/ServersContext';

interface SettingRowProps {
  icon: React.ReactNode;
  label: string;
  value?: string;
  onPress?: () => void;
  dangerous?: boolean;
  chevron?: boolean;
}

function SettingRow({ icon, label, value, onPress, dangerous, chevron = true }: SettingRowProps) {
  const colors = useColors();
  const s = rowStyles(colors);
  return (
    <TouchableOpacity
      style={s.row}
      onPress={onPress}
      activeOpacity={onPress ? 0.6 : 1}
      disabled={!onPress}
    >
      <View style={s.iconWrap}>{icon}</View>
      <Text style={[s.label, dangerous && s.dangerLabel]}>{label}</Text>
      <View style={s.right}>
        {value && <Text style={s.value}>{value}</Text>}
        {chevron && onPress && <Feather name="chevron-right" size={16} color={colors.mutedForeground} />}
      </View>
    </TouchableOpacity>
  );
}

const rowStyles = (colors: ReturnType<typeof useColors>) =>
  StyleSheet.create({
    row: {
      flexDirection: 'row',
      alignItems: 'center',
      paddingVertical: 14,
      paddingHorizontal: 16,
      gap: 12,
    },
    iconWrap: {
      width: 32,
      height: 32,
      borderRadius: 8,
      backgroundColor: colors.secondary,
      alignItems: 'center',
      justifyContent: 'center',
    },
    label: {
      flex: 1,
      color: colors.foreground,
      fontSize: 15,
      fontFamily: 'Inter_400Regular',
    },
    dangerLabel: { color: '#ef4444' },
    right: {
      flexDirection: 'row',
      alignItems: 'center',
      gap: 6,
    },
    value: {
      color: colors.mutedForeground,
      fontSize: 14,
      fontFamily: 'Inter_400Regular',
    },
  });

export default function SettingsScreen() {
  const colors = useColors();
  const insets = useSafeAreaInsets();
  const { servers, disconnect, connectionState } = useServers();

  const topPad = Platform.OS === 'web' ? 67 : insets.top;
  const bottomPad = Platform.OS === 'web' ? 84 : insets.bottom + 60;

  const s = styles(colors);

  const handleGithub = () => {
    Linking.openURL('https://github.com/vpn-pro-android/vpn-pro-android/releases');
  };

  const handleDisconnectAll = () => {
    if (connectionState !== 'connected') return;
    Alert.alert('Disconnect', 'Disconnect from current VPN?', [
      { text: 'Cancel', style: 'cancel' },
      { text: 'Disconnect', style: 'destructive', onPress: disconnect },
    ]);
  };

  return (
    <View style={[s.container, { paddingTop: topPad, paddingBottom: bottomPad }]}>
      <View style={s.header}>
        <Text style={s.title}>Settings</Text>
      </View>

      {/* Stats */}
      <View style={s.statsRow}>
        <View style={s.statCard}>
          <Text style={s.statValue}>{servers.length}</Text>
          <Text style={s.statLabel}>Servers</Text>
        </View>
        <View style={s.statCard}>
          <Text style={[s.statValue, { color: connectionState === 'connected' ? colors.connected : colors.mutedForeground }]}>
            {connectionState === 'connected' ? 'ON' : 'OFF'}
          </Text>
          <Text style={s.statLabel}>Status</Text>
        </View>
        <View style={s.statCard}>
          <Text style={s.statValue}>{servers.filter(s => s.ping != null && s.ping < 100).length}</Text>
          <Text style={s.statLabel}>Fast</Text>
        </View>
      </View>

      {/* Connection */}
      <View style={s.section}>
        <Text style={s.sectionTitle}>CONNECTION</Text>
        <View style={s.card}>
          <SettingRow
            icon={<MaterialCommunityIcons name="shield-outline" size={18} color={colors.primary} />}
            label="Protocol"
            value="Auto"
            onPress={() => {}}
          />
          <View style={s.divider} />
          <SettingRow
            icon={<Feather name="wifi-off" size={18} color="#ef4444" />}
            label="Disconnect"
            dangerous
            onPress={handleDisconnectAll}
            chevron={false}
          />
        </View>
      </View>

      {/* App */}
      <View style={s.section}>
        <Text style={s.sectionTitle}>APP</Text>
        <View style={s.card}>
          <SettingRow
            icon={<MaterialCommunityIcons name="github" size={18} color={colors.foreground} />}
            label="Download APK"
            value="GitHub"
            onPress={handleGithub}
          />
          <View style={s.divider} />
          <SettingRow
            icon={<Feather name="info" size={18} color={colors.primary} />}
            label="Version"
            value="1.0.0"
            chevron={false}
          />
        </View>
      </View>

      {/* Footer */}
      <View style={s.footer}>
        <View style={s.shieldBadge}>
          <MaterialCommunityIcons name="shield-check" size={16} color={colors.primary} />
          <Text style={s.footerText}>VPN Pro · Secure & Private</Text>
        </View>
      </View>
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
    statsRow: {
      flexDirection: 'row',
      gap: 10,
      padding: 16,
    },
    statCard: {
      flex: 1,
      backgroundColor: colors.card,
      borderRadius: 14,
      borderWidth: 1,
      borderColor: colors.border,
      alignItems: 'center',
      paddingVertical: 16,
    },
    statValue: {
      color: colors.primary,
      fontSize: 24,
      fontFamily: 'Inter_700Bold',
    },
    statLabel: {
      color: colors.mutedForeground,
      fontSize: 11,
      fontFamily: 'Inter_500Medium',
      marginTop: 4,
      textTransform: 'uppercase',
      letterSpacing: 0.5,
    },
    section: {
      paddingHorizontal: 16,
      marginBottom: 16,
    },
    sectionTitle: {
      color: colors.mutedForeground,
      fontSize: 11,
      fontFamily: 'Inter_600SemiBold',
      letterSpacing: 1,
      textTransform: 'uppercase',
      marginBottom: 8,
      paddingHorizontal: 4,
    },
    card: {
      backgroundColor: colors.card,
      borderRadius: 14,
      borderWidth: 1,
      borderColor: colors.border,
      overflow: 'hidden',
    },
    divider: {
      height: 1,
      backgroundColor: colors.border,
      marginLeft: 60,
    },
    footer: {
      flex: 1,
      alignItems: 'center',
      justifyContent: 'flex-end',
      paddingBottom: 20,
    },
    shieldBadge: {
      flexDirection: 'row',
      alignItems: 'center',
      gap: 6,
    },
    footerText: {
      color: colors.mutedForeground,
      fontSize: 12,
      fontFamily: 'Inter_400Regular',
    },
  });
