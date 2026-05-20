import { useCallback, useEffect, useState } from "react";

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

export function usePushNotifications() {
  const [permission, setPermission] = useState<PushPermissionState>(() => readPermission());

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

  const requestPermission = useCallback(
    async (trigger: PushPromptTrigger): Promise<PushPermissionState> => {
      const currentPermission = readPermission();
      setPermission(currentPermission);

      if (currentPermission === "unsupported" || currentPermission !== "default") {
        return currentPermission;
      }

      if (wasPromptedThisSession(trigger)) {
        return currentPermission;
      }

      markPromptedThisSession(trigger);

      try {
        const nextPermission = await Notification.requestPermission();
        setPermission(nextPermission);
        return nextPermission;
      } catch (error) {
        console.warn("No se pudo solicitar el permiso de notificaciones.", error);
        return currentPermission;
      }
    },
    []
  );

  return {
    permission,
    isSupported: permission !== "unsupported",
    canRequestPermission: permission === "default",
    requestPermission
  };
}
