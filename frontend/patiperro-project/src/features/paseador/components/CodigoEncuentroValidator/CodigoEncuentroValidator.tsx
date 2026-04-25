import { useEffect, useRef, useState } from "react";
import type { SolicitudPendientePaseador } from "../../types/solicitudPaseador.types";
import {
  obtenerEstadoEncuentroReserva,
  validarCodigoEncuentroPaseador
} from "../../services/solicitudesPaseadorService";
import styles from "./CodigoEncuentroValidator.module.css";

type Props = {
  solicitud: SolicitudPendientePaseador;
  onSuccess?: (solicitud: SolicitudPendientePaseador, startTime: string) => void;
};

const EMPTY_DIGITS = ["", "", "", ""];
const MAX_ATTEMPTS = 3;
const STATUS_REFRESH_MS = 10000;

function formatTime(value: string): string {
  return value || "--:--";
}

export default function CodigoEncuentroValidator({ solicitud, onSuccess }: Props) {
  const [digits, setDigits] = useState(EMPTY_DIGITS);
  const [attempts, setAttempts] = useState(0);
  const [error, setError] = useState("");
  const [isShaking, setIsShaking] = useState(false);
  const [isSuccess, setIsSuccess] = useState(false);
  const [isValidating, setIsValidating] = useState(false);
  const [lockedUntil, setLockedUntil] = useState<number | null>(null);
  const [now, setNow] = useState(() => Date.now());
  const inputsRef = useRef<Array<HTMLInputElement | null>>([]);
  const successNotifiedRef = useRef(false);

  const lockedRemainingSeconds =
    lockedUntil == null ? 0 : Math.max(0, Math.ceil((lockedUntil - now) / 1000));
  const isLocked = lockedRemainingSeconds > 0;
  const code = digits.join("");
  const isComplete = code.length === 4;
  const [successStartTime, setSuccessStartTime] = useState<string | null>(null);

  useEffect(() => {
    if (!lockedUntil) return;
    const timer = window.setInterval(() => setNow(Date.now()), 1000);
    return () => window.clearInterval(timer);
  }, [lockedUntil]);

  useEffect(() => {
    if (lockedUntil != null && lockedRemainingSeconds <= 0) {
      setLockedUntil(null);
      setError("");
    }
  }, [lockedRemainingSeconds, lockedUntil]);

  useEffect(() => {
    if (!isSuccess || !successStartTime || !onSuccess || successNotifiedRef.current) return;
    successNotifiedRef.current = true;
    const timer = window.setTimeout(() => {
      onSuccess(solicitud, successStartTime);
    }, 1600);
    return () => window.clearTimeout(timer);
  }, [isSuccess, onSuccess, solicitud, successStartTime]);

  function focusInput(index: number) {
    window.setTimeout(() => inputsRef.current[index]?.focus(), 0);
  }

  function resetDigits() {
    setDigits(EMPTY_DIGITS);
    focusInput(0);
  }

  function triggerError(message: string) {
    setError(message);
    setIsShaking(false);
    window.setTimeout(() => setIsShaking(true), 0);
    window.setTimeout(() => setIsShaking(false), 420);
    resetDigits();
  }

  async function refreshEstadoEncuentro(silent = false): Promise<void> {
    try {
      const estado = await obtenerEstadoEncuentroReserva(solicitud.idReserva);
      setAttempts(Math.max(0, estado.intentosFallidos || 0));
      setLockedUntil(estado.bloqueadoHasta ? new Date(estado.bloqueadoHasta).getTime() : null);

      if (estado.estadoEncuentro === "CONFIRMADO") {
        setError("");
        setSuccessStartTime(
          solicitud.fechaInicioReal ??
            (typeof solicitud.horaInicio === "string" ? solicitud.horaInicio : new Date().toISOString())
        );
        setIsSuccess(true);
        return;
      }

      if (!silent && estado.mensaje) {
        setError(estado.mensaje);
      }
    } catch (e) {
      if (!silent) {
        setError(
          e instanceof Error ? e.message : "No se pudo consultar el estado actual del encuentro."
        );
      }
    }
  }

  function handleDigitChange(index: number, value: string) {
    if (isLocked || isSuccess) return;
    const onlyDigits = value.replace(/\D/g, "");
    if (!onlyDigits) {
      setDigits((prev) => prev.map((digit, i) => (i === index ? "" : digit)));
      return;
    }

    if (onlyDigits.length > 1) {
      const next = [...EMPTY_DIGITS];
      onlyDigits.slice(0, 4).split("").forEach((digit, i) => {
        next[i] = digit;
      });
      setDigits(next);
      focusInput(Math.min(onlyDigits.length, 4) - 1);
      setError("");
      return;
    }

    setDigits((prev) => prev.map((digit, i) => (i === index ? onlyDigits : digit)));
    setError("");
    if (index < 3) focusInput(index + 1);
  }

  function handleKeyDown(index: number, key: string) {
    if (key === "Backspace" && !digits[index] && index > 0) {
      focusInput(index - 1);
    }
  }

  async function handleSubmit() {
    if (isLocked || isSuccess || isValidating) return;
    if (!isComplete) {
      triggerError("Ingresa los 4 digitos del codigo.");
      return;
    }
    if (!/^\d{4}$/.test(code)) {
      triggerError("El codigo debe tener 4 digitos.");
      return;
    }

    setIsValidating(true);
    try {
      const response = await validarCodigoEncuentroPaseador(solicitud.idReserva, code);
      setError("");
      setAttempts(0);
      setLockedUntil(null);
      setSuccessStartTime(
        response.fechaInicioReal ??
          solicitud.fechaInicioReal ??
          (typeof solicitud.horaInicio === "string" ? solicitud.horaInicio : new Date().toISOString())
      );
      setIsSuccess(true);
    } catch (e) {
      const msg = e instanceof Error ? e.message : "Codigo incorrecto, intentalo nuevamente";
      triggerError(msg);
      await refreshEstadoEncuentro(true);
    } finally {
      setIsValidating(false);
    }
  }

  useEffect(() => {
    void refreshEstadoEncuentro(true);
    const timer = window.setInterval(() => void refreshEstadoEncuentro(true), STATUS_REFRESH_MS);
    return () => window.clearInterval(timer);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [solicitud.idReserva]);

  useEffect(() => {
    if (!isLocked && !isSuccess && !isValidating && isComplete) {
      void handleSubmit();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [isComplete, code, isLocked, isSuccess, isValidating]);

  if (isSuccess) {
    return (
      <section className={styles.successCard} aria-live="polite">
        <div className={styles.checkWrap} aria-hidden="true">
          <svg viewBox="0 0 52 52" className={styles.checkIcon}>
            <circle cx="26" cy="26" r="24" />
            <path d="M15 27.5 22.5 35 38 18" />
          </svg>
        </div>
        <div>
          <p className={styles.eyebrow}>Encuentro confirmado</p>
          <h3>El paseo comenzo correctamente</h3>
          <p className={styles.successText}>
            El backend confirmo el encuentro y la reserva ya quedo en estado EN_CURSO.
          </p>
        </div>
        <div className={styles.summaryGrid}>
          <div>
            <span>Reserva</span>
            <strong>#{solicitud.idReserva}</strong>
          </div>
          <div>
            <span>Mascota</span>
            <strong>{solicitud.mascotaNombre}</strong>
          </div>
          <div>
            <span>Tutor</span>
            <strong>{solicitud.tutorNombre}</strong>
          </div>
          <div>
            <span>Hora programada</span>
            <strong>{formatTime(successStartTime ?? solicitud.fechaInicioReal ?? solicitud.horaInicio)}</strong>
          </div>
        </div>
      </section>
    );
  }

  return (
    <section className={styles.card}>
      <div className={styles.header}>
        <div>
          <p className={styles.eyebrow}>Validar encuentro</p>
          <h3>Ingresa el codigo del tutor</h3>
        </div>
        <span className={styles.attempts}>
          {isLocked ? "Bloqueado" : `${Math.max(0, MAX_ATTEMPTS - attempts)} intentos`}
        </span>
      </div>

      <div className={`${styles.pinRow} ${isShaking ? styles.shake : ""}`}>
        {digits.map((digit, index) => (
          <input
            // El indice es estable: siempre son cuatro campos fijos.
            key={index}
            ref={(node) => {
              inputsRef.current[index] = node;
            }}
            value={digit}
            inputMode="numeric"
            maxLength={4}
            aria-label={`Digito ${index + 1}`}
            className={styles.pinInput}
            disabled={isLocked || isValidating}
            onChange={(event) => handleDigitChange(index, event.target.value)}
            onKeyDown={(event) => handleKeyDown(index, event.key)}
          />
        ))}
      </div>

      {error ? (
        <p className={styles.errorText} role="alert">
          {error}
        </p>
      ) : (
        <p className={styles.helper}>
          Pide al tutor que muestre el codigo en pantalla e ingresalo antes de iniciar el paseo.
        </p>
      )}

      {isLocked ? (
        <p className={styles.lockText}>
          Puedes volver a intentar en {Math.floor(lockedRemainingSeconds / 60)}:
          {String(lockedRemainingSeconds % 60).padStart(2, "0")}.
        </p>
      ) : null}

      <button type="button" className={styles.validateButton} onClick={() => void handleSubmit()} disabled={isLocked || isValidating}>
        {isValidating ? "Validando..." : "Validar codigo"}
      </button>
    </section>
  );
}
