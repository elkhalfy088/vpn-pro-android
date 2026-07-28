import React, { useEffect, useRef } from 'react';
import {
  View,
  Text,
  TouchableOpacity,
  StyleSheet,
  Animated,
  Platform,
} from 'react-native';
import { MaterialCommunityIcons } from '@expo/vector-icons';
import * as Haptics from 'expo-haptics';
import { useColors } from '@/hooks/useColors';

type ConnectionState = 'disconnected' | 'connecting' | 'connected';

interface ConnectionOrbProps {
  state: ConnectionState;
  serverName?: string;
  onPress: () => void;
}

export function ConnectionOrb({ state, serverName, onPress }: ConnectionOrbProps) {
  const colors = useColors();
  const pulseAnim = useRef(new Animated.Value(1)).current;
  const glowAnim = useRef(new Animated.Value(0)).current;
  const rotateAnim = useRef(new Animated.Value(0)).current;

  useEffect(() => {
    if (state === 'connecting') {
      Animated.loop(
        Animated.sequence([
          Animated.timing(rotateAnim, { toValue: 1, duration: 1000, useNativeDriver: true }),
          Animated.timing(rotateAnim, { toValue: 0, duration: 0, useNativeDriver: true }),
        ])
      ).start();
    } else {
      rotateAnim.setValue(0);
    }

    if (state === 'connected') {
      Animated.loop(
        Animated.sequence([
          Animated.timing(pulseAnim, { toValue: 1.08, duration: 2000, useNativeDriver: true }),
          Animated.timing(pulseAnim, { toValue: 1, duration: 2000, useNativeDriver: true }),
        ])
      ).start();
      Animated.timing(glowAnim, { toValue: 1, duration: 600, useNativeDriver: true }).start();
    } else {
      pulseAnim.setValue(1);
      Animated.timing(glowAnim, { toValue: 0, duration: 400, useNativeDriver: true }).start();
    }
  }, [state]);

  const handlePress = () => {
    if (Platform.OS !== 'web') Haptics.impactAsync(Haptics.ImpactFeedbackStyle.Medium);
    onPress();
  };

  const spin = rotateAnim.interpolate({ inputRange: [0, 1], outputRange: ['0deg', '360deg'] });

  const orbColor =
    state === 'connected' ? colors.connected :
    state === 'connecting' ? colors.warning :
    colors.disconnected;

  const iconName =
    state === 'connected' ? 'shield-check' :
    state === 'connecting' ? 'shield-sync' :
    'shield-outline';

  const stateLabel =
    state === 'connected' ? 'Connected' :
    state === 'connecting' ? 'Connecting...' :
    'Disconnected';

  const s = styles(colors);

  return (
    <View style={s.wrapper}>
      <Animated.View
        style={[
          s.glow,
          {
            backgroundColor: orbColor + '18',
            opacity: glowAnim,
            transform: [{ scale: pulseAnim }],
          },
        ]}
      />
      <Animated.View
        style={[
          s.outerRing,
          { borderColor: orbColor + '55', transform: [{ scale: pulseAnim }] },
        ]}
      />
      <TouchableOpacity onPress={handlePress} activeOpacity={0.85} style={s.orbButton}>
        <View style={[s.orb, { backgroundColor: orbColor + '22', borderColor: orbColor }]}>
          <Animated.View style={{ transform: state === 'connecting' ? [{ rotate: spin }] : [] }}>
            <MaterialCommunityIcons name={iconName as any} size={52} color={orbColor} />
          </Animated.View>
        </View>
      </TouchableOpacity>
      <Text style={[s.stateLabel, { color: orbColor }]}>{stateLabel}</Text>
      {serverName && state !== 'disconnected' && (
        <Text style={s.serverName}>{serverName}</Text>
      )}
      {state === 'disconnected' && (
        <Text style={s.hint}>Tap to connect</Text>
      )}
    </View>
  );
}

const styles = (colors: ReturnType<typeof useColors>) =>
  StyleSheet.create({
    wrapper: {
      alignItems: 'center',
      justifyContent: 'center',
    },
    glow: {
      position: 'absolute',
      width: 200,
      height: 200,
      borderRadius: 100,
    },
    outerRing: {
      position: 'absolute',
      width: 160,
      height: 160,
      borderRadius: 80,
      borderWidth: 1,
    },
    orbButton: {
      width: 130,
      height: 130,
      borderRadius: 65,
      overflow: 'hidden',
    },
    orb: {
      flex: 1,
      borderRadius: 65,
      borderWidth: 2,
      alignItems: 'center',
      justifyContent: 'center',
    },
    stateLabel: {
      marginTop: 20,
      fontSize: 18,
      fontFamily: 'Inter_700Bold',
      letterSpacing: 0.5,
    },
    serverName: {
      marginTop: 6,
      color: colors.mutedForeground,
      fontSize: 13,
      fontFamily: 'Inter_400Regular',
    },
    hint: {
      marginTop: 6,
      color: colors.mutedForeground,
      fontSize: 13,
      fontFamily: 'Inter_400Regular',
    },
  });
