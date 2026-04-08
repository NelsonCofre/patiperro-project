// Vista de agenda con calendario semanal (actual + siguientes) e integración API agenda-bloque.
import { Link } from "react-router-dom";
import PaseadorNavbar from "../../components/PaseadorNavbar/PaseadorNavbar";
import { usePaseadorAgendaApi } from "../../hooks/usePaseadorAgendaApi";
import { MAX_WEEKS_AHEAD, weekdayLabelFromISODate } from "../../utils/agendaWeekUtils";
import styles from "./PaseadorAgenda.module.css";

const TIMELINE_START_MINUTES = 7 * 60;
const TIMELINE_HEIGHT = 780;
const MINUTES_RANGE = 14 * 60;

function getBlockStyle(startMinutes: number, endMinutes: number) {
  const top = ((startMinutes - TIMELINE_START_MINUTES) / MINUTES_RANGE) * TIMELINE_HEIGHT;
  const height = ((endMinutes - startMinutes) / MINUTES_RANGE) * TIMELINE_HEIGHT;

  return {
    top: `${top}px`,
    height: `${Math.max(height, 56)}px`
  };
}

function formatWeekRangeLabel(weekDays: { date: Date }[]): string {
  if (weekDays.length === 0) return "";
  const first = weekDays[0].date;
  const last = weekDays[6].date;
  const opts: Intl.DateTimeFormatOptions = { day: "numeric", month: "short", year: "numeric" };
  return `${first.toLocaleDateString("es-CL", opts)} – ${last.toLocaleDateString("es-CL", opts)}`;
}

export default function PaseadorAgenda() {
  const {
    weekDays,
    goPrevWeek,
    goNextWeek,
    canGoPrevWeek,
    canGoNextWeek,
    selectedISODate,
    setSelectedISODate,
    selectedDayLabel,
    hourSlots,
    allBlocks,
    selectedDayBlocks,
    isAddModalOpen,
    form,
    formErrors,
    isAddDisabled,
    addBlockDisabledReason,
    repeatMismoDiaEnMes,
    setRepeatMismoDiaEnMes,
    saving,
    toast,
    openAddModal,
    closeAddModal,
    dismissToast,
    updateFormField,
    handleFieldBlur,
    handleAddBlock,
    handleDeleteBlock,
    textoEstadoServidor,
    catalogLoading,
    catalogError
  } = usePaseadorAgendaApi();

  const nombreDiaMesRepetir =
    form.fecha && form.fecha.length >= 10
      ? weekdayLabelFromISODate(form.fecha).toLowerCase()
      : selectedDayLabel.toLowerCase();

  return (
    <main className={styles.page}>
      {toast ? (
        <div
          className={`${styles.toast} ${
            toast.type === "error"
              ? styles.toastError
              : toast.type === "success"
                ? styles.toastSuccess
                : styles.toastInfo
          }`}
          role="alert"
        >
          <div className={styles.toastContent}>
            <strong>{toast.title}</strong>
            <p>{toast.message}</p>
          </div>
          <button type="button" className={styles.toastCloseButton} onClick={dismissToast}>
            Cerrar
          </button>
        </div>
      ) : null}

      {isAddModalOpen ? (
        <div className={styles.modalOverlay}>
          <div className={styles.modalCard} role="dialog" aria-modal="true">
            <span className={styles.modalEyebrow}>Agregar bloque</span>
            <h2 className={styles.modalTitle}>Franja de disponibilidad</h2>
            <p className={styles.modalText}>
              Se guarda en el servidor como agenda-bloque (fecha, horario y catálogos día/estado).
            </p>

            <div className={styles.modalFields}>
              <label className={styles.modalField}>
                <span>Fecha</span>
                <input
                  type="date"
                  value={form.fecha}
                  onChange={(event) => updateFormField("fecha", event.target.value)}
                  onBlur={() => handleFieldBlur("fecha")}
                />
                {formErrors.fecha ? <small>{formErrors.fecha}</small> : null}
              </label>

              <label className={styles.modalField}>
                <span>Hora de inicio</span>
                <input
                  type="time"
                  value={form.startTime}
                  onChange={(event) => updateFormField("startTime", event.target.value)}
                  onBlur={() => handleFieldBlur("startTime")}
                />
                {formErrors.startTime ? <small>{formErrors.startTime}</small> : null}
              </label>

              <label className={styles.modalField}>
                <span>Hora de término</span>
                <input
                  type="time"
                  value={form.endTime}
                  onChange={(event) => updateFormField("endTime", event.target.value)}
                  onBlur={() => handleFieldBlur("endTime")}
                />
                {formErrors.endTime ? <small>{formErrors.endTime}</small> : null}
              </label>

              <div className={styles.modalField}>
                <button
                  type="button"
                  role="checkbox"
                  aria-checked={repeatMismoDiaEnMes}
                  className={`${styles.repeatWeekdayToggle} ${
                    repeatMismoDiaEnMes ? styles.repeatWeekdayToggleOn : ""
                  }`}
                  onClick={() => setRepeatMismoDiaEnMes((v) => !v)}
                >
                  <span className={styles.repeatWeekdayToggleKnob} aria-hidden />
                  <span className={styles.repeatWeekdayToggleLabel}>
                    Repetir este mismo día para los siguientes{" "}
                    <strong className={styles.repeatWeekdayName}>{nombreDiaMesRepetir}</strong> del mes.
                  </span>
                </button>
              </div>
            </div>

            {addBlockDisabledReason ? (
              <p className={styles.modalText} style={{ color: "#b45309", marginTop: 12 }}>
                {addBlockDisabledReason}
              </p>
            ) : null}

            <div className={styles.modalActions}>
              <button type="button" className={styles.modalSecondaryButton} onClick={closeAddModal}>
                Cancelar
              </button>
              <button
                type="button"
                className={styles.modalPrimaryButton}
                onClick={() => void handleAddBlock()}
                disabled={isAddDisabled}
              >
                {saving ? "Guardando…" : repeatMismoDiaEnMes ? "Guardar serie del mes" : "Guardar en servidor"}
              </button>
            </div>
          </div>
        </div>
      ) : null}

      <PaseadorNavbar />

      <section className={styles.hero}>
        <div className={styles.heroContent}>
          <p className={styles.eyebrow}>Mi Agenda</p>
          <h1 className={styles.title}>Calendario semanal y disponibilidad</h1>
          <p className={styles.description}>
            Navega la semana en curso y las próximas semanas. Cada bloque se sincroniza con el
            microservicio de agenda.
          </p>

          <div className={styles.heroRibbon}>
            <span className={styles.heroRibbonTag}>Integrado</span>
            <p>
              Los bloques se crean y eliminan vía API. Los reservados no se pueden borrar desde aquí.
            </p>
          </div>
        </div>

        <div className={styles.heroBadge}>
          <span className={styles.heroBadgeLabel}>Estado en servidor</span>
          <strong>{textoEstadoServidor()}</strong>
          <p className={styles.heroBadgeText}>
            {catalogLoading
              ? "Cargando catálogos (días y estados)…"
              : catalogError
                ? catalogError
                : `${allBlocks.length} bloque(s) totales · Día seleccionado: ${selectedDayLabel} (${selectedISODate})`}
          </p>
        </div>
      </section>

      <section className={styles.board}>
        <article className={styles.mainCard}>
          <div className={styles.sectionHeader}>
            <div>
              <p className={styles.cardEyebrow}>Calendario</p>
              <h2>Semana visible y línea de tiempo del día</h2>
            </div>
            <span className={styles.emptyChip}>Lunes a domingo</span>
          </div>

          <div className={styles.weekNav}>
            <div className={styles.weekNavControls}>
              <button
                type="button"
                className={styles.weekNavButton}
                onClick={goPrevWeek}
                disabled={!canGoPrevWeek}
              >
                Semana anterior
              </button>
              <button
                type="button"
                className={styles.weekNavButton}
                onClick={goNextWeek}
                disabled={!canGoNextWeek}
              >
                Semana siguiente
              </button>
            </div>
            <div>
              <p className={styles.weekNavLabel}>{formatWeekRangeLabel(weekDays)}</p>
              <p className={styles.weekNavHint}>
                Solo semanas actuales y futuras (hasta {MAX_WEEKS_AHEAD} semanas adelante).
              </p>
            </div>
          </div>

          <p className={styles.supportText}>
            Pulsa un día de la semana mostrada para ver sus franjas. Las horas cargadas coinciden con
            los bloques devueltos por el backend para esa fecha.
          </p>

          <div className={styles.dayTabs} role="tablist" aria-label="Días de la semana visible">
            {weekDays.map((cell) => (
              <button
                key={cell.iso}
                type="button"
                role="tab"
                aria-selected={selectedISODate === cell.iso}
                className={`${styles.dayTab} ${selectedISODate === cell.iso ? styles.dayTabActive : ""} ${
                  cell.isToday ? styles.dayTabToday : ""
                }`}
                onClick={() => setSelectedISODate(cell.iso)}
              >
                <span>{cell.weekdayShort}</span>
                <span className={styles.dayTabDate}>{cell.dayNum}</span>
              </button>
            ))}
          </div>

          <div className={styles.timelineSection}>
            <div className={styles.timelineHeader}>
              <div>
                <p className={styles.cardEyebrow}>Día seleccionado</p>
                <h3>
                  {selectedDayLabel} · {selectedISODate}
                </h3>
              </div>
              <span className={styles.timelineMeta}>
                {selectedDayBlocks.length > 0
                  ? `${selectedDayBlocks.length} bloque(s)`
                  : "Sin bloques este día"}
              </span>
            </div>

            <div className={styles.timelineWrapper}>
              <div className={styles.timeColumn}>
                {hourSlots.map((hour) => (
                  <span key={hour} className={styles.timeLabel}>
                    {hour}
                  </span>
                ))}
              </div>

              <div className={styles.timelineLane}>
                {hourSlots.map((hour) => (
                  <div key={hour} className={styles.timeRow} />
                ))}

                {selectedDayBlocks.length > 0 ? (
                  selectedDayBlocks.map((block) => (
                    <article
                      key={block.id}
                      className={`${styles.blockCard} ${
                        block.status === "booked" ? styles.blockBooked : styles.blockAvailable
                      }`}
                      style={getBlockStyle(block.startMinutes, block.endMinutes)}
                    >
                      <div className={styles.blockHeader}>
                        <strong>{block.title}</strong>
                        <span>{block.status === "booked" ? "Reservado" : "Disponible"}</span>
                      </div>
                      <p>{`${block.startTime} - ${block.endTime}`}</p>
                      <small>{block.durationLabel}</small>
                      <div className={styles.blockActions}>
                        <button
                          type="button"
                          className={styles.blockActionButton}
                          onClick={() => void handleDeleteBlock(block.idAgenda)}
                          disabled={saving || block.status === "booked"}
                        >
                          Eliminar
                        </button>
                      </div>
                    </article>
                  ))
                ) : (
                  <div className={styles.emptyTimeline}>
                    <strong>Sin bloques este día</strong>
                    <p>Crea uno con el botón del panel lateral (fecha sugerida: día seleccionado).</p>
                  </div>
                )}
              </div>
            </div>
          </div>
        </article>

        <aside className={styles.sideCard}>
          <p className={styles.cardEyebrow}>Acciones</p>
          <h2>Gestionar disponibilidad</h2>
          <p className={styles.supportText}>
            El alta envía POST a /api/agenda/bloques con estado &quot;disponible&quot; inferido del
            catálogo. La baja usa DELETE cuando el bloque no está reservado.
          </p>

          <div className={styles.summaryStack}>
            <div className={styles.summaryCard}>
              <span className={styles.summaryLabel}>Bloques totales</span>
              <strong>{allBlocks.length}</strong>
            </div>
            <div className={styles.summaryCard}>
              <span className={styles.summaryLabel}>Día activo</span>
              <strong>{selectedDayLabel}</strong>
            </div>
          </div>

          <button type="button" className={styles.primaryButton} onClick={openAddModal}>
            Nuevo bloque (API)
          </button>

          <p className={styles.helperText}>
            La fecha inicial del formulario es la del día seleccionado en el calendario.
          </p>

          <Link to="/paseador/dashboard" className={styles.secondaryLink}>
            Volver al dashboard
          </Link>
        </aside>
      </section>
    </main>
  );
}
