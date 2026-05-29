import path from "node:path";

import tanStackRouter from "@tanstack/router-plugin/vite";
import react from "@vitejs/plugin-react";
import UnoCSS from "unocss/vite";
import { defineConfig } from "vite";

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    tanStackRouter({
      target: "react",
      autoCodeSplitting: true,
    }),
    UnoCSS(),
    react(),
  ],
  resolve: {
    alias: {
      "@": path.resolve(__dirname, "src"),
    },
  },
  server: {
    proxy: {
      "/api": {
        target: "http://localhost:8080",
        changeOrigin: true,
        rewrite: (proxyPath) => proxyPath.replace(/^\/api/, ""),
      },
    },
  },
});
