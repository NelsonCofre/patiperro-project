import { useCallback, useEffect, useState } from "react";
import { ACCESS_TOKEN_SESSION_KEY } from "../../../config/api";
import {
  isPushSubscriptionSupported,
  syncPushSubscriptionWithBackend
} from "../services/pushApi";

type PushPromptTrigger = "login" | "chat-entry";

export type PushPermissionState = NotificationPermission | "unsupported";

const PROMPT_SESSION_KEY_PREFIX = "patiperro_push_permission_prompted";

function readPermission(): PushPermissionState {
  if (typeof window === "undefined" || !("Notification" in window)) {
    return "unsupported";
  }
  return Notification.permission;
}

function buildPromptSessionKey(trigger: PushPromptTrigger): string {
  return `${PROMPT_SESSION_KEY_PREFIX}_${trigger}`;
}

function wasPromptedThisSession(trigger: PushPromptTrigger): boolean {
  if (typeof window === "undefined") {
    return false;
  }
  return window.sessionStorage.getItem(buildPromptSessionKey(trigger)) === "1";
}

function markPromptedThisSession(trigger: PushPromptTrigger): void {
  if (typeof window === "undefined") {
    return;
  }
  window.sessionStorage.setItem(buildPromptSessionKey(trigger), "1");
}

function hasAuthSession(): boolean {
  return Boolean(sessionStorage.getItem(ACCESS_TOKEN_SESSION_KEY)?.trim());
}

export function usePushNotifications() {
  const [permission, setPermission] = useState<PushPermissionState>(() => readPermission());
  const [isSyncingSubscription, setIsSyncingSubscription] = useState(false);

  const syncSubscriptionIfGranted = useCallback(async (): Promise<boolean> => {
    if (!isPushSubscriptionSupported() || readPermission() !== "granted" || !hasAuthSession()) {
      return false;
    }
    setIsSyncingSubscription(true);
    try {
      return await syncPushSubscriptionWithBackend();
    } finally {
      setIsSyncingSubscription(false);
    }
  }, []);

  useEffect(() => {
    const syncPermission = () => setPermission(readPermission());

    syncPermission();

    if (typeof document !== "undefined") {
      document.addEventListener("visibilitychange", syncPermission);
    }
    if (typeof window !== "undefined") {
      window.addEventListener("focus", syncPermission);
    }

    return () => {
      if (typeof document !== "undefined") {
        document.removeEventListener("visibilitychange", syncPermission);
      }
      if (typeof window !== "undefined") {
        window.removeEventListener("focus", syncPermission);
      }
    };
  }, []);

  useEffect(() => {
    if (readPermission() !== "granted" || !hasAuthSession()) {
      return;
    }
    void syncSubscriptionIfGranted();
  }, [syncSubscriptionIfGranted]);

  const requestPermission = useCallback(
    async (trigger: PushPromptTrigger): Promise<PushPermissionState> => {
      let currentPermission = readPermission();
      setPermission(currentPermission);

      if (currentPermission === "unsupported") {
        return currentPermission;
      }

      if (currentPermission === "default") {
        if (wasPromptedThisSession(trigger)) {
          return currentPermission;
        }

        markPromptedThisSession(trigger);

        try {
          currentPermission = await Notification.requestPermission();
          setPermission(currentPermission);
        } catch (error) {
          console.warn("No se pudo solicitar el permiso de notificaciones.", error);
          return currentPermission;
        }
      }

      if (currentPermission === "granted") {
        await syncSubscriptionIfGranted();
      }

      return currentPermission;
    },
    [syncSubscriptionIfGranted]
  );

  return {
    permission,
    isSupported: permission !== "unsupported" && isPushSubscriptionSupported(),
    canRequestPermission: permission === "default",
    isSyncingSubscription,
    requestPermission,
    syncSubscriptionIfGranted
  };
};
