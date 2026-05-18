import type { ProxyOptions } from "vite";
import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

const gatewayTarget = process.env.VITE_GATEWAY_PROXY_TARGET || "http://127.0.0.1:8080";

const apiProxy: ProxyOptions = {
  target: gatewayTarget,
  changeOrigin: true
};

if (/ngrok-free\.(dev|app)|\.ngrok\.io/i.test(gatewayTarget)) {
  apiProxy.configure = (proxy) => {
    proxy.on("proxyReq", (proxyReq) => {
      proxyReq.setHeader("ngrok-skip-browser-warning", "true");
    });
  };
}

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
  server: {
    // Cloudflare Quick Tunnel / otros hosts dinámicos (retorno MP, acceso móvil).
    allowedHosts: true,
    // En dev las imágenes van por mismo origen (sin cabecera ngrok en <img>).
    proxy: {
      "/api": apiProxy,
      "/geo/nominatim": nominatimProxy
    }
  }
});
