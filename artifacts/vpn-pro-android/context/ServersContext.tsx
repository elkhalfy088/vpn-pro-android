import React, { createContext, useContext, useState } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import {
  useListServers,
  useGetActiveServer,
  useActivateServer,
  useDeleteServer,
  getListServersQueryKey,
  getGetActiveServerQueryKey,
} from '@workspace/api-client-react';
import type { VpnServer } from '@workspace/api-client-react';

type ConnectionState = 'disconnected' | 'connecting' | 'connected';

interface ServersContextValue {
  servers: VpnServer[];
  activeServer: VpnServer | null;
  connectionState: ConnectionState;
  isLoading: boolean;
  connect: (server: VpnServer) => Promise<void>;
  disconnect: () => void;
  deleteServer: (id: string) => Promise<void>;
  refetch: () => void;
}

const ServersContext = createContext<ServersContextValue | null>(null);

export function ServersProvider({ children }: { children: React.ReactNode }) {
  const queryClient = useQueryClient();
  const [connectionState, setConnectionState] = useState<ConnectionState>('disconnected');

  const { data: servers = [], isLoading: loadingServers, refetch } = useListServers();
  const { data: activeData } = useGetActiveServer();
  const { mutateAsync: activateServerMutation } = useActivateServer();
  const { mutateAsync: deleteServerMutation } = useDeleteServer();

  const activeServer = activeData?.server ?? null;

  const connect = async (server: VpnServer) => {
    try {
      setConnectionState('connecting');
      await activateServerMutation({ id: server.id });
      await queryClient.invalidateQueries({ queryKey: getListServersQueryKey() });
      await queryClient.invalidateQueries({ queryKey: getGetActiveServerQueryKey() });
      // Simulate handshake delay
      await new Promise(resolve => setTimeout(resolve, 1500));
      setConnectionState('connected');
    } catch {
      setConnectionState('disconnected');
    }
  };

  const disconnect = () => {
    setConnectionState('disconnected');
  };

  const deleteServer = async (id: string) => {
    await deleteServerMutation({ id });
    await queryClient.invalidateQueries({ queryKey: getListServersQueryKey() });
    await queryClient.invalidateQueries({ queryKey: getGetActiveServerQueryKey() });
    if (activeServer?.id === id) {
      setConnectionState('disconnected');
    }
  };

  return (
    <ServersContext.Provider
      value={{
        servers,
        activeServer,
        connectionState,
        isLoading: loadingServers,
        connect,
        disconnect,
        deleteServer,
        refetch,
      }}
    >
      {children}
    </ServersContext.Provider>
  );
}

export function useServers() {
  const ctx = useContext(ServersContext);
  if (!ctx) throw new Error('useServers must be used within ServersProvider');
  return ctx;
}
