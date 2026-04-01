// Configuración de servicio: radio de cobertura y tarifas por tamaño (API real + JWT por cookie).
import { Link } from "react-router-dom";
import PaseadorNavbar from "../../components/PaseadorNavbar/PaseadorNavbar";
import { useTarifasForm } from "../../hooks/useTarifasForm";
import { keepOnlyMoneyDigits } from "../../utils/tarifasValidators";
import styles from "./PaseadorConfiguracion.module.css";

export default function PaseadorConfiguracion() {
  const {
    form,
    errors,
    radioError,
    successMessage,
    loadError,
    submitError,
    loadStatus,
    isSubmitting,
    isSubmitDisabled,
    setSuccessMessage,
    updateRadio,
    updatePrice,
    toggleTarifa,
    handleFieldBlur,
    handleRadioBlur,
    handleSubmit
  } = useTarifasForm();

  const isLoading = loadStatus === "loading";

  return (
    <main className={styles.page}>
      {successMessage ? (
        <div className={styles.toastOverlay}>
          <div className={styles.toastCard} role="dialog" aria-modal="true">
            <span className={styles.toastEyebrow}>Configuración guardada</span>
            <h2 className={styles.toastTitle}>Los cambios ya están en el servidor</h2>
            <p className={styles.toastText}>{successMessage}</p>
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
          <h1 className={styles.title}>Radio de cobertura y tarifas por tamaño</h1>
          <p className={styles.description}>
            Define el radio en kilómetros donde ofreces paseos y el precio por hora según el tamaño
            del perro. Los cambios se guardan al pulsar Guardar configuración.
          </p>
        </div>

        <div className={styles.infoCard}>
          <span className={styles.infoLabel}>Estado</span>
          <strong>
            {isLoading ? "Cargando…" : loadError ? "Error al cargar" : "Listo para editar"}
          </strong>
        </div>
      </section>

      {loadError ? (
        <div className={styles.bannerError} role="alert">
          <p>{loadError}</p>
          <p className={styles.bannerHint}>
            Si no has iniciado sesión como paseador, ve a{" "}
            <Link to="/login/paseador">iniciar sesión</Link>.
          </p>
        </div>
      ) : null}

      {submitError ? (
        <div className={styles.bannerError} role="alert">
          {submitError}
        </div>
      ) : null}

      {isLoading ? (
        <p className={styles.loadingText}>Cargando catálogo y tu configuración…</p>
      ) : null}

      {!isLoading && !loadError && form.tarifas.length > 0 ? (
        <section className={styles.formCard}>
          <div className={styles.sectionHeader}>
            <div>
              <p className={styles.cardEyebrow}>Radio de cobertura</p>
              <h2>Zona donde ofreces paseos</h2>
            </div>
          </div>

          <label className={styles.inputGroup}>
            <span>Radio (km)</span>
            <input
              className={styles.textInput}
              value={form.radioCoberturaKm}
              onChange={(e) => updateRadio(e.target.value)}
              onBlur={handleRadioBlur}
              placeholder="Ej: 5 o 10.5"
              inputMode="decimal"
            />
          </label>
          {radioError ? <p className={styles.errorText}>{radioError}</p> : null}

          <div className={styles.sectionHeader}>
            <div>
              <p className={styles.cardEyebrow}>Tarifas por tamaño</p>
              <h2>Precio por hora (entero, mínimo 1)</h2>
            </div>
          </div>

          <div className={styles.tarifasGrid}>
            {form.tarifas.map((tarifa) => (
              <article key={tarifa.tamanoId} className={styles.tarifaItem}>
                <div className={styles.tarifaHeader}>
                  <div>
                    <h3>{tarifa.tamanoNombre}</h3>
                    <p>{tarifa.descripcion || "—"}</p>
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
                  <span>Tarifa / hora</span>
                  <div className={styles.moneyInput}>
                    <span>$</span>
                    <input
                      value={tarifa.precioBase}
                      onChange={(event) =>
                        updatePrice(tarifa.tamanoId, keepOnlyMoneyDigits(event.target.value))
                      }
                      onBlur={() => handleFieldBlur(tarifa.tamanoId)}
                      inputMode="numeric"
                      placeholder="Ej: 7000"
                      disabled={!tarifa.enabled}
                    />
                  </div>
                </label>

                {errors[tarifa.tamanoId] ? (
                  <p className={styles.errorText}>{errors[tarifa.tamanoId]}</p>
                ) : (
                  <p className={styles.helperText}>
                    {tarifa.enabled
                      ? "Enteros ≥ 1 (requisito del backend)."
                      : "Este tamaño no se enviará al guardar."}
                  </p>
                )}
              </article>
            ))}
          </div>

          <div className={styles.actions}>
            <button
              type="button"
              className={styles.primaryButton}
              onClick={() => void handleSubmit()}
              disabled={isSubmitDisabled}
            >
              {isSubmitting ? "Guardando…" : "Guardar configuración"}
            </button>
          </div>
        </section>
      ) : null}
    </main>
  );
}
