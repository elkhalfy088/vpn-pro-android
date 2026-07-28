import React, { useState, useEffect } from 'react';
import {
  View,
  Text,
  StyleSheet,
  TouchableOpacity,
  ScrollView,
  TextInput,
  Alert,
  Platform,
  KeyboardAvoidingView,
  ActivityIndicator,
} from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { Feather } from '@expo/vector-icons';
import { useColors } from '@/hooks/useColors';
import {
  useGetServer,
  useUpdateServer,
  getGetServersQueryKey,
  getGetServerQueryKey,
} from '@workspace/api-client-react';
import { useQueryClient } from '@tanstack/react-query';
import { router, useLocalSearchParams } from 'expo-router';
import * as Haptics from 'expo-haptics';

const FLAGS = [
  { flag: '🇺🇸', country: 'United States' },
  { flag: '🇩🇪', country: 'Germany' },
  { flag: '🇯🇵', country: 'Japan' },
  { flag: '🇳🇱', country: 'Netherlands' },
  { flag: '🇸🇬', country: 'Singapore' },
  { flag: '🇬🇧', country: 'United Kingdom' },
  { flag: '🇫🇷', country: 'France' },
  { flag: '🇰🇷', country: 'South Korea' },
  { flag: '🇨🇦', country: 'Canada' },
  { flag: '🇦🇺', country: 'Australia' },
  { flag: '🇧🇷', country: 'Brazil' },
  { flag: '🇮🇳', country: 'India' },
  { flag: '🌐', country: 'Global' },
];

export default function EditServerScreen() {
  const colors = useColors();
  const insets = useSafeAreaInsets();
  const queryClient = useQueryClient();
  const { id } = useLocalSearchParams<{ id: string }>();
  const serverId = parseInt(id ?? '0');

  const { data: server, isLoading } = useGetServer(serverId, {
    query: { enabled: !!serverId, queryKey: getGetServerQueryKey(serverId) },
  });
  const updateMutation = useUpdateServer();

  const [name, setName] = useState('');
  const [config, setConfig] = useState('');
  const [host, setHost] = useState('');
  const [port, setPort] = useState('');
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [selectedFlag, setSelectedFlag] = useState(FLAGS[12]);
  const [showFlags, setShowFlags] = useState(false);

  useEffect(() => {
    if (server) {
      setName(server.name ?? '');
      setConfig(server.config ?? '');
      setHost(server.host ?? '');
      setPort(server.port?.toString() ?? '');
      setUsername(server.username ?? '');
      setPassword(server.password ?? '');
      const found = FLAGS.find(f => f.flag === server.flag);
      if (found) setSelectedFlag(found);
    }
  }, [server]);

  const isLink = server && ['vmess', 'vless', 'trojan', 'shadowsocks'].includes(server.type);
  const isJson = server?.type === 'v2ray';
  const isProxy = server?.type === 'proxy';

  const handleSave = async () => {
    if (!name.trim()) {
      Alert.alert('Missing Name', 'Please enter a server name.');
      return;
    }

    const payload: Record<string, unknown> = {
      name: name.trim(),
      flag: selectedFlag.flag,
      country: selectedFlag.country,
    };

    if (isLink || isJson) payload.config = config;
    if (isProxy) {
      payload.host = host;
      if (port) payload.port = parseInt(port);
      if (username) payload.username = username;
      if (password) payload.password = password;
    }

    try {
      await updateMutation.mutateAsync({ id: serverId, data: payload as any });
      queryClient.invalidateQueries({ queryKey: getGetServersQueryKey() });
      queryClient.invalidateQueries({ queryKey: getGetServerQueryKey(serverId) });
      await Haptics.notificationAsync(Haptics.NotificationFeedbackType.Success);
      router.back();
    } catch {
      Alert.alert('Error', 'Failed to update server.');
      await Haptics.notificationAsync(Haptics.NotificationFeedbackType.Error);
    }
  };

  const topPad = Platform.OS === 'web' ? 67 : insets.top;
  const bottomPad = Platform.OS === 'web' ? 34 : insets.bottom;

  if (isLoading || !server) {
    return (
      <View style={[styles.container, { backgroundColor: colors.background, justifyContent: 'center', alignItems: 'center' }]}>
        <ActivityIndicator size="large" color={colors.primary} />
      </View>
    );
  }

  return (
    <KeyboardAvoidingView
      style={[styles.container, { backgroundColor: colors.background }]}
      behavior={Platform.OS === 'ios' ? 'padding' : 'height'}
    >
      <View style={[styles.header, { paddingTop: topPad + 12 }]}>
        <TouchableOpacity
          style={[styles.backBtn, { backgroundColor: colors.surface }]}
          onPress={() => router.back()}
        >
          <Feather name="chevron-left" size={22} color={colors.foreground} />
        </TouchableOpacity>
        <Text style={[styles.headerTitle, { color: colors.foreground }]}>Edit Server</Text>
        <TouchableOpacity
          style={[styles.saveBtn, { backgroundColor: colors.primary }]}
          onPress={handleSave}
          disabled={updateMutation.isPending}
        >
          <Text style={[styles.saveBtnText, { color: colors.primaryForeground }]}>
            {updateMutation.isPending ? 'Saving…' : 'Save'}
          </Text>
        </TouchableOpacity>
      </View>

      <ScrollView
        contentContainerStyle={[styles.scroll, { paddingBottom: bottomPad + 20 }]}
        showsVerticalScrollIndicator={false}
        keyboardShouldPersistTaps="handled"
      >
        <Text style={[styles.fieldLabel, { color: colors.mutedForeground }]}>SERVER NAME</Text>
        <View style={[styles.inputWrap, { backgroundColor: colors.card, borderColor: colors.border }]}>
          <Feather name="tag" size={16} color={colors.mutedForeground} style={{ marginRight: 10 }} />
          <TextInput
            style={[styles.input, { color: colors.foreground }]}
            value={name}
            onChangeText={setName}
            placeholder="Server name"
            placeholderTextColor={colors.mutedForeground}
          />
        </View>

        <Text style={[styles.fieldLabel, { color: colors.mutedForeground }]}>COUNTRY</Text>
        <TouchableOpacity
          style={[styles.inputWrap, { backgroundColor: colors.card, borderColor: colors.border }]}
          onPress={() => setShowFlags(!showFlags)}
        >
          <Text style={styles.flagPick}>{selectedFlag.flag}</Text>
          <Text style={[styles.countryText, { color: colors.foreground }]}>{selectedFlag.country}</Text>
          <Feather name={showFlags ? 'chevron-up' : 'chevron-down'} size={16} color={colors.mutedForeground} />
        </TouchableOpacity>
        {showFlags && (
          <View style={[styles.flagGrid, { backgroundColor: colors.card, borderColor: colors.border }]}>
            {FLAGS.map(f => (
              <TouchableOpacity
                key={f.flag}
                style={[
                  styles.flagItem,
                  selectedFlag.flag === f.flag && { backgroundColor: colors.primaryGlow },
                ]}
                onPress={() => { setSelectedFlag(f); setShowFlags(false); Haptics.selectionAsync(); }}
              >
                <Text style={styles.flagEmoji}>{f.flag}</Text>
                <Text style={[styles.flagLabel, { color: colors.mutedForeground }]} numberOfLines={1}>
                  {f.country.split(' ')[0]}
                </Text>
              </TouchableOpacity>
            ))}
          </View>
        )}

        {(isLink || isJson) && (
          <>
            <Text style={[styles.fieldLabel, { color: colors.mutedForeground }]}>
              {isJson ? 'V2RAY JSON CONFIG' : 'SERVER LINK'}
            </Text>
            <View style={[styles.textAreaWrap, { backgroundColor: colors.card, borderColor: colors.border }]}>
              <TextInput
                style={[styles.textArea, { color: colors.foreground }]}
                value={config}
                onChangeText={setConfig}
                multiline
                textAlignVertical="top"
                autoCapitalize="none"
                autoCorrect={false}
                placeholder={isJson ? '{ "inbounds": [...] }' : `${server.type}://…`}
                placeholderTextColor={colors.mutedForeground}
              />
            </View>
          </>
        )}

        {isProxy && (
          <>
            <Text style={[styles.fieldLabel, { color: colors.mutedForeground }]}>HOST / IP</Text>
            <View style={[styles.inputWrap, { backgroundColor: colors.card, borderColor: colors.border }]}>
              <Feather name="server" size={16} color={colors.mutedForeground} style={{ marginRight: 10 }} />
              <TextInput
                style={[styles.input, { color: colors.foreground }]}
                value={host}
                onChangeText={setHost}
                autoCapitalize="none"
                autoCorrect={false}
                keyboardType="url"
                placeholder="192.168.1.1"
                placeholderTextColor={colors.mutedForeground}
              />
            </View>

            <Text style={[styles.fieldLabel, { color: colors.mutedForeground }]}>PORT</Text>
            <View style={[styles.inputWrap, { backgroundColor: colors.card, borderColor: colors.border }]}>
              <Feather name="hash" size={16} color={colors.mutedForeground} style={{ marginRight: 10 }} />
              <TextInput
                style={[styles.input, { color: colors.foreground }]}
                value={port}
                onChangeText={setPort}
                keyboardType="number-pad"
                placeholder="8080"
                placeholderTextColor={colors.mutedForeground}
              />
            </View>

            <Text style={[styles.fieldLabel, { color: colors.mutedForeground }]}>USERNAME</Text>
            <View style={[styles.inputWrap, { backgroundColor: colors.card, borderColor: colors.border }]}>
              <Feather name="user" size={16} color={colors.mutedForeground} style={{ marginRight: 10 }} />
              <TextInput
                style={[styles.input, { color: colors.foreground }]}
                value={username}
                onChangeText={setUsername}
                autoCapitalize="none"
                autoCorrect={false}
                placeholder="username"
                placeholderTextColor={colors.mutedForeground}
              />
            </View>

            <Text style={[styles.fieldLabel, { color: colors.mutedForeground }]}>PASSWORD</Text>
            <View style={[styles.inputWrap, { backgroundColor: colors.card, borderColor: colors.border }]}>
              <Feather name="lock" size={16} color={colors.mutedForeground} style={{ marginRight: 10 }} />
              <TextInput
                style={[styles.input, { color: colors.foreground }]}
                value={password}
                onChangeText={setPassword}
                secureTextEntry
                autoCapitalize="none"
                autoCorrect={false}
                placeholder="password"
                placeholderTextColor={colors.mutedForeground}
              />
            </View>
          </>
        )}
      </ScrollView>
    </KeyboardAvoidingView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1 },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: 16,
    paddingBottom: 12,
  },
  backBtn: {
    width: 40,
    height: 40,
    borderRadius: 12,
    justifyContent: 'center',
    alignItems: 'center',
  },
  headerTitle: { fontSize: 18, fontFamily: 'Inter_600SemiBold' },
  saveBtn: {
    paddingHorizontal: 16,
    paddingVertical: 9,
    borderRadius: 10,
    minWidth: 60,
    alignItems: 'center',
  },
  saveBtnText: { fontFamily: 'Inter_600SemiBold', fontSize: 14 },
  scroll: { padding: 16, gap: 8 },
  fieldLabel: {
    fontSize: 11,
    fontFamily: 'Inter_600SemiBold',
    letterSpacing: 1.1,
    marginTop: 16,
    marginBottom: 6,
  },
  inputWrap: {
    flexDirection: 'row',
    alignItems: 'center',
    borderRadius: 12,
    borderWidth: 1,
    paddingHorizontal: 14,
    paddingVertical: 14,
  },
  input: {
    flex: 1,
    fontSize: 15,
    fontFamily: 'Inter_400Regular',
  },
  textAreaWrap: {
    borderRadius: 12,
    borderWidth: 1,
    padding: 14,
    minHeight: 140,
  },
  textArea: {
    fontSize: 13,
    fontFamily: 'Inter_400Regular',
    minHeight: 120,
    lineHeight: 20,
  },
  flagPick: { fontSize: 24, marginRight: 10 },
  countryText: { flex: 1, fontSize: 15, fontFamily: 'Inter_400Regular' },
  flagGrid: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    borderRadius: 12,
    borderWidth: 1,
    padding: 8,
    gap: 4,
    marginTop: 4,
  },
  flagItem: {
    width: '22%',
    alignItems: 'center',
    paddingVertical: 8,
    borderRadius: 8,
    gap: 4,
  },
  flagEmoji: { fontSize: 24 },
  flagLabel: { fontSize: 10, fontFamily: 'Inter_400Regular' },
});
