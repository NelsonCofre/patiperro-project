// Vista principal de agenda del paseador.
// Muestra una linea de tiempo por dia, el modal de agregar bloque y toasts visuales.
import { Link } from "react-router-dom";
import PaseadorNavbar from "../../components/PaseadorNavbar/PaseadorNavbar";
import { usePaseadorAgenda } from "../../hooks/usePaseadorAgenda";
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

export default function PaseadorAgenda() {
  const {
    days,
    hourSlots,
    allBlocks,
    selectedDay,
    setSelectedDay,
    selectedDayBlocks,
    isAddModalOpen,
    form,
    formErrors,
    isAddDisabled,
    toast,
    openAddModal,
    closeAddModal,
    dismissToast,
    updateFormField,
    handleFieldBlur,
    handleAddBlock,
    handleEditBlock,
    handleDeleteBlock
  } = usePaseadorAgenda();

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
          <button
            type="button"
            className={styles.toastCloseButton}
            onClick={dismissToast}
          >
            Cerrar
          </button>
        </div>
      ) : null}

      {isAddModalOpen ? (
        <div className={styles.modalOverlay}>
          <div className={styles.modalCard} role="dialog" aria-modal="true">
            <span className={styles.modalEyebrow}>Agregar bloque</span>
            <h2 className={styles.modalTitle}>Define una franja de disponibilidad</h2>
            <p className={styles.modalText}>
              Selecciona el dia y el rango horario que quieres mostrar como disponible.
            </p>

            <div className={styles.modalFields}>
              <label className={styles.modalField}>
                <span>Dia</span>
                <select
                  value={form.day}
                  onChange={(event) => updateFormField("day", event.target.value as typeof form.day)}
                  onBlur={() => handleFieldBlur("day")}
                >
                  {days.map((day) => (
                    <option key={day} value={day}>
                      {day}
                    </option>
                  ))}
                </select>
                {formErrors.day ? <small>{formErrors.day}</small> : null}
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
            </div>

            <div className={styles.modalActions}>
              <button
                type="button"
                className={styles.modalSecondaryButton}
                onClick={closeAddModal}
              >
                Cancelar
              </button>
              <button
                type="button"
                className={styles.modalPrimaryButton}
                onClick={handleAddBlock}
                disabled={isAddDisabled}
              >
                Agregar bloque
              </button>
            </div>
          </div>
        </div>
      ) : null}

      <PaseadorNavbar />

      <section className={styles.hero}>
        <div className={styles.heroContent}>
          <p className={styles.eyebrow}>Mi Agenda</p>
          <h1 className={styles.title}>Visualiza tu disponibilidad por dia y hora</h1>
          <p className={styles.description}>
            Esta vista te permite revisar tus bloques semanales de forma intuitiva
            y deja preparada la agenda para crear, detectar conflictos y comunicar
            restricciones desde el frontend.
          </p>

          <div className={styles.heroRibbon}>
            <span className={styles.heroRibbonTag}>Agenda operativa</span>
            <p>
              Ya puedes agregar bloques, verlos en la linea de tiempo y recibir
              feedback visual cuando el horario se cruza o cuando un bloque esta reservado.
            </p>
          </div>
        </div>

        <div className={styles.heroBadge}>
          <span className={styles.heroBadgeLabel}>Resumen actual</span>
          <strong>{allBlocks.length} bloques visibles</strong>
          <p className={styles.heroBadgeText}>
            {selectedDayBlocks.length > 0
              ? `${selectedDay} tiene ${selectedDayBlocks.length} bloque(s) visibles en la agenda.`
              : `${selectedDay} todavia no muestra bloques en la vista actual.`}
          </p>
        </div>
      </section>

      <section className={styles.board}>
        <article className={styles.mainCard}>
          <div className={styles.sectionHeader}>
            <div>
              <p className={styles.cardEyebrow}>Agenda semanal</p>
              <h2>Selecciona un dia para ver su linea de tiempo</h2>
            </div>
            <span className={styles.emptyChip}>Vista inicial</span>
          </div>

          <p className={styles.supportText}>
            Esta agenda ya permite probar la creacion local de bloques y deja listas
            las notificaciones visuales para conflictos y restricciones del servicio.
          </p>

          <div className={styles.dayTabs} role="tablist" aria-label="Dias de agenda">
            {days.map((day) => (
              <button
                key={day}
                type="button"
                role="tab"
                aria-selected={selectedDay === day}
                className={`${styles.dayTab} ${selectedDay === day ? styles.dayTabActive : ""}`}
                onClick={() => setSelectedDay(day)}
              >
                {day}
              </button>
            ))}
          </div>

          <div className={styles.timelineSection}>
            <div className={styles.timelineHeader}>
              <div>
                <p className={styles.cardEyebrow}>Dia seleccionado</p>
                <h3>{selectedDay}</h3>
              </div>
              <span className={styles.timelineMeta}>
                {selectedDayBlocks.length > 0
                  ? `${selectedDayBlocks.length} bloque(s) visibles`
                  : "Sin bloques todavia"}
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
                        <span>
                          {block.status === "booked" ? "Reservado" : "Disponible"}
                        </span>
                      </div>
                      <p>{`${block.startTime} - ${block.endTime}`}</p>
                      <small>{block.durationLabel}</small>
                      <div className={styles.blockActions}>
                        <button
                          type="button"
                          className={styles.blockActionButton}
                          onClick={() => handleEditBlock(block.id)}
                        >
                          Editar
                        </button>
                        <button
                          type="button"
                          className={styles.blockActionButton}
                          onClick={() => handleDeleteBlock(block.id)}
                        >
                          Eliminar
                        </button>
                      </div>
                    </article>
                  ))
                ) : (
                  <div className={styles.emptyTimeline}>
                    <strong>{selectedDay} esta libre por completo</strong>
                    <p>
                      Cuando empieces a agregar bloques, apareceran aqui sobre la
                      linea de tiempo por hora.
                    </p>
                  </div>
                )}
              </div>
            </div>
          </div>
        </article>

        <aside className={styles.sideCard}>
          <p className={styles.cardEyebrow}>Acciones y pruebas</p>
          <h2>Agenda lista para la siguiente integracion</h2>
          <p className={styles.supportText}>
            Desde aqui ya puedes abrir el modal, agregar bloques locales y ver
            los mensajes visuales cuando el horario se cruza con otro o el bloque
            no se puede modificar porque esta reservado.
          </p>

          <div className={styles.summaryStack}>
            <div className={styles.summaryCard}>
              <span className={styles.summaryLabel}>Bloques visibles</span>
              <strong>{allBlocks.length}</strong>
            </div>
            <div className={styles.summaryCard}>
              <span className={styles.summaryLabel}>Dia activo</span>
              <strong>{selectedDay}</strong>
            </div>
          </div>

          <button type="button" className={styles.primaryButton} onClick={openAddModal}>
            Agregar bloque
          </button>

          <p className={styles.helperText}>
            Prueba tambien creando un bloque que se cruce con otro existente para
            ver el toast de conflicto.
          </p>

          <Link to="/paseador/dashboard" className={styles.secondaryLink}>
            Volver al dashboard
          </Link>
        </aside>
      </section>
    </main>
  );
}
