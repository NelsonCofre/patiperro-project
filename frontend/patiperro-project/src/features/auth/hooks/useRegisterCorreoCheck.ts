import { useCallback, useEffect, useRef, useState } from "react";
import {
  fetchCorreoDisponiblePaseador,
  fetchCorreoDisponibleTutor
} from "../services/authServices";
import { isValidEmail } from "../utils/validators";

type RegisterRole = "tutor" | "paseador";

const DEBOUNCE_MS = 450;

/** Verifica en backend si el correo ya existe (tutor o paseador) mientras el usuario escribe. */
export function useRegisterCorreoCheck(correo: string, role: RegisterRole) {
  const [correoRemotoError, setCorreoRemotoError] = useState<string | undefined>();
  const [checkingCorreo, setCheckingCorreo] = useState(false);
  const requestSeq = useRef(0);

  const runCheck = useCallback(
    async (email: string): Promise<boolean> => {
      const trimmed = email.trim();
      if (!trimmed || !isValidEmail(trimmed)) {
        setCorreoRemotoError(undefined);
        setCheckingCorreo(false);
        return true;
      }

      const seq = ++requestSeq.current;
      setCheckingCorreo(true);
      try {
        const result =
          role === "tutor"
            ? await fetchCorreoDisponibleTutor(trimmed)
            : await fetchCorreoDisponiblePaseador(trimmed);
        if (seq !== requestSeq.current) {
          return true;
        }
        const disponible = result.disponible === true;
        setCorreoRemotoError(
          disponible ? undefined : result.mensaje ?? "El correo ya está registrado"
        );
        return disponible;
      } catch {
        if (seq !== requestSeq.current) {
          return true;
        }
        setCorreoRemotoError(undefined);
        return true;
      } finally {
        if (seq === requestSeq.current) {
          setCheckingCorreo(false);
        }
      }
    },
    [role]
  );

  useEffect(() => {
    const id = window.setTimeout(() => {
      void runCheck(correo);
    }, DEBOUNCE_MS);
    return () => window.clearTimeout(id);
  }, [correo, runCheck]);

  const checkCorreoNow = useCallback(() => {
    void runCheck(correo);
  }, [correo, runCheck]);

  const verifyCorreoNow = useCallback(async () => runCheck(correo), [correo, runCheck]);

  return { correoRemotoError, checkingCorreo, checkCorreoNow, verifyCorreoNow };
}
