import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useNavigate } from "@tanstack/react-router";
import { message } from "antd";

import { api } from "@/features/auth/apiClient";
import { clearAuthentication } from "@/features/auth/authSession";
import { authTokenStore, expiresAt, useAuthStore } from "@/features/auth/authStore";
import {
  loginWithPassword,
  type PasswordLoginRequest,
} from "@/features/auth/login/api";

export function usePasswordLoginMutation() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (request: PasswordLoginRequest) => loginWithPassword(request),

    onSuccess: async (result) => {
      await authTokenStore.setTokenState?.({
        accessToken: result.session.accessToken,
        refreshToken: result.session.refreshToken,
        accessTokenExpiresAt: expiresAt(result.session.expiresIn),
        refreshTokenExpiresAt: expiresAt(result.session.refreshExpiresIn),
      });

      useAuthStore.getState().setAuthenticated(result.user);
      await queryClient.invalidateQueries({ queryKey: ["auth"] });

      void navigate({ to: "/dashboard" });
    },

    onError: async (error) => {
      await message.error(error instanceof Error ? error.message : "auth.login_failed");
    },
  });
}

export function useLogoutMutation() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async () => {
      const accessToken = await authTokenStore.getAccessToken();
      if (!accessToken) {
        return;
      }

      await api.DELETE("/iam/auth/sessions/current", {
        params: {
          header: {
            Authorization: `Bearer ${accessToken}`,
          },
        },
      });
    },

    onSettled: async () => {
      await clearAuthentication();
      queryClient.clear();
      void navigate({ to: "/login" });
    },
  });
}
