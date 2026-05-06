import { initMercadoPago, Payment } from "@mercadopago/sdk-react";
import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { getMercadoPagoPublicKey } from "../../../../config/mercadopago";
import type { BrickPagoRequestBody } from "../../services/pagoTutorApi";
import { procesarPagoBrick } from "../../services/pagoTutorApi";
import styles from "./MercadoPagoReservaBricks.module.css";

type Props = {
  idReserva: number;
  amountClp: number;
  onPagoFinalizado?: (resultado: { ok: boolean; mensaje?: string; mpStatus?: string }) => void;
};

let lastInitializedPublicKey: string | null = null;

function ensureInitMercadoPago(publicKey: string) {
  const k = publicKey.trim();
  if (!k) return;
  if (lastInitializedPublicKey === k) return;
  initMercadoPago(k, { locale: "es-CL" });
  lastInitializedPublicKey = k;
}

function asRecord(value: unknown): Record<string, unknown> {
  return value && typeof value === "object" ? (value as Record<string, unknown>) : {};
}

function firstNonEmptyString(...values: unknown[]): string {
  for (const v of values) {
    if (typeof v === "string" && v.trim() !== "") {
      return v.trim();
    }
  }
  return "";
}

function mapBrickSubmit(
  idReserva: number,
  param: unknown,
  additionalData?: unknown
): BrickPagoRequestBody {
  const p = asRecord(param);
  const formData = asRecord(p.formData);
  const payer = asRecord(formData.payer ?? p.payer);
  const identification = (payer.identification ?? {}) as Record<string, unknown>;
  const ad = asRecord(additionalData);

  const paymentMethodId = firstNonEmptyString(
    formData.payment_method_id,
    formData.paymentMethodId,
    ad.payment_method_id,
    ad.paymentMethodId,
    p.paymentMethodId,
    p.payment_method_id
  );

  const token = firstNonEmptyString(formData.token, p.token);
  const installmentsNum = Number(formData.installments ?? p.installments ?? 1);

  const issuerRaw =
    (formData.issuer_id as string | undefined) ??
    (p.issuerId as string | undefined) ??
    (p.issuer_id as string | undefined);
  const issuerId =
    issuerRaw !== undefined && issuerRaw !== null && String(issuerRaw).trim() !== ""
      ? String(issuerRaw).trim()
      : undefined;

  const email = firstNonEmptyString(payer.email, formData.email, p.email);

  const idType =
    (identification.type as string | undefined) ??
    (p.identificationType as string | undefined);
  const idNumber =
    (identification.number as string | undefined) ??
    (p.identificationNumber as string | undefined);

  const identificationType =
    idType !== undefined && String(idType).trim() !== "" ? String(idType).trim() : undefined;
  const identificationNumber =
    idNumber !== undefined && String(idNumber).trim() !== "" ? String(idNumber).trim() : undefined;

  return {
    idReserva,
    token,
    paymentMethodId,
    installments: Number.isFinite(installmentsNum) && installmentsNum > 0 ? installmentsNum : 1,
    issuerId,
    payerEmail: email.trim(),
    identificationType,
    identificationNumber
  };
}

function validarPayloadBrick(payload: BrickPagoRequestBody): string | null {
  if (!payload.token.trim()) {
    return "Mercado Pago no devolvió el token del medio de pago. Vuelve a seleccionar la tarjeta e inténtalo de nuevo.";
  }
  if (!payload.paymentMethodId.trim()) {
    return "No se pudo identificar el medio de pago seleccionado.";
  }
  if (!payload.payerEmail.trim()) {
    return "Debes ingresar un correo del pagador para continuar.";
  }
  return null;
}

/**
 * Checkout API embebido: Payment Brick sin preferencia; el cobro se confirma en pagos-service con POST /v1/payments.
 */
export default function MercadoPagoReservaBricks({ idReserva, amountClp, onPagoFinalizado }: Props) {
  const publicKey = getMercadoPagoPublicKey();
  const [sdkReady, setSdkReady] = useState(false);
  const [procesando, setProcesando] = useState(false);
  const mounted = useRef(true);
  const onPagoFinalizadoRef = useRef(onPagoFinalizado);
  onPagoFinalizadoRef.current = onPagoFinalizado;

  /** El Payment de sdk-react depende por referencia de `initialization`/`customization`; si son objetos nuevos cada render, desmonta y remonta el brick (UI duplicada, errores SVG). */
  const initialization = useMemo(() => ({ amount: amountClp }), [amountClp]);

  const customization = useMemo(
    () =>
      ({
        visual: { style: { theme: "flat" as const } },
        paymentMethods: {
          creditCard: "all",
          debitCard: "all",
          prepaidCard: "all"
        }
      }) as const,
    []
  );

  const brickContainerId = useMemo(() => `paymentBrick_reserva_${idReserva}`, [idReserva]);

  const handleSubmit = useCallback(
    async (param: unknown, additionalData?: unknown) => {
      setProcesando(true);
      try {
        const body = mapBrickSubmit(idReserva, param, additionalData);
        const validationError = validarPayloadBrick(body);
        if (validationError) {
          onPagoFinalizadoRef.current?.({ ok: false, mensaje: validationError });
          return;
        }
        const resultado = await procesarPagoBrick(body);
        const st = resultado.mpStatus.toLowerCase();
        const ok =
          st === "approved" || st === "pending" || st === "in_process" || st === "authorized";
        if (!ok) {
          onPagoFinalizadoRef.current?.({
            ok: false,
            mpStatus: resultado.mpStatus,
            mensaje:
              resultado.mpStatusDetail ||
              resultado.mpStatus ||
              "El pago no fue aprobado. Intenta con otro medio o revisa los datos."
          });
          return;
        }
        onPagoFinalizadoRef.current?.({ ok: true, mpStatus: resultado.mpStatus, mensaje: "" });
      } catch (e) {
        const msg = e instanceof Error ? e.message : "No se pudo procesar el pago.";
        onPagoFinalizadoRef.current?.({ ok: false, mensaje: msg });
      } finally {
        setProcesando(false);
      }
    },
    [idReserva]
  );

  const handleError = useCallback((detail: unknown) => {
    const msg =
      detail && typeof detail === "object" && "message" in detail && typeof detail.message === "string"
        ? detail.message
        : "Error en el formulario de Mercado Pago.";
    onPagoFinalizadoRef.current?.({ ok: false, mensaje: msg });
  }, []);

  useEffect(() => {
    mounted.current = true;
    if (!publicKey) {
      setSdkReady(false);
      return () => {
        mounted.current = false;
      };
    }
    try {
      ensureInitMercadoPago(publicKey);
      if (mounted.current) setSdkReady(true);
    } catch {
      setSdkReady(false);
    }
    return () => {
      mounted.current = false;
    };
  }, [publicKey]);

  if (!publicKey) {
    return (
      <div className={styles.fallback}>
        <p>
          Configura la Public Key en <code>src/config/mercadopago.ts</code> para usar el formulario de pago embebido.
        </p>
      </div>
    );
  }

  if (!sdkReady) {
    return <p className={styles.loading}>Cargando medios de pago…</p>;
  }

  return (
    <div className={styles.wrap}>
      <p className={styles.hint}>Completa tus datos y confirma el pago (Checkout API, sin redirección a checkout Pro).</p>
      <div className={styles.brick}>
        <Payment
          id={brickContainerId}
          initialization={initialization}
          customization={customization}
          onSubmit={handleSubmit}
          onError={handleError}
        />
      </div>
      {procesando ? <p className={styles.loading}>Enviando pago al servidor…</p> : null}
    </div>
  );
}
