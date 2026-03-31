// Hook del formulario de tarifas del paseador.
// Maneja tarifas por tamano usando una estructura cercana al modelo tarifa_paseador.
import { useMemo, useState } from "react";
import type { TarifasErrors, TarifasForm } from "../types/tarifas.types";
import {
  buildTarifaPaseadorPayload,
  updateTarifaItem,
  validateTarifaValue,
  validateTarifasForm
} from "../utils/tarifasValidators";

type UseTarifasFormParams = {
  initialForm: TarifasForm;
};

export function useTarifasForm({ initialForm }: UseTarifasFormParams) {
  const [form, setForm] = useState<TarifasForm>(initialForm);
  const [errors, setErrors] = useState<TarifasErrors>({});
  const [successMessage, setSuccessMessage] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);

  const validate = (nextForm: TarifasForm = form) => {
    const nextErrors = validateTarifasForm(nextForm);
    const cleaned = Object.fromEntries(
      Object.entries(nextErrors).filter(([, value]) => Boolean(value))
    ) as TarifasErrors;

    return cleaned;
  };

  const updateFieldError = (tamanoId: number, enabled: boolean, precioBase: string) => {
    const nextError = validateTarifaValue(enabled, precioBase);

    setErrors((prev) => ({
      ...prev,
      [tamanoId]: nextError
    }));
  };

  const updatePrice = (tamanoId: number, nextPrice: string) => {
    setForm((prev) => {
      const nextTarifas = updateTarifaItem(prev.tarifas, tamanoId, (item) => ({
        ...item,
        precioBase: nextPrice
      }));

      const updatedItem = nextTarifas.find((item) => item.tamanoId === tamanoId);

      if (updatedItem) {
        updateFieldError(tamanoId, updatedItem.enabled, updatedItem.precioBase);
      }

      return {
        ...prev,
        tarifas: nextTarifas
      };
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

      return {
        ...prev,
        tarifas: nextTarifas
      };
    });
  };

  const handleFieldBlur = (tamanoId: number) => {
    const currentItem = form.tarifas.find((item) => item.tamanoId === tamanoId);

    if (!currentItem) {
      return;
    }

    updateFieldError(tamanoId, currentItem.enabled, currentItem.precioBase);
  };

  const currentValidation = useMemo(() => validate(form), [form]);
  const isSubmitDisabled = isSubmitting || Object.keys(currentValidation).length > 0;

  const handleSubmit = () => {
    const validationErrors = validate(form);
    setErrors(validationErrors);
    setSuccessMessage("");

    if (Object.keys(validationErrors).length > 0) {
      return;
    }

    setIsSubmitting(true);

    window.setTimeout(() => {
      buildTarifaPaseadorPayload(form);
      setIsSubmitting(false);
      setSuccessMessage("Tarifas actualizadas correctamente");
    }, 600);
  };

  return {
    form,
    errors,
    successMessage,
    isSubmitting,
    isSubmitDisabled,
    setSuccessMessage,
    updatePrice,
    toggleTarifa,
    handleFieldBlur,
    handleSubmit
  };
}
