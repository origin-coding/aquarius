import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { createRouter, RouterProvider } from "@tanstack/react-router";
import { ConfigProvider } from "antd";
import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import { useTranslation } from "react-i18next";

import "antd/dist/reset.css";
import "virtual:uno.css";

import "@/i18n";
import { getAntdLocale } from "@/i18n/antdLocale";
import { routeTree } from "@/routeTree.gen";

const queryClient = new QueryClient();

const router = createRouter({
  routeTree,
  defaultPreload: "intent",
});

declare module "@tanstack/react-router" {
  interface Register {
    router: typeof router;
  }
}

function AppProviders() {
  const { i18n } = useTranslation();

  return (
    <QueryClientProvider client={queryClient}>
      <ConfigProvider locale={getAntdLocale(i18n.resolvedLanguage ?? i18n.language)}>
        <RouterProvider router={router} />
      </ConfigProvider>
    </QueryClientProvider>
  );
}

createRoot(document.getElementById("root")!).render(
  <StrictMode>
    <AppProviders />
  </StrictMode>,
);
