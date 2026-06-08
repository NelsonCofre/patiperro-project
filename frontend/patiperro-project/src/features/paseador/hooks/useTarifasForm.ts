// Formulario de configuración de servicio (radio + tarifas) con carga y guardado vía API.
// GET /public/tamanos sin exigir sesión; GET/PUT /me/configuracion con credentials (JWT).
import { useCallback, useEffect, useMemo, useState } from "react";
import {
  fetchPublicTamanos,
  getMyConfiguracion,
  putMyConfiguracion,
  type ConfiguracionPaseadorDTO,
  type TamanoPublicDTO,
  type TarifaConfiguracionDTO
} from "../services/paseadorConfigService";
import type { TarifasErrors, TarifasForm } from "../types/tarifas.types";
import {
  buildUpsertConfiguracionBody,
  countTarifasOfrecidas,
  normalizeTamanoId,
  parsePrecioPorHora,
  updateTarifaItem,
  validateConfigForm,
  validateTarifaValue
} from "../utils/tarifasValidators";

function buildFormFromApi(
  tamanos: TamanoPublicDTO[],
  config: ConfiguracionPaseadorDTO
): TarifasForm {
  const byTamano = new Map<number, TarifaConfiguracionDTO>();
  for (const tarifa of config.tarifas ?? []) {
    const tamanoId = normalizeTamanoId(tarifa.tamanoId);
    if (tamanoId > 0) {
      byTamano.set(tamanoId, tarifa);
    }
  }

  const radio =
    config.radioCoberturaKm != null && !Number.isNaN(Number(config.radioCoberturaKm))
      ? String(config.radioCoberturaKm)
      : "";

  return {
    radioCoberturaKm: radio,
    tarifas: tamanos.map((t) => {
      const tamanoId = normalizeTamanoId(t.id);
      const ex = byTamano.get(tamanoId);
      const precio = ex ? parsePrecioPorHora(ex.precioPorHora) : null;
      return {
        tarifaId: null,
        configuracionId: config.configuracionId,
        tamanoId,
        tamanoNombre: t.nombre,
        descripcion: t.descripcion ?? "",
        enabled: precio != null,
        precioBase: precio != null ? String(precio) : ""
      };
    })
  };
}

const EMPTY_FORM: TarifasForm = { radioCoberturaKm: "", tarifas: [] };

export function useTarifasForm() {
  const [form, setForm] = useState<TarifasForm>(EMPTY_FORM);
  const [errors, setErrors] = useState<TarifasErrors>({});
  const [radioError, setRadioError] = useState<string | undefined>(undefined);
  const [successMessage, setSuccessMessage] = useState("");
  const [loadError, setLoadError] = useState<string | null>(null);
  const [submitError, setSubmitError] = useState<string | null>(null);
  const [loadStatus, setLoadStatus] = useState<"loading" | "ready">("loading");
  const [isSubmitting, setIsSubmitting] = useState(false);

  const load = useCallback(async () => {
    setLoadStatus("loading");
    setLoadError(null);
    try {
      const tamanos = await fetchPublicTamanos();
      if (tamanos.length === 0) {
        setForm(EMPTY_FORM);
        setLoadError(
          "No hay tamaños en el catálogo. Pobla la tabla tamano en la base de datos del paseadores-service y vuelve a cargar esta página."
        );
        setLoadStatus("ready");
        return;
      }
      const config = await getMyConfiguracion();
      setForm(buildFormFromApi(tamanos, config));
    } catch (e) {
      setForm(EMPTY_FORM);
      setLoadError(e instanceof Error ? e.message : "No se pudo cargar la configuración.");
    } finally {
      setLoadStatus("ready");
    }
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  const validate = (nextForm: TarifasForm = form) => {
    const { radio, tarifas } = validateConfigForm(nextForm);
    const cleaned = Object.fromEntries(
      Object.entries(tarifas).filter(([, value]) => Boolean(value))
    ) as TarifasErrors;
    return { radio, tarifas: cleaned };
  };

  const updateFieldError = (tamanoId: number, enabled: boolean, precioBase: string) => {
    const nextError = validateTarifaValue(enabled, precioBase);
    setErrors((prev) => ({
      ...prev,
      [tamanoId]: nextError
    }));
  };

  const updateRadio = (value: string) => {
    setForm((prev) => ({ ...prev, radioCoberturaKm: value }));
    setRadioError(undefined);
  };

  const updatePrice = (tamanoId: number, nextPrice: string) => {
    setForm((prev) => {
      const nextTarifas = updateTarifaItem(prev.tarifas, tamanoId, (item) => ({
        ...item,
        precioBase: nextPrice,
        enabled: nextPrice.trim() ? true : item.enabled
      }));
      const updatedItem = nextTarifas.find((item) => item.tamanoId === tamanoId);
      if (updatedItem) {
        updateFieldError(tamanoId, updatedItem.enabled, updatedItem.precioBase);
      }
      return { ...prev, tarifas: nextTarifas };
    });
  };

  const toggleTarifa = (tamanoId: number, enabled: boolean) => {
    setForm((prev) => {
      const nextTarifas = updateTarifaItem(prev.tarifas, tamanoId, (item) => ({
        ...item,
        enabled,
        precioBase: enabled ? item.precioBase : ""
      }));
      const updatedItem = nextTarifas.find((item) => item.tamanoId === tamanoId);
      if (updatedItem) {
        updateFieldError(tamanoId, updatedItem.enabled, updatedItem.precioBase);
      }
      return { ...prev, tarifas: nextTarifas };
    });
  };

  const handleFieldBlur = (tamanoId: number) => {
    const currentItem = form.tarifas.find((item) => item.tamanoId === tamanoId);
    if (!currentItem) return;
    updateFieldError(tamanoId, currentItem.enabled, currentItem.precioBase);
  };

  const handleRadioBlur = () => {
    const { radio } = validateConfigForm(form);
    setRadioError(radio);
  };

  const currentValidation = useMemo(() => {
    const { radio, tarifas } = validateConfigForm(form);
    const cleaned = Object.fromEntries(
      Object.entries(tarifas).filter(([, value]) => Boolean(value))
    ) as TarifasErrors;
    return { radio, tarifas: cleaned };
  }, [form]);

  const tarifasOfrecidasCount = useMemo(() => countTarifasOfrecidas(form), [form]);

  const isSubmitDisabled =
    loadStatus !== "ready" ||
    Boolean(loadError) ||
    form.tarifas.length === 0 ||
    isSubmitting ||
    Boolean(currentValidation.radio) ||
    Object.keys(currentValidation.tarifas).length > 0;

  const handleSubmit = async () => {
    const validationErrors = validate(form);
    setErrors(validationErrors.tarifas);
    setRadioError(validationErrors.radio);
    setSuccessMessage("");
    setSubmitError(null);

    if (validationErrors.radio || Object.keys(validationErrors.tarifas).length > 0) {
      return;
    }

    setIsSubmitting(true);
    try {
      const body = buildUpsertConfiguracionBody(form);
      await putMyConfiguracion(body);
      setSuccessMessage("Configuración guardada correctamente.");
      await load();
    } catch (e) {
      setSubmitError(e instanceof Error ? e.message : "Error al guardar.");
    } finally {
      setIsSubmitting(false);
    }
  };

  return {
    form,
    errors,
    radioError,
    successMessage,
    loadError,
    submitError,
    loadStatus,
    isSubmitting,
    isSubmitDisabled,
    tarifasOfrecidasCount,
    setSuccessMessage,
    reload: load,
    updateRadio,
    updatePrice,
    toggleTarifa,
    handleFieldBlur,
    handleRadioBlur,
    handleSubmit
  };
}
