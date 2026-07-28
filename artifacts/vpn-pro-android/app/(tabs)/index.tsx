import React, { useState } from 'react';
import {
  View,
  Text,
  FlatList,
  StyleSheet,
  TouchableOpacity,
  Platform,
  Alert,
  RefreshControl,
  ActivityIndicator,
} from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { Feather } from '@expo/vector-icons';
import { useColors } from '@/hooks/useColors';
import { useServers } from '@/context/ServersContext';
import { ServerCard } from '@/components/ServerCard';
import { ConnectionOrb } from '@/components/ConnectionOrb';
import { AddServerSheet } from '@/components/AddServerSheet';
import type { VpnServer } from '@workspace/api-client-react';

export default function ServersScreen() {
  const colors = useColors();
  const insets = useSafeAreaInsets();
  const {
    servers,
    activeServer,
    connectionState,
    isLoading,
    connect,
    disconnect,
    deleteServer,
    refetch,
  } = useServers();

  const [showAdd, setShowAdd] = useState(false);
  const [editServer, setEditServer] = useState<VpnServer | null>(null);
  const [refreshing, setRefreshing] = useState(false);

  const handleOrbPress = () => {
    if (connectionState === 'connected') {
      Alert.alert('Disconnect', `Disconnect from ${activeServer?.name}?`, [
        { text: 'Cancel', style: 'cancel' },
        { text: 'Disconnect', style: 'destructive', onPress: disconnect },
      ]);
    } else if (connectionState === 'disconnected' && activeServer) {
      connect(activeServer);
    } else if (connectionState === 'disconnected' && servers.length > 0) {
      connect(servers[0]!);
    }
  };

  const handleServerPress = (server: VpnServer) => {
    if (connectionState === 'connected' && activeServer?.id === server.id) {
      Alert.alert('Disconnect', `Disconnect from ${server.name}?`, [
        { text: 'Cancel', style: 'cancel' },
        { text: 'Disconnect', style: 'destructive', onPress: disconnect },
      ]);
    } else {
      Alert.alert(
        `Connect to ${server.name}`,
        `${server.flag} ${server.country} · ${server.type.toUpperCase()}`,
        [
          { text: 'Cancel', style: 'cancel' },
          { text: 'Connect', onPress: () => connect(server) },
        ]
      );
    }
  };

  const handleDelete = (id: string) => {
    Alert.alert('Delete Server', 'Remove this server from the list?', [
      { text: 'Cancel', style: 'cancel' },
      { text: 'Delete', style: 'destructive', onPress: () => deleteServer(id) },
    ]);
  };

  const handleRefresh = async () => {
    setRefreshing(true);
    await refetch();
    setRefreshing(false);
  };

  const s = styles(colors);
  const topPad = Platform.OS === 'web' ? 67 : insets.top;
  const bottomPad = Platform.OS === 'web' ? 84 : insets.bottom + 60;

  return (
    <View style={[s.container, { paddingTop: topPad }]}>
      {/* Header */}
      <View style={s.header}>
        <View>
          <Text style={s.headerTitle}>VPN Pro</Text>
          <Text style={s.headerSub}>{servers.length} servers available</Text>
        </View>
        <TouchableOpacity
          onPress={() => { setEditServer(null); setShowAdd(true); }}
          style={s.addBtn}
        >
          <Feather name="plus" size={22} color={colors.primary} />
        </TouchableOpacity>
      </View>

      <FlatList
        data={servers}
        keyExtractor={item => item.id}
        scrollEnabled={servers.length > 0}
        contentContainerStyle={[s.listContent, { paddingBottom: bottomPad }]}
        refreshControl={
          <RefreshControl
            refreshing={refreshing}
            onRefresh={handleRefresh}
            tintColor={colors.primary}
            colors={[colors.primary]}
          />
        }
        ListHeaderComponent={
          <View style={s.orbSection}>
            <ConnectionOrb
              state={connectionState}
              serverName={activeServer?.name}
              onPress={handleOrbPress}
            />
            {servers.length === 0 && !isLoading && (
              <View style={s.emptyHint}>
                <Feather name="arrow-down" size={18} color={colors.mutedForeground} />
                <Text style={s.emptyHintText}>Scroll down to add your first server</Text>
              </View>
            )}
          </View>
        }
        ListEmptyComponent={
          isLoading ? (
            <ActivityIndicator color={colors.primary} style={{ marginTop: 20 }} />
          ) : (
            <View style={s.emptyState}>
              <Feather name="shield-off" size={40} color={colors.mutedForeground} />
              <Text style={s.emptyTitle}>No Servers Yet</Text>
              <Text style={s.emptyText}>Tap + to add your first VPN server</Text>
              <TouchableOpacity
                style={s.emptyAddBtn}
                onPress={() => { setEditServer(null); setShowAdd(true); }}
              >
                <Text style={s.emptyAddBtnText}>Add Server</Text>
              </TouchableOpacity>
            </View>
          )
        }
        ListFooterComponent={servers.length > 0 ? <View style={s.listFooter} /> : null}
        renderItem={({ item }) => (
          <ServerCard
            server={item}
            isActive={activeServer?.id === item.id}
            isConnected={connectionState === 'connected' && activeServer?.id === item.id}
            onPress={handleServerPress}
            onEdit={(s) => { setEditServer(s); setShowAdd(true); }}
            onDelete={handleDelete}
          />
        )}
        showsVerticalScrollIndicator={false}
      />

      <AddServerSheet
        visible={showAdd}
        onClose={() => { setShowAdd(false); setEditServer(null); }}
        editServer={editServer}
      />
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
      flexDirection: 'row',
      alignItems: 'center',
      justifyContent: 'space-between',
      paddingHorizontal: 20,
      paddingVertical: 12,
      borderBottomWidth: 1,
      borderBottomColor: colors.border,
    },
    headerTitle: {
      color: colors.foreground,
      fontSize: 22,
      fontFamily: 'Inter_700Bold',
    },
    headerSub: {
      color: colors.mutedForeground,
      fontSize: 12,
      fontFamily: 'Inter_400Regular',
      marginTop: 2,
    },
    addBtn: {
      width: 40,
      height: 40,
      borderRadius: 12,
      backgroundColor: colors.primary + '18',
      borderWidth: 1,
      borderColor: colors.primary + '44',
      alignItems: 'center',
      justifyContent: 'center',
    },
    orbSection: {
      alignItems: 'center',
      paddingVertical: 40,
      paddingHorizontal: 20,
    },
    emptyHint: {
      flexDirection: 'row',
      alignItems: 'center',
      gap: 8,
      marginTop: 24,
    },
    emptyHintText: {
      color: colors.mutedForeground,
      fontSize: 13,
      fontFamily: 'Inter_400Regular',
    },
    listContent: {
      paddingHorizontal: 16,
    },
    listFooter: { height: 16 },
    emptyState: {
      alignItems: 'center',
      paddingTop: 20,
      gap: 10,
    },
    emptyTitle: {
      color: colors.foreground,
      fontSize: 18,
      fontFamily: 'Inter_600SemiBold',
    },
    emptyText: {
      color: colors.mutedForeground,
      fontSize: 14,
      fontFamily: 'Inter_400Regular',
    },
    emptyAddBtn: {
      marginTop: 8,
      backgroundColor: colors.primary,
      paddingHorizontal: 24,
      paddingVertical: 12,
      borderRadius: 10,
    },
    emptyAddBtnText: {
      color: colors.primaryForeground,
      fontSize: 15,
      fontFamily: 'Inter_600SemiBold',
    },
  });
