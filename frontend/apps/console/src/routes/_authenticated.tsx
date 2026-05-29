import { createFileRoute, redirect } from "@tanstack/react-router";

import { bootstrapAuthState } from "@/features/auth/authSession";
import { useAuthStore } from "@/features/auth/authStore";
import { AuthenticatedLayout } from "@/features/shell/AuthenticatedLayout";

export const Route = createFileRoute("/_authenticated")({
  beforeLoad: async () => {
    await bootstrapAuthState();

    if (useAuthStore.getState().status !== "authenticated") {
      throw redirect({
        to: "/login",
      });
    }
  },
  component: AuthenticatedLayout,
});
