import { useCallback, useEffect, useRef } from 'react';
import SockJS from 'sockjs-client';
import { Client, IMessage } from '@stomp/stompjs';

export interface WsEvent {
  type: string;
  data: { id: string; ownerId: string };
}

function getCurrentUserId(): string | null {
  try {
    const raw = localStorage.getItem('user');
    if (!raw) return null;
    const user = JSON.parse(raw) as { id?: string };
    return user.id ?? null;
  } catch {
    return null;
  }
}

/**
 * Hook that connects to the backend WebSocket via STOMP/SockJS
 * and calls `onEvent` whenever a CRUD event is received on the user's topic.
 */
export function useWebSocket(onEvent: (event: WsEvent) => void) {
  const clientRef = useRef<Client | null>(null);
  const onEventRef = useRef(onEvent);
  onEventRef.current = onEvent;

  const connect = useCallback(() => {
    const token = localStorage.getItem('token');
    const userId = getCurrentUserId();
    if (!token || !userId) return;

    const client = new Client({
      webSocketFactory: () => new SockJS('/ws'),
      connectHeaders: { Authorization: `Bearer ${token}` },
      reconnectDelay: 5000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      onConnect: () => {
        client.subscribe(`/topic/updates/${userId}`, (message: IMessage) => {
          try {
            const event: WsEvent = JSON.parse(message.body);
            onEventRef.current(event);
          } catch {
            // ignore malformed messages
          }
        });
      },
      onStompError: (frame: { headers: Record<string, string> }) => {
        console.error('[WS] STOMP error:', frame.headers['message']);
      },
      onWebSocketClose: () => {
        console.log('[WS] Connection closed, will reconnect...');
      },
    });

    client.activate();
    clientRef.current = client;
  }, []);

  useEffect(() => {
    connect();
    return () => {
      clientRef.current?.deactivate();
      clientRef.current = null;
    };
  }, [connect]);
}
