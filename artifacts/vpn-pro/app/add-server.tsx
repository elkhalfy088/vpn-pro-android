import React, { useState } from 'react';
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
} from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { Feather, MaterialCommunityIcons } from '@expo/vector-icons';
import { useColors } from '@/hooks/useColors';
import { useCreateServer, getGetServersQueryKey } from '@workspace/api-client-react';
import { useQueryClient } from '@tanstack/react-query';
import { router } from 'expo-router';
import * as Haptics from 'expo-haptics';
import Animated, { FadeInDown } from 'react-native-reanimated';

type ServerType = 'v2ray' | 'proxy' | 'vmess' | 'vless' | 'trojan' | 'shadowsocks';

const SERVER_TYPES: { type: ServerType; label: string; desc: string; icon: string; color: string }[] = [
  { type: 'vmess',       label: 'VMess',       desc: 'V2Ray VMess link (vmess://…)',     icon: 'server-network',             color: '#7B5CF0' },
  { type: 'vless',       label: 'VLess',       desc: 'V2Ray VLess link (vless://…)',     icon: 'server-network',             color: '#9F7AEA' },
  { type: 'trojan',      label: 'Trojan',      desc: 'Trojan link (trojan://…)',          icon: 'shield-lock',                color: '#F6AD55' },
  { type: 'shadowsocks', label: 'Shadowsocks', desc: 'Shadowsocks link (ss://…)',         icon: 'server-security',            color: '#68D391' },
  { type: 'v2ray',       label: 'V2Ray JSON',  desc: 'Paste raw V2Ray JSON config',      icon: 'code-json',                  color: '#00C9FF' },
  { type: 'proxy',       label: 'HTTP/SOCKS',  desc: 'ip:port or ip:port@user:pass',     icon: 'transit-connection-variant', color: '#FC8181' },
];

const FLAGS: { flag: string; country: string }[] = [
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

function parseProxyString(input: string) {
  // Formats: host:port, host:port@user:pass, user:pass@host:port
  const trimmed = input.trim();
  
  // Try user:pass@host:port
  const atIdx = trimmed.lastIndexOf('@');
  if (atIdx > 0) {
    const creds = trimmed.substring(0, atIdx);
    const hostPort = trimmed.substring(atIdx + 1);
    const colonIdx = hostPort.lastIndexOf(':');
    const host = colonIdx > 0 ? hostPort.substring(0, colonIdx) : hostPort;
    const port = colonIdx > 0 ? parseInt(hostPort.substring(colonIdx + 1)) : undefined;
    const credParts = creds.split(':');
    return {
      host,
      port: isNaN(port!) ? undefined : port,
      username: credParts[0],
      password: credParts.slice(1).join(':'),
    };
  }

  // Try host:port
  const parts = trimmed.split(':');
  if (parts.length >= 2) {
    const port = parseInt(parts[parts.length - 1]);
    const host = parts.slice(0, -1).join(':');
    return { host, port: isNaN(port) ? undefined : port, username: undefined, password: undefined };
  }

  return { host: trimmed, port: undefined, username: undefined, password: undefined };
}

export default function AddServerScreen() {
  const colors = useColors();
  const insets = useSafeAreaInsets();
  const queryClient = useQueryClient();
  const createMutation = useCreateServer();

  const [step, setStep] = useState<'type' | 'config'>('type');
  const [selectedType, setSelectedType] = useState<ServerType | null>(null);
  const [name, setName] = useState('');
  const [config, setConfig] = useState('');
  const [host, setHost] = useState('');
  const [port, setPort] = useState('');
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [selectedFlag, setSelectedFlag] = useState(FLAGS[12]);
  const [showFlags, setShowFlags] = useState(false);

  const typeInfo = selectedType ? SERVER_TYPES.find(t => t.type === selectedType) : null;
  const isLink = selectedType && ['vmess', 'vless', 'trojan', 'shadowsocks'].includes(selectedType);
  const isJson = selectedType === 'v2ray';
  const isProxy = selectedType === 'proxy';

  const handleSelectType = (type: ServerType) => {
    setSelectedType(type);
    setStep('config');
    Haptics.selectionAsync();
  };

  const handleSave = async () => {
    if (!name.trim()) {
      Alert.alert('Missing Name', 'Please enter a server name.');
      return;
    }
    if (!selectedType) return;

    let payload: Record<string, unknown> = {
      name: name.trim(),
      type: selectedType,
      flag: selectedFlag.flag,
      country: selectedFlag.country,
    };

    if (isLink || isJson) {
      if (!config.trim()) {
        Alert.alert('Missing Config', `Please paste the ${isJson ? 'JSON config' : 'link'}.`);
        return;
      }
      payload.config = config.trim();
    } else if (isProxy) {
      if (!host.trim()) {
        Alert.alert('Missing Host', 'Please enter the host address.');
        return;
      }
      payload.host = host.trim();
      if (port) payload.port = parseInt(port);
      if (username) payload.username = username.trim();
      if (password) payload.password = password;
    }

    try {
      await createMutation.mutateAsync({ data: payload as any });
      queryClient.invalidateQueries({ queryKey: getGetServersQueryKey() });
      await Haptics.notificationAsync(Haptics.NotificationFeedbackType.Success);
      router.back();
    } catch {
      Alert.alert('Error', 'Failed to save server. Please try again.');
      await Haptics.notificationAsync(Haptics.NotificationFeedbackType.Error);
    }
  };

  const handleQuickParse = () => {
    if (!config.trim()) return;
    const parsed = parseProxyString(config);
    if (parsed.host) setHost(parsed.host);
    if (parsed.port) setPort(parsed.port.toString());
    if (parsed.username) setUsername(parsed.username);
    if (parsed.password) setPassword(parsed.password);
    setConfig('');
    Haptics.selectionAsync();
  };

  const topPad = Platform.OS === 'web' ? 67 : insets.top;
  const bottomPad = Platform.OS === 'web' ? 34 : insets.bottom;

  return (
    <KeyboardAvoidingView
      style={[styles.container, { backgroundColor: colors.background }]}
      behavior={Platform.OS === 'ios' ? 'padding' : 'height'}
    >
      {/* Header */}
      <View style={[styles.header, { paddingTop: topPad + 12 }]}>
        <TouchableOpacity
          style={[styles.backBtn, { backgroundColor: colors.surface }]}
          onPress={() => {
            if (step === 'config') { setStep('type'); setSelectedType(null); }
            else router.back();
          }}
        >
          <Feather name="chevron-left" size={22} color={colors.foreground} />
        </TouchableOpacity>
        <Text style={[styles.headerTitle, { color: colors.foreground }]}>
          {step === 'type' ? 'Add Server' : `New ${typeInfo?.label} Server`}
        </Text>
        {step === 'config' ? (
          <TouchableOpacity
            style={[styles.saveBtn, { backgroundColor: colors.primary }]}
            onPress={handleSave}
            disabled={createMutation.isPending}
          >
            <Text style={[styles.saveBtnText, { color: colors.primaryForeground }]}>
              {createMutation.isPending ? 'Saving…' : 'Save'}
            </Text>
          </TouchableOpacity>
        ) : (
          <View style={{ width: 60 }} />
        )}
      </View>

      <ScrollView
        contentContainerStyle={[styles.scroll, { paddingBottom: bottomPad + 20 }]}
        showsVerticalScrollIndicator={false}
        keyboardShouldPersistTaps="handled"
      >
        {step === 'type' ? (
          <>
            <Text style={[styles.sectionLabel, { color: colors.mutedForeground }]}>SELECT SERVER TYPE</Text>
            {SERVER_TYPES.map((t, i) => (
              <Animated.View key={t.type} entering={FadeInDown.delay(i * 50).springify()}>
                <TouchableOpacity
                  style={[styles.typeRow, { backgroundColor: colors.card, borderColor: colors.border }]}
                  onPress={() => handleSelectType(t.type)}
                  activeOpacity={0.75}
                >
                  <View style={[styles.typeIcon, { backgroundColor: t.color + '22' }]}>
                    <MaterialCommunityIcons name={t.icon as any} size={22} color={t.color} />
                  </View>
                  <View style={styles.typeInfo}>
                    <Text style={[styles.typeRowLabel, { color: colors.foreground }]}>{t.label}</Text>
                    <Text style={[styles.typeRowDesc, { color: colors.mutedForeground }]}>{t.desc}</Text>
                  </View>
                  <Feather name="chevron-right" size={18} color={colors.mutedForeground} />
                </TouchableOpacity>
              </Animated.View>
            ))}
          </>
        ) : (
          <>
            {/* Server Name */}
            <Text style={[styles.fieldLabel, { color: colors.mutedForeground }]}>SERVER NAME</Text>
            <View style={[styles.inputWrap, { backgroundColor: colors.card, borderColor: colors.border }]}>
              <Feather name="tag" size={16} color={colors.mutedForeground} style={{ marginRight: 10 }} />
              <TextInput
                style={[styles.input, { color: colors.foreground }]}
                placeholder="e.g. US Server 1"
                placeholderTextColor={colors.mutedForeground}
                value={name}
                onChangeText={setName}
                autoFocus
              />
            </View>

            {/* Flag picker */}
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

            {/* Config fields based on type */}
            {(isLink || isJson) && (
              <>
                <Text style={[styles.fieldLabel, { color: colors.mutedForeground }]}>
                  {isJson ? 'V2RAY JSON CONFIG' : `${typeInfo?.label.toUpperCase()} LINK`}
                </Text>
                <View style={[styles.textAreaWrap, { backgroundColor: colors.card, borderColor: colors.border }]}>
                  <TextInput
                    style={[styles.textArea, { color: colors.foreground }]}
                    placeholder={isJson
                      ? '{\n  "inbounds": [...],\n  "outbounds": [...]\n}'
                      : `${selectedType}://…`}
                    placeholderTextColor={colors.mutedForeground}
                    value={config}
                    onChangeText={setConfig}
                    multiline
                    textAlignVertical="top"
                    autoCapitalize="none"
                    autoCorrect={false}
                  />
                </View>
              </>
            )}

            {isProxy && (
              <>
                {/* Quick paste parser */}
                <Text style={[styles.fieldLabel, { color: colors.mutedForeground }]}>QUICK PASTE (ip:port or ip:port@user:pass)</Text>
                <View style={[styles.rowWrap, { backgroundColor: colors.card, borderColor: colors.border }]}>
                  <TextInput
                    style={[styles.input, { color: colors.foreground, flex: 1 }]}
                    placeholder="192.168.1.1:8080@user:pass"
                    placeholderTextColor={colors.mutedForeground}
                    value={config}
                    onChangeText={setConfig}
                    autoCapitalize="none"
                    autoCorrect={false}
                  />
                  <TouchableOpacity
                    style={[styles.parseBtn, { backgroundColor: colors.primaryGlow, borderColor: colors.primary }]}
                    onPress={handleQuickParse}
                  >
                    <Text style={[styles.parseBtnText, { color: colors.primary }]}>Parse</Text>
                  </TouchableOpacity>
                </View>

                <Text style={[styles.orLabel, { color: colors.mutedForeground }]}>— OR ENTER MANUALLY —</Text>

                <Text style={[styles.fieldLabel, { color: colors.mutedForeground }]}>HOST / IP ADDRESS</Text>
                <View style={[styles.inputWrap, { backgroundColor: colors.card, borderColor: colors.border }]}>
                  <Feather name="server" size={16} color={colors.mutedForeground} style={{ marginRight: 10 }} />
                  <TextInput
                    style={[styles.input, { color: colors.foreground }]}
                    placeholder="192.168.1.1 or example.com"
                    placeholderTextColor={colors.mutedForeground}
                    value={host}
                    onChangeText={setHost}
                    autoCapitalize="none"
                    autoCorrect={false}
                    keyboardType="url"
                  />
                </View>

                <Text style={[styles.fieldLabel, { color: colors.mutedForeground }]}>PORT</Text>
                <View style={[styles.inputWrap, { backgroundColor: colors.card, borderColor: colors.border }]}>
                  <Feather name="hash" size={16} color={colors.mutedForeground} style={{ marginRight: 10 }} />
                  <TextInput
                    style={[styles.input, { color: colors.foreground }]}
                    placeholder="8080"
                    placeholderTextColor={colors.mutedForeground}
                    value={port}
                    onChangeText={setPort}
                    keyboardType="number-pad"
                  />
                </View>

                <Text style={[styles.fieldLabel, { color: colors.mutedForeground }]}>USERNAME (optional)</Text>
                <View style={[styles.inputWrap, { backgroundColor: colors.card, borderColor: colors.border }]}>
                  <Feather name="user" size={16} color={colors.mutedForeground} style={{ marginRight: 10 }} />
                  <TextInput
                    style={[styles.input, { color: colors.foreground }]}
                    placeholder="username"
                    placeholderTextColor={colors.mutedForeground}
                    value={username}
                    onChangeText={setUsername}
                    autoCapitalize="none"
                    autoCorrect={false}
                  />
                </View>

                <Text style={[styles.fieldLabel, { color: colors.mutedForeground }]}>PASSWORD (optional)</Text>
                <View style={[styles.inputWrap, { backgroundColor: colors.card, borderColor: colors.border }]}>
                  <Feather name="lock" size={16} color={colors.mutedForeground} style={{ marginRight: 10 }} />
                  <TextInput
                    style={[styles.input, { color: colors.foreground }]}
                    placeholder="password"
                    placeholderTextColor={colors.mutedForeground}
                    value={password}
                    onChangeText={setPassword}
                    secureTextEntry
                    autoCapitalize="none"
                    autoCorrect={false}
                  />
                </View>
              </>
            )}
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
  sectionLabel: {
    fontSize: 11,
    fontFamily: 'Inter_600SemiBold',
    letterSpacing: 1.2,
    marginBottom: 8,
    marginTop: 4,
  },
  typeRow: {
    flexDirection: 'row',
    alignItems: 'center',
    borderRadius: 14,
    borderWidth: 1,
    padding: 16,
    marginBottom: 10,
    gap: 14,
  },
  typeIcon: {
    width: 44,
    height: 44,
    borderRadius: 12,
    justifyContent: 'center',
    alignItems: 'center',
  },
  typeInfo: { flex: 1 },
  typeRowLabel: { fontSize: 16, fontFamily: 'Inter_600SemiBold', marginBottom: 2 },
  typeRowDesc: { fontSize: 12, fontFamily: 'Inter_400Regular' },
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
  rowWrap: {
    flexDirection: 'row',
    alignItems: 'center',
    borderRadius: 12,
    borderWidth: 1,
    paddingHorizontal: 14,
    paddingVertical: 10,
    gap: 8,
  },
  parseBtn: {
    paddingHorizontal: 14,
    paddingVertical: 8,
    borderRadius: 8,
    borderWidth: 1,
  },
  parseBtnText: { fontFamily: 'Inter_600SemiBold', fontSize: 13 },
  orLabel: {
    textAlign: 'center',
    fontSize: 11,
    fontFamily: 'Inter_500Medium',
    letterSpacing: 1,
    marginVertical: 12,
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
