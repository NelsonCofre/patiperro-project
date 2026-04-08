// Utilidades de calendario semanal (lunes como inicio de semana, alineado con la agenda del paseador).

/** Devuelve YYYY-MM-DD en hora local. */
export function toISODateLocal(d: Date): string {
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, "0");
  const day = String(d.getDate()).padStart(2, "0");
  return `${y}-${m}-${day}`;
}

/** Lunes 00:00 local de la semana que contiene `d`. */
export function startOfWeekMonday(d: Date): Date {
  const c = new Date(d.getFullYear(), d.getMonth(), d.getDate());
  const day = c.getDay();
  const diff = day === 0 ? -6 : 1 - day;
  c.setDate(c.getDate() + diff);
  c.setHours(0, 0, 0, 0);
  return c;
}

export function addDays(d: Date, n: number): Date {
  const x = new Date(d.getFullYear(), d.getMonth(), d.getDate());
  x.setDate(x.getDate() + n);
  x.setHours(0, 0, 0, 0);
  return x;
}

export const WEEKDAY_LABELS = [
  "Domingo",
  "Lunes",
  "Martes",
  "Miercoles",
  "Jueves",
  "Viernes",
  "Sabado"
] as const;

export function weekdayLabelFromISODate(fechaISO: string): string {
  const d = new Date(`${fechaISO}T12:00:00`);
  return WEEKDAY_LABELS[d.getDay()];
}

export function normalizeDayName(s: string): string {
  return s
    .trim()
    .toLowerCase()
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "");
}

export type WeekDayCell = {
  iso: string;
  date: Date;
  weekdayShort: string;
  dayNum: number;
  isToday: boolean;
};

export function buildWeekDays(weekMonday: Date): WeekDayCell[] {
  const today = toISODateLocal(new Date());
  const cells: WeekDayCell[] = [];
  for (let i = 0; i < 7; i++) {
    const date = addDays(weekMonday, i);
    const iso = toISODateLocal(date);
    cells.push({
      iso,
      date,
      weekdayShort: WEEKDAY_LABELS[date.getDay()].slice(0, 3),
      dayNum: date.getDate(),
      isToday: iso === today
    });
  }
  return cells;
}

/** Semanas futuras permitidas desde la semana actual (inclusive). */
export const MAX_WEEKS_AHEAD = 52;

export function weeksBetween(currentMonday: Date, targetMonday: Date): number {
  const ms = targetMonday.getTime() - currentMonday.getTime();
  return Math.round(ms / (7 * 24 * 60 * 60 * 1000));
}
