// Vista de agenda con calendario semanal, bloques API y bloqueo local de dias por motivos personales.
import { Link } from "react-router-dom";
import PaseadorNavbar from "../../components/PaseadorNavbar/PaseadorNavbar";
import { usePaseadorAgendaApi } from "../../hooks/usePaseadorAgendaApi";
import { MAX_WEEKS_AHEAD } from "../../utils/agendaWeekUtils";
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

function formatWeekRangeLabel(weekDays: { isoDate: string }[]) {
  if (weekDays.length === 0) return "";

  const first = new Date(`${weekDays[0].isoDate}T12:00:00`);
  const last = new Date(`${weekDays[weekDays.length - 1].isoDate}T12:00:00`);
  const opts: Intl.DateTimeFormatOptions = { day: "numeric", month: "short", year: "numeric" };
  return `${first.toLocaleDateString("es-CL", opts)} - ${last.toLocaleDateString("es-CL", opts)}`;
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
    selectedDateBlocked,
    blockedRanges,
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
    isBlockDaysModalOpen,
    blockRangeForm,
    blockRangeErrors,
    isBlockDaysDisabled,
    openAddModal,
    closeAddModal,
    openBlockDaysModal,
    closeBlockDaysModal,
    dismissToast,
    updateFormField,
    updateBlockRangeField,
    handleFieldBlur,
    handleBlockRangeBlur,
    handleAddBlock,
    handleBlockDays,
    handleDeleteBlock,
    handleUnblockSelectedDate,
    textoEstadoServidor,
    catalogLoading,
    catalogError
  } = usePaseadorAgendaApi();

  const selectedCell = weekDays.find((cell) => cell.isoDate === selectedISODate);

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
              Se guarda en el servidor como agenda-bloque con fecha y horario exactos.
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
                <span>Hora de termino</span>
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
                  onClick={() => setRepeatMismoDiaEnMes((value) => !value)}
                >
                  <span className={styles.repeatWeekdayToggleKnob} aria-hidden />
                  <span className={styles.repeatWeekdayToggleLabel}>
                    Repetir este mismo dia para los siguientes{" "}
                    <strong className={styles.repeatWeekdayName}>
                      {selectedDayLabel.toLowerCase()}
                    </strong>{" "}
                    del mes.
                  </span>
                </button>
              </div>
            </div>

            {addBlockDisabledReason ? (
              <p className={styles.modalWarning}>{addBlockDisabledReason}</p>
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
                {saving ? "Guardando..." : repeatMismoDiaEnMes ? "Guardar serie del mes" : "Guardar bloque"}
              </button>
            </div>
          </div>
        </div>
      ) : null}

      {isBlockDaysModalOpen ? (
        <div className={styles.modalOverlay}>
          <div className={styles.modalCard} role="dialog" aria-modal="true">
            <span className={styles.modalEyebrow}>Bloquear dias</span>
            <h2 className={styles.modalTitle}>Bloquea fechas por motivos personales</h2>
            <p className={styles.modalText}>
              Este formulario ya queda preparado para enviar al backend un objeto con
              <strong> fecha_inicio </strong>y<strong> fecha_fin</strong>.
            </p>

            <div className={styles.modalFields}>
              <label className={styles.modalField}>
                <span>Fecha inicio</span>
                <input
                  type="date"
                  value={blockRangeForm.fecha_inicio}
                  onChange={(event) =>
                    updateBlockRangeField("fecha_inicio", event.target.value)
                  }
                  onBlur={() => handleBlockRangeBlur("fecha_inicio")}
                />
                {blockRangeErrors.fecha_inicio ? (
                  <small>{blockRangeErrors.fecha_inicio}</small>
                ) : null}
              </label>

              <label className={styles.modalField}>
                <span>Fecha fin</span>
                <input
                  type="date"
                  value={blockRangeForm.fecha_fin}
                  onChange={(event) => updateBlockRangeField("fecha_fin", event.target.value)}
                  onBlur={() => handleBlockRangeBlur("fecha_fin")}
                />
                {blockRangeErrors.fecha_fin ? (
                  <small>{blockRangeErrors.fecha_fin}</small>
                ) : null}
              </label>
            </div>

            <div className={styles.modalActions}>
              <button
                type="button"
                className={styles.modalSecondaryButton}
                onClick={closeBlockDaysModal}
              >
                Cancelar
              </button>
              <button
                type="button"
                className={styles.modalPrimaryButton}
                onClick={handleBlockDays}
                disabled={isBlockDaysDisabled}
              >
                Bloquear dias
              </button>
            </div>
          </div>
        </div>
      ) : null}

      <PaseadorNavbar />

      <section className={styles.hero}>
        <div className={styles.heroContent}>
          <p className={styles.eyebrow}>Mi Agenda</p>
          <h1 className={styles.title}>Calendario semanal y dias no disponibles</h1>
          <p className={styles.description}>
            Navega la semana visible, crea bloques horarios y bloquea fechas concretas
            por motivos personales para diferenciar claramente tu disponibilidad.
          </p>

          <div className={styles.heroRibbon}>
            <span className={styles.heroRibbonTag}>Agenda ampliada</span>
            <p>
              Los dias bloqueados se pintan distinto y ocultan los bloques horarios del
              dia en la linea de tiempo actual.
            </p>
          </div>
        </div>

        <div className={styles.heroBadge}>
          <span className={styles.heroBadgeLabel}>Estado en servidor</span>
          <strong>{textoEstadoServidor()}</strong>
          <p className={styles.heroBadgeText}>
            {catalogLoading
              ? "Cargando catalogos de agenda..."
              : catalogError
                ? catalogError
                : `${allBlocks.length} bloque(s) reales · ${blockedRanges.length} bloqueo(s) locales por fecha`}
          </p>
        </div>
      </section>

      <section className={styles.board}>
        <article className={styles.mainCard}>
          <div className={styles.sectionHeader}>
            <div>
              <p className={styles.cardEyebrow}>Calendario</p>
              <h2>Semana visible y linea de tiempo del dia</h2>
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
                Solo semanas actuales y futuras, hasta {MAX_WEEKS_AHEAD} semanas adelante.
              </p>
            </div>
          </div>

          <p className={styles.supportText}>
            Pulsa una fecha visible para revisar sus bloques. Si la fecha esta bloqueada,
            aparecera marcada como <strong>No disponible</strong> y la linea de tiempo
            ocultara los bloques de ese dia.
          </p>

          <div className={styles.dayTabs} role="tablist" aria-label="Dias de la semana visible">
            {weekDays.map((cell) => (
              <button
                key={cell.isoDate}
                type="button"
                role="tab"
                aria-selected={selectedISODate === cell.isoDate}
                className={`${styles.dayTab} ${selectedISODate === cell.isoDate ? styles.dayTabActive : ""} ${
                  cell.isToday ? styles.dayTabToday : ""
                } ${cell.isBlocked ? styles.dayBlocked : ""}`}
                onClick={() => setSelectedISODate(cell.isoDate)}
              >
                <span>{cell.dayLabel.slice(0, 3)}</span>
                <span className={styles.dayTabDate}>{cell.dayNumber}</span>
                <small className={styles.dayTabMonth}>{cell.monthLabel}</small>
                {cell.isBlocked ? <em className={styles.blockedBadge}>No disponible</em> : null}
              </button>
            ))}
          </div>

          <div className={styles.timelineSection}>
            <div className={styles.timelineHeader}>
              <div>
                <p className={styles.cardEyebrow}>Dia seleccionado</p>
                <h3>
                  {selectedDayLabel} · {selectedISODate}
                </h3>
              </div>
              <span className={styles.timelineMeta}>
                {selectedDateBlocked
                  ? "Dia bloqueado"
                  : selectedDayBlocks.length > 0
                    ? `${selectedDayBlocks.length} bloque(s)`
                    : "Sin bloques este dia"}
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

              <div className={`${styles.timelineLane} ${selectedDateBlocked ? styles.timelineLaneBlocked : ""}`}>
                {hourSlots.map((hour) => (
                  <div key={hour} className={styles.timeRow} />
                ))}

                {selectedDateBlocked ? (
                  <div className={styles.blockedTimeline}>
                    <strong>No disponible</strong>
                    <p>
                      Esta fecha fue bloqueada por motivos personales. Los bloques horarios
                      existentes quedan ocultos en esta vista.
                    </p>
                  </div>
                ) : selectedDayBlocks.length > 0 ? (
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
                    <strong>Sin bloques este dia</strong>
                    <p>Crea uno con el boton del panel lateral o bloquea la fecha completa.</p>
                  </div>
                )}
              </div>
            </div>
          </div>
        </article>

        <aside className={styles.sideCard}>
          <p className={styles.cardEyebrow}>Acciones</p>
          <h2>Gestionar disponibilidad y bloqueos</h2>
          <p className={styles.supportText}>
            Puedes crear bloques reales en la API y, desde frontend, bloquear fechas
            individuales o rangos usando <code>fecha_inicio</code> y <code>fecha_fin</code>.
          </p>

          <div className={styles.summaryStack}>
            <div className={styles.summaryCard}>
              <span className={styles.summaryLabel}>Bloques reales</span>
              <strong>{allBlocks.length}</strong>
            </div>
            <div className={styles.summaryCard}>
              <span className={styles.summaryLabel}>Fecha activa</span>
              <strong>{selectedCell ? `${selectedCell.dayNumber} ${selectedCell.monthLabel}` : selectedISODate}</strong>
            </div>
            <div className={styles.summaryCard}>
              <span className={styles.summaryLabel}>Bloqueos locales</span>
              <strong>{blockedRanges.length}</strong>
            </div>
          </div>

          <button type="button" className={styles.primaryButton} onClick={openAddModal}>
            Nuevo bloque (API)
          </button>

          <button type="button" className={styles.secondaryButton} onClick={openBlockDaysModal}>
            Bloquear dias
          </button>

          <button
            type="button"
            className={styles.ghostButton}
            onClick={handleUnblockSelectedDate}
          >
            Quitar bloqueo del dia seleccionado
          </button>

          <p className={styles.helperText}>
            Si la fecha activa esta bloqueada, la agenda la mostrara con estilo gris y
            etiqueta de no disponibilidad.
          </p>

          <Link to="/paseador/dashboard" className={styles.secondaryLink}>
            Volver al dashboard
          </Link>
        </aside>
      </section>
    </main>
  );
}
