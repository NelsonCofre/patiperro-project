declare module "@stomp/stompjs" {
  export type StompFrame = {
    headers: Record<string, string>;
    body: string;
  };

  export type ClientOptions = {
    brokerURL?: string;
    reconnectDelay?: number;
    heartbeatIncoming?: number;
    heartbeatOutgoing?: number;
    onStompError?: (frame: StompFrame) => void;
  };

  export class Client {
    constructor(options?: ClientOptions);
    onConnect?: (() => void) | undefined;
    onWebSocketError?: (() => void) | undefined;
    subscribe(destination: string, callback: (message: StompFrame) => void): void;
    activate(): void;
    deactivate(): void;
  }
}
