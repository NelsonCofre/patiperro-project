import type { ProxyOptions } from "vite";
import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

const gatewayTarget = process.env.VITE_GATEWAY_PROXY_TARGET || "http://127.0.0.1:8080";
/** WebSocket STOMP del chat: directo a chat-service (el gateway WebMVC no proxifica bien SockJS/WS). */
const chatWsTarget = process.env.VITE_CHAT_WS_PROXY_TARGET || "http://127.0.0.1:8089";

function withNgrokSkipHeader(proxy: ProxyOptions, target: string): ProxyOptions {
  if (!/ngrok-free\.(dev|app)|\.ngrok\.io/i.test(target)) {
    return proxy;
  }
  return {
    ...proxy,
    configure: (instance) => {
      instance.on("proxyReq", (proxyReq) => {
        proxyReq.setHeader("ngrok-skip-browser-warning", "true");
      });
    }
  };
}

const apiProxy: ProxyOptions = withNgrokSkipHeader(
  { target: gatewayTarget, changeOrigin: true },
  gatewayTarget
);

const chatWsProxy: ProxyOptions = withNgrokSkipHeader(
  { target: chatWsTarget, changeOrigin: true, ws: true },
  chatWsTarget
);

/** Nominatim exige User-Agent identificable; el navegador no siempre cumple la política OSM. */
const nominatimProxy: ProxyOptions = {
  target: "https://nominatim.openstreetmap.org",
  changeOrigin: true,
  rewrite: (path) => path.replace(/^\/geo\/nominatim/, ""),
  configure: (proxy) => {
    proxy.on("proxyReq", (proxyReq) => {
      proxyReq.setHeader(
        "User-Agent",
        "PatiperroDev/1.0 (desarrollo local; geocoding vía proxy Vite)"
      );
    });
  }
};

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  // sockjs-client (chat) referencia `global` como en Node; en el navegador no existe.
  define: {
    global: "globalThis"
  },
  server: {
    // Cloudflare Quick Tunnel / otros hosts dinámicos (retorno MP, acceso móvil).
    allowedHosts: true,
    // En dev las imágenes van por mismo origen (sin cabecera ngrok en <img>).
    proxy: {
      "/api": apiProxy,
      "/ws/chat": chatWsProxy,
      "/geo/nominatim": nominatimProxy
    }
  }
});
