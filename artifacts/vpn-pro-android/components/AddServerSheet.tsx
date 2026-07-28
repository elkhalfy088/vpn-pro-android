import React, { useState, useRef } from 'react';
import {
  View,
  Text,
  TextInput,
  TouchableOpacity,
  StyleSheet,
  ScrollView,
  Alert,
  Platform,
  Modal,
} from 'react-native';
import { Feather } from '@expo/vector-icons';
import * as Haptics from 'expo-haptics';
import { useColors } from '@/hooks/useColors';
import { useCreateServer, useUpdateServer, getListServersQueryKey } from '@workspace/api-client-react';
import type { VpnServer } from '@workspace/api-client-react';
import { useQueryClient } from '@tanstack/react-query';
import { KeyboardAwareScrollViewCompat } from '@/components/KeyboardAwareScrollViewCompat';

const SERVER_TYPES = [
  { value: 'v2ray', label: 'V2Ray', desc: 'vmess:// · vless://' },
  { value: 'vless', label: 'VLESS', desc: 'vless:// protocol' },
  { value: 'trojan', label: 'Trojan', desc: 'trojan:// protocol' },
  { value: 'shadowsocks', label: 'Shadowsocks', desc: 'ss:// protocol' },
  { value: 'hysteria2', label: 'Hysteria2', desc: 'hy2:// protocol' },
  { value: 'proxy', label: 'HTTP Proxy', desc: 'ip:port@user:pass' },
  { value: 'socks5', label: 'SOCKS5', desc: 'socks5://user:pass@ip:port' },
  { value: 'custom', label: 'Custom', desc: 'Any config string' },
] as const;

const COUNTRY_FLAGS: Record<string, string> = {
  'United States': '🇺🇸', 'Germany': '🇩🇪', 'Netherlands': '🇳🇱',
  'France': '🇫🇷', 'UK': '🇬🇧', 'Japan': '🇯🇵', 'Singapore': '🇸🇬',
  'Canada': '🇨🇦', 'Australia': '🇦🇺', 'Russia': '🇷🇺',
  'UAE': '🇦🇪', 'Turkey': '🇹🇷', 'Iran': '🇮🇷', 'Hong Kong': '🇭🇰',
  'South Korea': '🇰🇷', 'Sweden': '🇸🇪', 'Switzerland': '🇨🇭',
  'Poland': '🇵🇱', 'Brazil': '🇧🇷', 'India': '🇮🇳',
};

type ServerType = typeof SERVER_TYPES[number]['value'];

interface Props {
  visible: boolean;
  onClose: () => void;
  editServer?: VpnServer | null;
}

export function AddServerSheet({ visible, onClose, editServer }: Props) {
  const colors = useColors();
  const queryClient = useQueryClient();
  const { mutateAsync: createServer, isPending: creating } = useCreateServer();
  const { mutateAsync: updateServer, isPending: updating } = useUpdateServer();

  const [type, setType] = useState<ServerType>(editServer?.type as ServerType ?? 'v2ray');
  const [name, setName] = useState(editServer?.name ?? '');
  const [config, setConfig] = useState(editServer?.config ?? '');
  const [country, setCountry] = useState(editServer?.country ?? 'United States');
  const [note, setNote] = useState(editServer?.note ?? '');
  const [showCountryPicker, setShowCountryPicker] = useState(false);

  const flag = COUNTRY_FLAGS[country] ?? '🌐';
  const isPending = creating || updating;

  React.useEffect(() => {
    if (visible && editServer) {
      setType(editServer.type as ServerType);
      setName(editServer.name);
      setConfig(editServer.config);
      setCountry(editServer.country);
      setNote(editServer.note ?? '');
    } else if (visible && !editServer) {
      setType('v2ray');
      setName('');
      setConfig('');
      setCountry('United States');
      setNote('');
    }
  }, [visible, editServer]);

  const getPlaceholder = () => {
    switch (type) {
      case 'v2ray': return 'vmess://eyJ...base64';
      case 'vless': return 'vless://uuid@host:port?...';
      case 'trojan': return 'trojan://password@host:port';
      case 'shadowsocks': return 'ss://method:password@host:port';
      case 'hysteria2': return 'hy2://auth@host:port';
      case 'proxy': return '192.168.1.1:8080@username:password';
      case 'socks5': return 'socks5://user:pass@host:port';
      default: return 'Paste your config here';
    }
  };

  const handleSave = async () => {
    if (!name.trim()) return Alert.alert('Error', 'Server name is required');
    if (!config.trim()) return Alert.alert('Error', 'Server config is required');

    if (Platform.OS !== 'web') Haptics.notificationAsync(Haptics.NotificationFeedbackType.Success);

    try {
      if (editServer) {
        await updateServer({ id: editServer.id, data: { name: name.trim(), type, config: config.trim(), country, flag, note: note.trim() || undefined } });
      } else {
        await createServer({ data: { name: name.trim(), type, config: config.trim(), country, flag, note: note.trim() || undefined } });
      }
      await queryClient.invalidateQueries({ queryKey: getListServersQueryKey() });
      onClose();
    } catch {
      Alert.alert('Error', 'Failed to save server. Please try again.');
    }
  };

  const s = styles(colors);

  return (
    <Modal visible={visible} animationType="slide" presentationStyle="pageSheet" onRequestClose={onClose}>
      <View style={s.container}>
        <View style={s.header}>
          <TouchableOpacity onPress={onClose} style={s.cancelBtn}>
            <Text style={s.cancelText}>Cancel</Text>
          </TouchableOpacity>
          <Text style={s.title}>{editServer ? 'Edit Server' : 'Add Server'}</Text>
          <TouchableOpacity onPress={handleSave} disabled={isPending} style={s.saveBtn}>
            <Text style={[s.saveText, isPending && s.savePending]}>
              {isPending ? 'Saving...' : 'Save'}
            </Text>
          </TouchableOpacity>
        </View>

        <KeyboardAwareScrollViewCompat
          style={s.scroll}
          contentContainerStyle={s.scrollContent}
          keyboardShouldPersistTaps="handled"
          bottomOffset={20}
        >
          {/* Server Type */}
          <Text style={s.sectionLabel}>Protocol Type</Text>
          <ScrollView horizontal showsHorizontalScrollIndicator={false} style={s.typeRow} contentContainerStyle={{ gap: 8, paddingRight: 16 }}>
            {SERVER_TYPES.map(t => (
              <TouchableOpacity
                key={t.value}
                onPress={() => setType(t.value)}
                style={[s.typeChip, type === t.value && s.typeChipActive]}
              >
                <Text style={[s.typeChipLabel, type === t.value && s.typeChipLabelActive]}>{t.label}</Text>
                <Text style={[s.typeChipDesc, type === t.value && s.typeChipDescActive]}>{t.desc}</Text>
              </TouchableOpacity>
            ))}
          </ScrollView>

          {/* Name */}
          <Text style={s.sectionLabel}>Server Name</Text>
          <TextInput
            style={s.input}
            value={name}
            onChangeText={setName}
            placeholder="e.g. US-Fast-1"
            placeholderTextColor={colors.mutedForeground}
            autoCorrect={false}
          />

          {/* Config */}
          <Text style={s.sectionLabel}>Configuration</Text>
          <TextInput
            style={[s.input, s.configInput]}
            value={config}
            onChangeText={setConfig}
            placeholder={getPlaceholder()}
            placeholderTextColor={colors.mutedForeground}
            multiline
            autoCorrect={false}
            autoCapitalize="none"
            spellCheck={false}
          />

          {/* Country */}
          <Text style={s.sectionLabel}>Country</Text>
          <TouchableOpacity
            style={s.input}
            onPress={() => setShowCountryPicker(true)}
          >
            <Text style={s.countryValue}>{flag} {country}</Text>
          </TouchableOpacity>

          {/* Note */}
          <Text style={s.sectionLabel}>Note (optional)</Text>
          <TextInput
            style={s.input}
            value={note}
            onChangeText={setNote}
            placeholder="Any notes..."
            placeholderTextColor={colors.mutedForeground}
          />
        </KeyboardAwareScrollViewCompat>

        {/* Country Picker Modal */}
        <Modal visible={showCountryPicker} animationType="slide" presentationStyle="pageSheet" onRequestClose={() => setShowCountryPicker(false)}>
          <View style={s.container}>
            <View style={s.header}>
              <View style={{ width: 60 }} />
              <Text style={s.title}>Select Country</Text>
              <TouchableOpacity onPress={() => setShowCountryPicker(false)} style={s.saveBtn}>
                <Text style={s.saveText}>Done</Text>
              </TouchableOpacity>
            </View>
            <ScrollView contentContainerStyle={{ paddingBottom: 40 }}>
              {Object.entries(COUNTRY_FLAGS).map(([c, f]) => (
                <TouchableOpacity
                  key={c}
                  style={[s.countryRow, country === c && s.countryRowActive]}
                  onPress={() => { setCountry(c); setShowCountryPicker(false); }}
                >
                  <Text style={s.countryFlag}>{f}</Text>
                  <Text style={[s.countryLabel, country === c && s.countryLabelActive]}>{c}</Text>
                  {country === c && <Feather name="check" size={18} color={colors.primary} />}
                </TouchableOpacity>
              ))}
            </ScrollView>
          </View>
        </Modal>
      </View>
    </Modal>
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
      paddingVertical: 16,
      borderBottomWidth: 1,
      borderBottomColor: colors.border,
    },
    title: {
      color: colors.foreground,
      fontSize: 17,
      fontFamily: 'Inter_600SemiBold',
    },
    cancelBtn: { width: 60 },
    cancelText: { color: colors.mutedForeground, fontSize: 16, fontFamily: 'Inter_400Regular' },
    saveBtn: { width: 60, alignItems: 'flex-end' },
    saveText: { color: colors.primary, fontSize: 16, fontFamily: 'Inter_600SemiBold' },
    savePending: { opacity: 0.5 },
    scroll: { flex: 1 },
    scrollContent: { padding: 20, paddingBottom: 60 },
    sectionLabel: {
      color: colors.mutedForeground,
      fontSize: 12,
      fontFamily: 'Inter_600SemiBold',
      letterSpacing: 0.8,
      textTransform: 'uppercase',
      marginBottom: 8,
      marginTop: 20,
    },
    typeRow: { marginBottom: 4 },
    typeChip: {
      paddingHorizontal: 14,
      paddingVertical: 10,
      borderRadius: 10,
      borderWidth: 1,
      borderColor: colors.border,
      backgroundColor: colors.card,
      minWidth: 100,
    },
    typeChipActive: {
      borderColor: colors.primary,
      backgroundColor: colors.primary + '18',
    },
    typeChipLabel: {
      color: colors.mutedForeground,
      fontSize: 13,
      fontFamily: 'Inter_600SemiBold',
    },
    typeChipLabelActive: { color: colors.primary },
    typeChipDesc: {
      color: colors.mutedForeground,
      fontSize: 11,
      fontFamily: 'Inter_400Regular',
      marginTop: 2,
    },
    typeChipDescActive: { color: colors.primary + 'aa' },
    input: {
      backgroundColor: colors.card,
      borderWidth: 1,
      borderColor: colors.border,
      borderRadius: 12,
      paddingHorizontal: 16,
      paddingVertical: 14,
      color: colors.foreground,
      fontSize: 15,
      fontFamily: 'Inter_400Regular',
    },
    configInput: {
      minHeight: 100,
      textAlignVertical: 'top',
      fontFamily: 'Inter_400Regular',
      fontSize: 13,
    },
    countryValue: {
      color: colors.foreground,
      fontSize: 15,
      fontFamily: 'Inter_400Regular',
    },
    countryRow: {
      flexDirection: 'row',
      alignItems: 'center',
      paddingHorizontal: 20,
      paddingVertical: 14,
      borderBottomWidth: 1,
      borderBottomColor: colors.border,
      gap: 14,
    },
    countryRowActive: { backgroundColor: colors.primary + '11' },
    countryFlag: { fontSize: 22 },
    countryLabel: {
      flex: 1,
      color: colors.foreground,
      fontSize: 15,
      fontFamily: 'Inter_400Regular',
    },
    countryLabelActive: { color: colors.primary, fontFamily: 'Inter_600SemiBold' },
  });
