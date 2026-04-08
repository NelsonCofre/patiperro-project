// Hook base de la agenda del paseador.
// Controla el dia seleccionado, los bloques visibles, el modal y los toasts de agenda.
import { useMemo, useState } from "react";
import type {
  AgendaBlock,
  AgendaBlockForm,
  AgendaBlockFormErrors,
  AgendaDay,
  AgendaToast
} from "../types/agenda.types";
import {
  hasOverlappingAgendaBlock,
  validateAgendaBlockForm
} from "../utils/agendaValidators";

const DAYS: AgendaDay[] = [
  "Lunes",
  "Martes",
  "Miercoles",
  "Jueves",
  "Viernes",
  "Sabado",
  "Domingo"
];

const HOUR_SLOTS = [
  "07:00",
  "08:00",
  "09:00",
  "10:00",
  "11:00",
  "12:00",
  "13:00",
  "14:00",
  "15:00",
  "16:00",
  "17:00",
  "18:00",
  "19:00",
  "20:00",
  "21:00"
];

const DEMO_BLOCKS: AgendaBlock[] = [
  {
    id: "bloque-1",
    day: "Lunes",
    startTime: "10:00",
    endTime: "11:00",
    title: "Bloque disponible",
    status: "available"
  },
  {
    id: "bloque-2",
    day: "Lunes",
    startTime: "16:00",
    endTime: "18:00",
    title: "Tarde libre",
    status: "available"
  },
  {
    id: "bloque-3",
    day: "Miercoles",
    startTime: "09:00",
    endTime: "10:30",
    title: "Disponibilidad matinal",
    status: "available"
  },
  {
    id: "bloque-4",
    day: "Viernes",
    startTime: "15:00",
    endTime: "17:00",
    title: "Bloque reservado",
    status: "booked"
  }
];

const INITIAL_FORM: AgendaBlockForm = {
  day: "Lunes",
  startTime: "10:00",
  endTime: "11:00"
};

function timeToMinutes(value: string): number {
  const [hours, minutes] = value.split(":").map(Number);
  return hours * 60 + minutes;
}

function formatDuration(startTime: string, endTime: string): string {
  const durationMinutes = timeToMinutes(endTime) - timeToMinutes(startTime);
  const hours = Math.floor(durationMinutes / 60);
  const minutes = durationMinutes % 60;

  if (hours > 0 && minutes > 0) {
    return `${hours} h ${minutes} min`;
  }

  if (hours > 0) {
    return `${hours} h`;
  }

  return `${minutes} min`;
}

export function usePaseadorAgenda() {
  const [selectedDay, setSelectedDay] = useState<AgendaDay>("Lunes");
  const [blocks, setBlocks] = useState<AgendaBlock[]>(DEMO_BLOCKS);
  const [isAddModalOpen, setIsAddModalOpen] = useState(false);
  const [form, setForm] = useState<AgendaBlockForm>(INITIAL_FORM);
  const [formErrors, setFormErrors] = useState<AgendaBlockFormErrors>({});
  const [toast, setToast] = useState<AgendaToast | null>(null);

  const selectedDayBlocks = useMemo(() => {
    return blocks.filter((block) => block.day === selectedDay);
  }, [blocks, selectedDay]);

  const blocksWithMeta = useMemo(
    () =>
      selectedDayBlocks.map((block) => ({
        ...block,
        startMinutes: timeToMinutes(block.startTime),
        endMinutes: timeToMinutes(block.endTime),
        durationLabel: formatDuration(block.startTime, block.endTime)
      })),
    [selectedDayBlocks]
  );

  const isAddDisabled = Object.keys(validateAgendaBlockForm(form)).length > 0;

  const showToast = (nextToast: AgendaToast) => {
    setToast(nextToast);
  };

  const dismissToast = () => {
    setToast(null);
  };

  const openAddModal = () => {
    setForm({
      day: selectedDay,
      startTime: "10:00",
      endTime: "11:00"
    });
    setFormErrors({});
    setIsAddModalOpen(true);
  };

  const closeAddModal = () => {
    setFormErrors({});
    setIsAddModalOpen(false);
  };

  const updateFormField = <K extends keyof AgendaBlockForm>(name: K, value: AgendaBlockForm[K]) => {
    setForm((prev) => ({
      ...prev,
      [name]: value
    }));

    setFormErrors((prev) => ({
      ...prev,
      [name]: undefined
    }));
  };

  const handleFieldBlur = <K extends keyof AgendaBlockForm>(name: K) => {
    const validationErrors = validateAgendaBlockForm(form);

    setFormErrors((prev) => ({
      ...prev,
      [name]: validationErrors[name]
    }));
  };

  const handleAddBlock = () => {
    const validationErrors = validateAgendaBlockForm(form);
    setFormErrors(validationErrors);

    if (Object.keys(validationErrors).length > 0) {
      return;
    }

    if (hasOverlappingAgendaBlock(blocks, form)) {
      showToast({
        type: "error",
        title: "Conflicto de horario",
        message: "Ya tienes un bloque de disponibilidad que coincide con este horario"
      });
      return;
    }

    const newBlock: AgendaBlock = {
      id: `bloque-${Date.now()}`,
      day: form.day,
      startTime: form.startTime,
      endTime: form.endTime,
      title: "Nuevo bloque",
      status: "available"
    };

    setBlocks((prev) => [...prev, newBlock]);
    setSelectedDay(form.day);
    closeAddModal();
    showToast({
      type: "success",
      title: "Bloque agregado",
      message: "La vista de agenda ya refleja el nuevo bloque en frontend."
    });
  };

  const handleEditBlock = (blockId: string) => {
    const targetBlock = blocks.find((block) => block.id === blockId);

    if (!targetBlock) {
      return;
    }

    if (targetBlock.status === "booked") {
      showToast({
        type: "error",
        title: "Bloque protegido",
        message:
          "No puedes modificar este bloque porque ya tienes un paseo programado. Debes gestionar la cancelación del servicio primero"
      });
      return;
    }

    showToast({
      type: "info",
      title: "Edicion pendiente",
      message: "La edicion de bloques se habilitara en la siguiente iteracion."
    });
  };

  const handleDeleteBlock = (blockId: string) => {
    const targetBlock = blocks.find((block) => block.id === blockId);

    if (!targetBlock) {
      return;
    }

    if (targetBlock.status === "booked") {
      showToast({
        type: "error",
        title: "Bloque protegido",
        message:
          "No puedes modificar este bloque porque ya tienes un paseo programado. Debes gestionar la cancelación del servicio primero"
      });
      return;
    }

    setBlocks((prev) => prev.filter((block) => block.id !== blockId));
    showToast({
      type: "success",
      title: "Bloque eliminado",
      message: "El bloque se eliminó de la vista local de la agenda."
    });
  };

  return {
    days: DAYS,
    hourSlots: HOUR_SLOTS,
    allBlocks: blocks,
    selectedDay,
    setSelectedDay,
    selectedDayBlocks: blocksWithMeta,
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
  };
}
