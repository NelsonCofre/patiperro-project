// Configuración de servicio: radio de cobertura y tarifas por tamaño (API real + JWT por cookie).
import { Link } from "react-router-dom";
import PaseadorNavbar from "../../components/PaseadorNavbar/PaseadorNavbar";
import { useTarifasForm } from "../../hooks/useTarifasForm";
import {
  formatTarifaCLP,
  isTarifaOfrecida,
  keepOnlyMoneyDigits
} from "../../utils/tarifasValidators";
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
    tarifasOfrecidasCount,
    setSuccessMessage,
    updateRadio,
    updatePrice,
    toggleTarifa,
    handleFieldBlur,
    handleRadioBlur,
    handleSubmit
  } = useTarifasForm();

  const isLoading = loadStatus === "loading";
  const totalTamanos = form.tarifas.length;

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
          <p className={styles.eyebrow}>Configurar mi servicio</p>
          <h1 className={styles.title}>Radio de cobertura y tarifas por tamaño</h1>
          <p className={styles.description}>
            Indica en qué zona trabajas y cuánto cobras por hora según el tamaño del perro. Solo
            se publican los tamaños que actives con un precio válido.
          </p>
        </div>

        <div className={styles.heroStats}>
          <article className={styles.infoCard}>
            <span className={styles.infoLabel}>Estado</span>
            <strong>
              {isLoading ? "Cargando…" : loadError ? "Error al cargar" : "Listo para editar"}
            </strong>
          </article>
          {!isLoading && !loadError && totalTamanos > 0 ? (
            <article className={styles.infoCard}>
              <span className={styles.infoLabel}>Tarifas publicadas</span>
              <strong>
                {tarifasOfrecidasCount} de {totalTamanos} tamaños
              </strong>
              <small>
                {tarifasOfrecidasCount > 0
                  ? "Los tutores verán estos precios al buscarte."
                  : "Activa al menos un tamaño con precio por hora."}
              </small>
            </article>
          ) : null}
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
              <p className={styles.sectionHint}>Entre 1 y 50 km desde tu dirección registrada.</p>
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
              <h2>Precio por hora (CLP, entero ≥ 1)</h2>
              <p className={styles.sectionHint}>
                Activa el interruptor para ofrecer ese tamaño y define tu tarifa. Si lo desactivas,
                no aparecerá en tu perfil público.
              </p>
            </div>
          </div>

          <div className={styles.tarifasGrid}>
            {form.tarifas.map((tarifa) => {
              const publicada = isTarifaOfrecida(tarifa);
              const pendientePrecio = tarifa.enabled && !publicada;

              return (
                <article
                  key={tarifa.tamanoId}
                  className={`${styles.tarifaItem} ${publicada ? styles.tarifaItemActive : ""} ${
                    pendientePrecio ? styles.tarifaItemPending : ""
                  }`}
                >
                  <div className={styles.tarifaHeader}>
                    <div>
                      <div className={styles.tarifaTitleRow}>
                        <h3>{tarifa.tamanoNombre}</h3>
                        <span
                          className={`${styles.statusBadge} ${
                            publicada
                              ? styles.statusBadgeActive
                              : pendientePrecio
                                ? styles.statusBadgePending
                                : styles.statusBadgeInactive
                          }`}
                        >
                          {publicada
                            ? "Publicada"
                            : pendientePrecio
                              ? "Falta precio"
                              : "No publicada"}
                        </span>
                      </div>
                      <p>{tarifa.descripcion || "Sin descripción adicional."}</p>
                      {publicada ? (
                        <p className={styles.publishedPrice}>
                          {formatTarifaCLP(tarifa.precioBase)} / hora
                        </p>
                      ) : null}
                    </div>
                  </div>

                  <div className={styles.switchRow}>
                    <button
                      type="button"
                      role="switch"
                      aria-checked={tarifa.enabled}
                      aria-label={`Ofrecer paseos para ${tarifa.tamanoNombre}`}
                      className={`${styles.switch} ${tarifa.enabled ? styles.switchOn : ""}`}
                      onClick={() => toggleTarifa(tarifa.tamanoId, !tarifa.enabled)}
                    >
                      <span className={styles.switchThumb} aria-hidden="true" />
                    </button>
                    <div className={styles.switchCopy}>
                      <strong>{tarifa.enabled ? "Ofrezco este tamaño" : "No ofrezco este tamaño"}</strong>
                      <span>
                        {tarifa.enabled
                          ? "Visible para tutores cuando guardes con un precio válido."
                          : "No se enviará al guardar la configuración."}
                      </span>
                    </div>
                  </div>

                  <label className={styles.inputGroup}>
                    <span>Precio por hora</span>
                    <div
                      className={`${styles.moneyInput} ${
                        !tarifa.enabled ? styles.moneyInputDisabled : ""
                      }`}
                    >
                      <span>$</span>
                      <input
                        value={tarifa.precioBase}
                        onChange={(event) =>
                          updatePrice(tarifa.tamanoId, keepOnlyMoneyDigits(event.target.value))
                        }
                        onBlur={() => handleFieldBlur(tarifa.tamanoId)}
                        inputMode="numeric"
                        placeholder={tarifa.enabled ? "Ej: 7000" : "Activa el tamaño primero"}
                        disabled={!tarifa.enabled}
                      />
                    </div>
                  </label>

                  {errors[tarifa.tamanoId] ? (
                    <p className={styles.errorText}>{errors[tarifa.tamanoId]}</p>
                  ) : (
                    <p className={styles.helperText}>
                      {publicada
                        ? "Esta tarifa se publicará al guardar."
                        : tarifa.enabled
                          ? "Ingresa un monto entero mayor a $0."
                          : "Activa el interruptor si quieres ofrecer este tamaño."}
                    </p>
                  )}
                </article>
              );
            })}
          </div>

          <div className={styles.actions}>
            <p className={styles.saveHint}>
              {tarifasOfrecidasCount > 0
                ? `Se guardarán ${tarifasOfrecidasCount} tarifa${
                    tarifasOfrecidasCount === 1 ? "" : "s"
                  } activa${tarifasOfrecidasCount === 1 ? "" : "s"}.`
                : "Necesitas al menos una tarifa activa con precio para guardar."}
            </p>
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
