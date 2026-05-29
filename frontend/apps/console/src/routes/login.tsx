import { createFileRoute, redirect } from "@tanstack/react-router";

import { bootstrapAuthState } from "@/features/auth/authSession";
import { useAuthStore } from "@/features/auth/authStore";
import { LoginPage } from "@/features/auth/login/LoginPage";

export const Route = createFileRoute("/login")({
  beforeLoad: async () => {
    await bootstrapAuthState();

    if (useAuthStore.getState().status === "authenticated") {
      throw redirect({
        to: "/dashboard",
      });
    }
  },
  component: LoginPage,
});
