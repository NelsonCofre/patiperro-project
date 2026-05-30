declare module "@stomp/stompjs" {
  export type StompFrame = {
    headers: Record<string, string>;
    body: string;
  };

  export type StompSubscription = {
    unsubscribe: () => void;
  };

  export type ClientOptions = {
    brokerURL?: string;
    webSocketFactory?: () => WebSocket;
    reconnectDelay?: number;
    heartbeatIncoming?: number;
    heartbeatOutgoing?: number;
    debug?: (message: string) => void;
    onStompError?: (frame: StompFrame) => void;
  };

  export class Client {
    constructor(options?: ClientOptions);
    connected: boolean;
    onConnect?: (() => void) | undefined;
    onDisconnect?: (() => void) | undefined;
    onWebSocketError?: ((event: unknown) => void) | undefined;
    activate(): void;
    deactivate(): void;
    publish(params: { destination: string; body: string }): void;
    subscribe(
      destination: string,
      callback: (message: StompFrame) => void
    ): StompSubscription;
  }
}
