// Hook reutilizable para el registro por pasos.
// Centraliza estado, errores, validacion por paso y preview de foto.
import { useCallback, useEffect, useMemo, useState } from "react";
import type {
  BaseRegisterForm,
  RegisterFormErrors
} from "../types/register.types";

type UseRegisterFormParams<TForm extends BaseRegisterForm> = {
  initialForm: TForm;
  validateField: (
    name: keyof TForm,
    value: string | File | null,
    form: TForm
  ) => string | undefined;
};

export function useRegisterForm<TForm extends BaseRegisterForm>({
  initialForm,
  validateField
}: UseRegisterFormParams<TForm>) {
  const [currentStep, setCurrentStep] = useState(0);
  const [form, setForm] = useState<TForm>(initialForm);
  const [errors, setErrors] = useState<RegisterFormErrors<TForm>>({});
  const [submitError, setSubmitError] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);

  const photoPreview = useMemo(() => {
    if (!form.foto_perfil) {
      return "";
    }

    return URL.createObjectURL(form.foto_perfil);
  }, [form.foto_perfil]);

  useEffect(() => {
    return () => {
      if (photoPreview) {
        URL.revokeObjectURL(photoPreview);
      }
    };
  }, [photoPreview]);

  const setFieldValue = <K extends keyof TForm>(name: K, value: TForm[K]) => {
    setForm((prev) => ({
      ...prev,
      [name]: value
    }));

    setErrors((prev) => ({
      ...prev,
      [name]: undefined
    }));
  };

  const handleBlur = (
    event: React.FocusEvent<HTMLInputElement | HTMLTextAreaElement>
  ) => {
    const { name, value } = event.target;
    const nextError = validateField(name as keyof TForm, value, form);

    setErrors((prev) => ({
      ...prev,
      [name]: nextError
    }));
  };

  const handlePhotoChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0] ?? null;

    setForm((prev) => ({
      ...prev,
      foto_perfil: file
    }));

    setErrors((prev) => ({
      ...prev,
      foto_perfil: undefined
    }));
  };

  const validateStep = useCallback((step: number): RegisterFormErrors<TForm> => {
    const nextErrors: RegisterFormErrors<TForm> = {};

    if (step === 0) {
      nextErrors.correo = validateField("correo" as keyof TForm, form.correo, form);
      nextErrors.contrasena = validateField(
        "contrasena" as keyof TForm,
        form.contrasena,
        form
      );
      nextErrors.confirmar_contrasena = validateField(
        "confirmar_contrasena" as keyof TForm,
        form.confirmar_contrasena,
        form
      );
    }

    if (step === 1) {
      nextErrors.rut = validateField("rut" as keyof TForm, form.rut, form);
      nextErrors.primer_nombre = validateField(
        "primer_nombre" as keyof TForm,
        form.primer_nombre,
        form
      );
      nextErrors.segundo_nombre = validateField(
        "segundo_nombre" as keyof TForm,
        form.segundo_nombre,
        form
      );
      nextErrors.apellido_paterno = validateField(
        "apellido_paterno" as keyof TForm,
        form.apellido_paterno,
        form
      );
      nextErrors.apellido_materno = validateField(
        "apellido_materno" as keyof TForm,
        form.apellido_materno,
        form
      );
      nextErrors.fecha_nacimiento = validateField(
        "fecha_nacimiento" as keyof TForm,
        form.fecha_nacimiento,
        form
      );
      nextErrors.telefono = validateField(
        "telefono" as keyof TForm,
        form.telefono,
        form
      );
      nextErrors.biografia = validateField(
        "biografia" as keyof TForm,
        form.biografia,
        form
      );
    }

    if (step === 2) {
      nextErrors.pais = validateField("pais" as keyof TForm, form.pais, form);
      nextErrors.region = validateField("region" as keyof TForm, form.region, form);
      nextErrors.ciudad = validateField("ciudad" as keyof TForm, form.ciudad, form);
      nextErrors.calle = validateField("calle" as keyof TForm, form.calle, form);
      nextErrors.comuna = validateField("comuna" as keyof TForm, form.comuna, form);
      nextErrors.numeracion = validateField(
        "numeracion" as keyof TForm,
        form.numeracion,
        form
      );
      nextErrors.foto_perfil = validateField(
        "foto_perfil" as keyof TForm,
        form.foto_perfil,
        form
      );
    }

    return Object.fromEntries(
      Object.entries(nextErrors).filter(([, value]) => Boolean(value))
    ) as RegisterFormErrors<TForm>;
  }, [form, validateField]);

  const currentStepErrors = useMemo(() => validateStep(currentStep), [currentStep, validateStep]);
  const isCurrentStepDisabled =
    isSubmitting || Object.keys(currentStepErrors).length > 0;

  const handleNextStep = () => {
    const validationErrors = validateStep(currentStep);
    setErrors(validationErrors);
    setSubmitError("");

    if (Object.keys(validationErrors).length > 0) {
      return;
    }

    setCurrentStep((prev) => prev + 1);
  };

  const handlePrevStep = () => {
    setErrors({});
    setSubmitError("");
    setCurrentStep((prev) => prev - 1);
  };

  return {
    currentStep,
    form,
    errors,
    photoPreview,
    submitError,
    isSubmitting,
    setSubmitError,
    setIsSubmitting,
    setFieldValue,
    handleBlur,
    handlePhotoChange,
    validateStep,
    handleNextStep,
    handlePrevStep,
    isCurrentStepDisabled
  };
}
