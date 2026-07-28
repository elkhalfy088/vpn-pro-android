import React, { createContext, useContext, useState, useCallback } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { useActivateServer, getGetServersQueryKey, getGetActiveServerQueryKey } from '@workspace/api-client-react';
import type { Server } from '@workspace/api-client-react';
import * as Haptics from 'expo-haptics';

interface VpnContextValue {
  connectingId: number | null;
  connect: (server: Server) => Promise<void>;
  disconnect: () => void;
}

const VpnContext = createContext<VpnContextValue | null>(null);

export function VpnProvider({ children }: { children: React.ReactNode }) {
  const [connectingId, setConnectingId] = useState<number | null>(null);
  const queryClient = useQueryClient();
  const activateMutation = useActivateServer();

  const connect = useCallback(async (server: Server) => {
    setConnectingId(server.id);
    await Haptics.impactAsync(Haptics.ImpactFeedbackStyle.Medium);
    try {
      await activateMutation.mutateAsync({ id: server.id });
      queryClient.invalidateQueries({ queryKey: getGetServersQueryKey() });
      queryClient.invalidateQueries({ queryKey: getGetActiveServerQueryKey() });
      await Haptics.notificationAsync(Haptics.NotificationFeedbackType.Success);
    } catch {
      await Haptics.notificationAsync(Haptics.NotificationFeedbackType.Error);
    } finally {
      setConnectingId(null);
    }
  }, [activateMutation, queryClient]);

  const disconnect = useCallback(() => {
    // No true disconnect in shared server list mode — just UI reset
    queryClient.invalidateQueries({ queryKey: getGetServersQueryKey() });
    queryClient.invalidateQueries({ queryKey: getGetActiveServerQueryKey() });
    Haptics.impactAsync(Haptics.ImpactFeedbackStyle.Light);
  }, [queryClient]);

  return (
    <VpnContext.Provider value={{ connectingId, connect, disconnect }}>
      {children}
    </VpnContext.Provider>
  );
}

export function useVpn() {
  const ctx = useContext(VpnContext);
  if (!ctx) throw new Error('useVpn must be used within VpnProvider');
  return ctx;
}
