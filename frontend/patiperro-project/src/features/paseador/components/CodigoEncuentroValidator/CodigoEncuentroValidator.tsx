import { useEffect, useRef, useState } from "react";
import type { SolicitudPendientePaseador } from "../../types/solicitudPaseador.types";
import { validarCodigoEncuentroPaseador } from "../../services/solicitudesPaseadorService";
import styles from "./CodigoEncuentroValidator.module.css";

type Props = {
  solicitud: SolicitudPendientePaseador;
  onSuccess?: (solicitud: SolicitudPendientePaseador, startTime: string) => void;
};

const EMPTY_DIGITS = ["", "", "", ""];
const MAX_ATTEMPTS = 3;
const LOCK_SECONDS = 5 * 60;

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
      setAttempts(0);
      setError("");
    }
  }, [lockedRemainingSeconds, lockedUntil]);

  useEffect(() => {
    if (!isSuccess || !successStartTime || !onSuccess) return;
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

  function triggerError(message: string, penalize = true) {
    const nextAttempts = penalize ? attempts + 1 : attempts;
    setAttempts(nextAttempts);
    setError(message);
    setIsShaking(false);
    window.setTimeout(() => setIsShaking(true), 0);
    window.setTimeout(() => setIsShaking(false), 420);
    resetDigits();

    if (penalize && nextAttempts >= MAX_ATTEMPTS) {
      setLockedUntil(Date.now() + LOCK_SECONDS * 1000);
      setError("Demasiados intentos. El ingreso queda bloqueado por 5 minutos.");
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
      triggerError("Ingresa los 4 digitos del codigo.", false);
      return;
    }
    if (!/^\d{4}$/.test(code)) {
      triggerError("El codigo debe tener 4 digitos.", false);
      return;
    }

    setIsValidating(true);
    try {
      await validarCodigoEncuentroPaseador(solicitud.idReserva, code);
      const startTime = new Date().toISOString();
      setError("");
      setSuccessStartTime(startTime);
      setIsSuccess(true);
    } catch (e) {
      const msg = e instanceof Error ? e.message : "Codigo incorrecto, intentalo nuevamente";
      triggerError(msg, true);
    } finally {
      setIsValidating(false);
    }
  }

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
            La reserva quedo lista para cambiar a EN_CURSO cuando el backend confirme la
            validacion real.
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
            <strong>{formatTime(successStartTime ?? solicitud.horaInicio)}</strong>
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
