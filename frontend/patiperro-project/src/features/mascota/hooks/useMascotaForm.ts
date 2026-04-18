// Hook del formulario de mascota por pasos.
// Centraliza estado, errores, validacion y preview de foto.
import { useCallback, useEffect, useMemo, useState } from "react";
import type {
  MascotaForm,
  MascotaFormErrors
} from "../types/mascota.types";
import { validateMascotaField, validateMascotaPhoto } from "../utils/mascotaValidators";

type UseMascotaFormParams = {
  initialForm: MascotaForm;
};

export function useMascotaForm({ initialForm }: UseMascotaFormParams) {
  const [currentStep, setCurrentStep] = useState(0);
  const [form, setForm] = useState<MascotaForm>(initialForm);
  const [errors, setErrors] = useState<MascotaFormErrors>({});
  const [submitError, setSubmitError] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);

  const photoPreview = useMemo(() => {
    if (!form.foto) return "";
    return URL.createObjectURL(form.foto);
  }, [form.foto]);

  useEffect(() => {
    return () => {
      if (photoPreview) URL.revokeObjectURL(photoPreview);
    };
  }, [photoPreview]);

  const setFieldValue = <K extends keyof MascotaForm>(
    name: K,
    value: MascotaForm[K]
  ) => {
    setForm((prev) => ({ ...prev, [name]: value }));
    setErrors((prev) => ({ ...prev, [name]: undefined }));
  };

  const handleBlur = (
    event: React.FocusEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>
  ) => {
    const { name, value } = event.target;
    setErrors((prev) => ({
      ...prev,
      [name]: validateMascotaField(name as keyof MascotaForm, value, form)
    }));
  };

  const handlePhotoChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0] ?? null;
    setForm((prev) => ({ ...prev, foto: file }));
    setErrors((prev) => ({ ...prev, foto: validateMascotaPhoto(file) }));
  };

  const validateStep = useCallback(
    (step: number): MascotaFormErrors => {
      const nextErrors: MascotaFormErrors = {};

      if (step === 0) {
        nextErrors.nombre = validateMascotaField("nombre", form.nombre, form);
        nextErrors.especie = validateMascotaField("especie", form.especie, form);
        nextErrors.raza = validateMascotaField("raza", form.raza, form);
        nextErrors.sexo = validateMascotaField("sexo", form.sexo, form);
        nextErrors.fecha_nacimiento = validateMascotaField(
          "fecha_nacimiento",
          form.fecha_nacimiento,
          form
        );
      }

      if (step === 1) {
        nextErrors.peso = validateMascotaField("peso", form.peso, form);
        nextErrors.tamano = validateMascotaField("tamano", form.tamano, form);
        nextErrors.comportamiento = validateMascotaField(
          "comportamiento",
          form.comportamiento,
          form
        );
        nextErrors.descripcion = validateMascotaField(
          "descripcion",
          form.descripcion,
          form
        );
        nextErrors.cuidados_especiales = validateMascotaField(
          "cuidados_especiales",
          form.cuidados_especiales,
          form
        );
        nextErrors.esterilizado = validateMascotaField(
          "esterilizado",
          form.esterilizado,
          form
        );
        nextErrors.numero_chip = validateMascotaField(
          "numero_chip",
          form.numero_chip,
          form
        );
      }

      if (step === 2) {
        nextErrors.foto = validateMascotaField("foto", form.foto, form);
      }

      return Object.fromEntries(
        Object.entries(nextErrors).filter(([, value]) => Boolean(value))
      ) as MascotaFormErrors;
    },
    [form]
  );

  const currentStepErrors = useMemo(
    () => validateStep(currentStep),
    [currentStep, validateStep]
  );
  const isCurrentStepDisabled =
    isSubmitting || Object.keys(currentStepErrors).length > 0;

  const handleNextStep = () => {
    const validationErrors = validateStep(currentStep);
    setErrors(validationErrors);
    setSubmitError("");

    if (Object.keys(validationErrors).length > 0) return;
    setCurrentStep((prev) => prev + 1);
  };

  const handlePrevStep = () => {
    setErrors({});
    setSubmitError("");
    setCurrentStep((prev) => prev - 1);
  };

  const validateEntireForm = useCallback((): MascotaFormErrors => {
    const e0 = validateStep(0);
    const e1 = validateStep(1);
    const e2 = validateStep(2);
    return { ...e0, ...e1, ...e2 };
  }, [validateStep]);

  const resetForm = useCallback(() => {
    setForm(initialForm);
    setCurrentStep(0);
    setErrors({});
    setSubmitError("");
  }, [initialForm]);

  return {
    currentStep,
    form,
    errors,
    photoPreview,
    submitError,
    isSubmitting,
    setSubmitError,
    setIsSubmitting,
    setErrors,
    setFieldValue,
    handleBlur,
    handlePhotoChange,
    validateStep,
    validateEntireForm,
    handleNextStep,
    handlePrevStep,
    resetForm,
    isCurrentStepDisabled
  };
}
