import React, { useState, useCallback } from 'react';
import {
  View,
  Text,
  StyleSheet,
  FlatList,
  TouchableOpacity,
  Alert,
  RefreshControl,
  Platform,
  ActivityIndicator,
} from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { Feather } from '@expo/vector-icons';
import Animated, {
  FadeInDown,
  useAnimatedStyle,
  useSharedValue,
  withRepeat,
  withTiming,
  Easing,
} from 'react-native-reanimated';
import { useColors } from '@/hooks/useColors';
import { useGetServers, useDeleteServer, useGetActiveServer, getGetServersQueryKey, getGetActiveServerQueryKey } from '@workspace/api-client-react';
import { useQueryClient } from '@tanstack/react-query';
import type { Server } from '@workspace/api-client-react';
import { ServerCard } from '@/components/ServerCard';
import { useVpn } from '@/contexts/VpnContext';
import { router } from 'expo-router';
import * as Haptics from 'expo-haptics';
import { LinearGradient } from 'expo-linear-gradient';

function ActiveStatusCard({ activeServer }: { activeServer: Server | null | undefined }) {
  const colors = useColors();
  const { disconnect } = useVpn();
  const isConnected = !!activeServer;

  const glowAnim = useSharedValue(0);

  React.useEffect(() => {
    if (isConnected) {
      glowAnim.value = withRepeat(
        withTiming(1, { duration: 2000, easing: Easing.inOut(Easing.sin) }),
        -1,
        true
      );
    } else {
      glowAnim.value = withTiming(0, { duration: 500 });
    }
  }, [isConnected]);

  const glowStyle = useAnimatedStyle(() => ({
    opacity: 0.4 + glowAnim.value * 0.3,
  }));

  return (
    <View style={[styles.statusCard, { backgroundColor: colors.card, borderColor: isConnected ? colors.success : colors.border }]}>
      {isConnected && (
        <Animated.View
          style={[StyleSheet.absoluteFill, { borderRadius: 18, backgroundColor: colors.successGlow }, glowStyle]}
        />
      )}
      <LinearGradient
        colors={isConnected ? ['#00E67610', '#00E67605', 'transparent'] : ['#1A203510', '#0F142010', 'transparent']}
        start={{ x: 0, y: 0 }}
        end={{ x: 1, y: 1 }}
        style={[StyleSheet.absoluteFill, { borderRadius: 18 }]}
      />
      <View style={styles.statusCardContent}>
        <View style={[styles.statusIconWrap, { backgroundColor: isConnected ? colors.successGlow : colors.muted }]}>
          <Feather name={isConnected ? 'shield' : 'shield-off'} size={28} color={isConnected ? colors.success : colors.mutedForeground} />
        </View>
        <View style={styles.statusInfo}>
          <View style={styles.statusRow}>
            <View style={[styles.statusDot, { backgroundColor: isConnected ? colors.success : colors.mutedForeground }]} />
            <Text style={[styles.statusLabel, { color: isConnected ? colors.success : colors.mutedForeground }]}>
              {isConnected ? 'CONNECTED' : 'NOT CONNECTED'}
            </Text>
          </View>
          <Text style={[styles.statusServer, { color: colors.foreground }]} numberOfLines={1}>
            {isConnected ? activeServer!.name : 'Select a server to connect'}
          </Text>
          {isConnected && activeServer!.host && (
            <Text style={[styles.statusHost, { color: colors.mutedForeground }]} numberOfLines={1}>
              {activeServer!.flag} {activeServer!.country ?? activeServer!.host}
            </Text>
          )}
        </View>
      </View>
    </View>
  );
}

export default function ServersScreen() {
  const colors = useColors();
  const insets = useSafeAreaInsets();
  const queryClient = useQueryClient();

  const { data: servers, isLoading, isError, refetch } = useGetServers();
  const { data: activeData } = useGetActiveServer();
  const deleteMutation = useDeleteServer();
  const [refreshing, setRefreshing] = useState(false);

  const activeServer = activeData?.server ?? null;

  const onRefresh = useCallback(async () => {
    setRefreshing(true);
    await refetch();
    setRefreshing(false);
  }, [refetch]);

  const handleDelete = useCallback((id: number) => {
    Alert.alert(
      'Delete Server',
      'Remove this server from the list?',
      [
        { text: 'Cancel', style: 'cancel' },
        {
          text: 'Delete',
          style: 'destructive',
          onPress: async () => {
            await Haptics.impactAsync(Haptics.ImpactFeedbackStyle.Medium);
            await deleteMutation.mutateAsync({ id });
            queryClient.invalidateQueries({ queryKey: getGetServersQueryKey() });
            queryClient.invalidateQueries({ queryKey: getGetActiveServerQueryKey() });
          },
        },
      ]
    );
  }, [deleteMutation, queryClient]);

  const renderServer = useCallback(
    ({ item, index }: { item: Server; index: number }) => (
      <Animated.View entering={FadeInDown.delay(index * 60).springify()}>
        <ServerCard server={item} onDelete={handleDelete} />
      </Animated.View>
    ),
    [handleDelete]
  );

  const topPad = Platform.OS === 'web' ? 67 : insets.top;
  const bottomPad = Platform.OS === 'web' ? 34 : insets.bottom;

  return (
    <View style={[styles.container, { backgroundColor: colors.background }]}>
      {/* Header */}
      <View style={[styles.header, { paddingTop: topPad + 12 }]}>
        <View style={styles.headerLeft}>
          <View style={[styles.logoWrap, { backgroundColor: colors.primaryGlow, borderColor: colors.primary + '44' }]}>
            <Feather name="shield" size={20} color={colors.primary} />
          </View>
          <View>
            <Text style={[styles.appName, { color: colors.foreground }]}>VPN Pro</Text>
            <Text style={[styles.serverCount, { color: colors.mutedForeground }]}>
              {servers ? `${servers.length} server${servers.length !== 1 ? 's' : ''}` : '—'}
            </Text>
          </View>
        </View>
        <TouchableOpacity
          style={[styles.addBtn, { backgroundColor: colors.primary }]}
          onPress={() => router.push('/add-server')}
          activeOpacity={0.8}
        >
          <Feather name="plus" size={20} color={colors.primaryForeground} />
        </TouchableOpacity>
      </View>

      {/* Active status */}
      <View style={{ paddingHorizontal: 16, paddingBottom: 8 }}>
        <ActiveStatusCard activeServer={activeServer} />
      </View>

      {/* Servers list */}
      {isLoading && !servers ? (
        <View style={styles.centerState}>
          <ActivityIndicator size="large" color={colors.primary} />
          <Text style={[styles.stateText, { color: colors.mutedForeground }]}>Loading servers…</Text>
        </View>
      ) : isError ? (
        <View style={styles.centerState}>
          <Feather name="wifi-off" size={48} color={colors.danger} />
          <Text style={[styles.stateTitle, { color: colors.foreground }]}>Connection Error</Text>
          <Text style={[styles.stateText, { color: colors.mutedForeground }]}>Could not reach server</Text>
          <TouchableOpacity
            style={[styles.retryBtn, { backgroundColor: colors.primary }]}
            onPress={() => refetch()}
          >
            <Text style={[styles.retryText, { color: colors.primaryForeground }]}>Retry</Text>
          </TouchableOpacity>
        </View>
      ) : !servers || servers.length === 0 ? (
        <View style={styles.centerState}>
          <View style={[styles.emptyIcon, { backgroundColor: colors.muted }]}>
            <Feather name="server" size={36} color={colors.mutedForeground} />
          </View>
          <Text style={[styles.stateTitle, { color: colors.foreground }]}>No Servers Yet</Text>
          <Text style={[styles.stateText, { color: colors.mutedForeground }]}>
            Tap the + button to add your first VPN server
          </Text>
          <TouchableOpacity
            style={[styles.addFirstBtn, { backgroundColor: colors.primary }]}
            onPress={() => router.push('/add-server')}
            activeOpacity={0.85}
          >
            <Feather name="plus" size={16} color={colors.primaryForeground} />
            <Text style={[styles.addFirstText, { color: colors.primaryForeground }]}>Add Server</Text>
          </TouchableOpacity>
        </View>
      ) : (
        <FlatList
          data={servers}
          renderItem={renderServer}
          keyExtractor={(item) => item.id.toString()}
          contentContainerStyle={[styles.list, { paddingBottom: bottomPad + 16 }]}
          showsVerticalScrollIndicator={false}
          refreshControl={
            <RefreshControl
              refreshing={refreshing}
              onRefresh={onRefresh}
              tintColor={colors.primary}
              colors={[colors.primary]}
            />
          }
          ListHeaderComponent={
            <Text style={[styles.sectionTitle, { color: colors.mutedForeground }]}>
              AVAILABLE SERVERS
            </Text>
          }
        />
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1 },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: 20,
    paddingBottom: 16,
  },
  headerLeft: { flexDirection: 'row', alignItems: 'center', gap: 12 },
  logoWrap: {
    width: 44,
    height: 44,
    borderRadius: 12,
    justifyContent: 'center',
    alignItems: 'center',
    borderWidth: 1,
  },
  appName: { fontSize: 22, fontFamily: 'Inter_700Bold' },
  serverCount: { fontSize: 12, fontFamily: 'Inter_400Regular', marginTop: 1 },
  addBtn: {
    width: 42,
    height: 42,
    borderRadius: 12,
    justifyContent: 'center',
    alignItems: 'center',
  },
  statusCard: {
    borderRadius: 18,
    borderWidth: 1,
    overflow: 'hidden',
    padding: 18,
  },
  statusCardContent: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 16,
  },
  statusIconWrap: {
    width: 56,
    height: 56,
    borderRadius: 16,
    justifyContent: 'center',
    alignItems: 'center',
  },
  statusInfo: { flex: 1, gap: 4 },
  statusRow: { flexDirection: 'row', alignItems: 'center', gap: 6 },
  statusDot: { width: 8, height: 8, borderRadius: 4 },
  statusLabel: { fontSize: 11, fontFamily: 'Inter_700Bold', letterSpacing: 1 },
  statusServer: { fontSize: 17, fontFamily: 'Inter_600SemiBold' },
  statusHost: { fontSize: 13, fontFamily: 'Inter_400Regular' },
  sectionTitle: {
    fontSize: 11,
    fontFamily: 'Inter_600SemiBold',
    letterSpacing: 1.2,
    paddingHorizontal: 16,
    paddingTop: 12,
    paddingBottom: 10,
  },
  list: { paddingTop: 4 },
  centerState: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    paddingHorizontal: 32,
    gap: 12,
  },
  emptyIcon: {
    width: 80,
    height: 80,
    borderRadius: 24,
    justifyContent: 'center',
    alignItems: 'center',
    marginBottom: 8,
  },
  stateTitle: { fontSize: 20, fontFamily: 'Inter_600SemiBold', textAlign: 'center' },
  stateText: { fontSize: 14, fontFamily: 'Inter_400Regular', textAlign: 'center', lineHeight: 20 },
  retryBtn: {
    paddingHorizontal: 24,
    paddingVertical: 12,
    borderRadius: 12,
    marginTop: 8,
  },
  retryText: { fontFamily: 'Inter_600SemiBold', fontSize: 15 },
  addFirstBtn: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
    paddingHorizontal: 24,
    paddingVertical: 13,
    borderRadius: 14,
    marginTop: 8,
  },
  addFirstText: { fontFamily: 'Inter_600SemiBold', fontSize: 15 },
});
