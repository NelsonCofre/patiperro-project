// Vista de configuracion del servicio del paseador.
// Permite definir tarifas por tamano usando una estructura preparada para backend.
import PaseadorNavbar from "../../components/PaseadorNavbar/PaseadorNavbar";
import { useTarifasForm } from "../../hooks/useTarifasForm";
import type { TarifasForm } from "../../types/tarifas.types";
import { keepOnlyMoneyDigits } from "../../utils/tarifasValidators";
import styles from "./PaseadorConfiguracion.module.css";

const INITIAL_FORM: TarifasForm = {
  tarifas: [
    {
      tarifaId: null,
      configuracionId: null,
      tamanoId: 1,
      tamanoNombre: "Pequeno",
      descripcion: "Ideal para mascotas de contextura liviana.",
      enabled: true,
      precioBase: ""
    },
    {
      tarifaId: null,
      configuracionId: null,
      tamanoId: 2,
      tamanoNombre: "Mediano",
      descripcion: "Configura el precio para paseos de tamano intermedio.",
      enabled: true,
      precioBase: ""
    },
    {
      tarifaId: null,
      configuracionId: null,
      tamanoId: 3,
      tamanoNombre: "Grande",
      descripcion: "Desactivalo si no deseas ofrecer este tamano.",
      enabled: false,
      precioBase: ""
    }
  ]
};

export default function PaseadorConfiguracion() {
  const {
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
  } = useTarifasForm({
    initialForm: INITIAL_FORM
  });

  return (
    <main className={styles.page}>
      {successMessage ? (
        <div className={styles.toastOverlay}>
          <div className={styles.toastCard} role="dialog" aria-modal="true">
            <span className={styles.toastEyebrow}>Configuracion guardada</span>
            <h2 className={styles.toastTitle}>Tarifas actualizadas correctamente</h2>
            <p className={styles.toastText}>
              El frontend ya quedo listo para integrarse con el backend y guardar
              estas tarifas usando tamano_id_tamano y precio_base.
            </p>
            <button
              type="button"
              className={styles.toastCloseButton}
              onClick={() => setSuccessMessage("")}
            >
              Cerrar
            </button>
          </div>
        </div>
      ) : null}

      <PaseadorNavbar />

      <section className={styles.hero}>
        <div>
          <p className={styles.eyebrow}>Configurar mi Servicio</p>
          <h1 className={styles.title}>Define tus tarifas por tamano</h1>
          <p className={styles.description}>
            Ajusta los precios de tu servicio para perros pequenos, medianos y
            grandes. Tambien puedes desactivar un tamano si no deseas ofrecerlo.
          </p>
        </div>

        <div className={styles.infoCard}>
          <span className={styles.infoLabel}>Estado</span>
          <strong>Configuracion inicial</strong>
        </div>
      </section>

      <section className={styles.formCard}>
        <div className={styles.sectionHeader}>
          <div>
            <p className={styles.cardEyebrow}>Tarifas por tamano</p>
            <h2>Prepara tu perfil comercial</h2>
          </div>
        </div>

        <div className={styles.tarifasGrid}>
          {form.tarifas.map((tarifa) => (
            <article key={tarifa.tamanoId} className={styles.tarifaItem}>
              <div className={styles.tarifaHeader}>
                <div>
                  <h3>{`Perro ${tarifa.tamanoNombre}`}</h3>
                  <p>{tarifa.descripcion}</p>
                </div>

                <label className={styles.toggleRow}>
                  <input
                    type="checkbox"
                    checked={tarifa.enabled}
                    onChange={(event) =>
                      toggleTarifa(tarifa.tamanoId, event.target.checked)
                    }
                  />
                  <span>{tarifa.enabled ? "Activo" : "No ofrecer"}</span>
                </label>
              </div>

              <label className={styles.inputGroup}>
                <span>Tarifa</span>
                <div className={styles.moneyInput}>
                  <span>$</span>
                  <input
                    value={tarifa.precioBase}
                    onChange={(event) =>
                      updatePrice(
                        tarifa.tamanoId,
                        keepOnlyMoneyDigits(event.target.value)
                      )
                    }
                    onBlur={() => handleFieldBlur(tarifa.tamanoId)}
                    placeholder="Ej: 7000"
                    inputMode="numeric"
                    disabled={!tarifa.enabled}
                  />
                </div>
              </label>

              {errors[tarifa.tamanoId] ? (
                <p className={styles.errorText}>{errors[tarifa.tamanoId]}</p>
              ) : (
                <p className={styles.helperText}>
                  {tarifa.enabled
                    ? "Ingresa un monto numerico mayor a $0."
                    : "Este tamano quedara fuera de tu servicio."}
                </p>
              )}
            </article>
          ))}
        </div>

        <div className={styles.actions}>
          <button
            type="button"
            className={styles.primaryButton}
            onClick={handleSubmit}
            disabled={isSubmitDisabled}
          >
            {isSubmitting ? "Guardando..." : "Guardar tarifas"}
          </button>
        </div>
      </section>
    </main>
  );
}
