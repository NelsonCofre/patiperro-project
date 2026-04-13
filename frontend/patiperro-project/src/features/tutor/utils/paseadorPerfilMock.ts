import type {
  DiaDisponibilidad,
  PaseadorHome,
  PaseadorPerfil,
  ResenaPaseador,
  TarifaPorTamano
} from "../types/paseadorHome.types";

const DAY_MS = 24 * 60 * 60 * 1000;

function formatDayLabel(date: Date): string {
  return new Intl.DateTimeFormat("es-CL", {
    weekday: "short",
    day: "2-digit",
    month: "short"
  }).format(date);
}

function buildDisponibilidadProximos7Dias(seed: number): DiaDisponibilidad[] {
  const today = new Date();

  return Array.from({ length: 7 }, (_, index) => {
    const date = new Date(today.getTime() + index * DAY_MS);
    const hasMorning = (seed + index) % 2 === 0;
    const hasAfternoon = (seed + index) % 3 !== 0;

    return {
      fecha: date.toISOString(),
      etiqueta: formatDayLabel(date),
      bloques: [
        ...(hasMorning ? [{ inicio: "09:00", fin: "11:00" }] : []),
        ...(hasAfternoon ? [{ inicio: "16:00", fin: "18:00" }] : [])
      ]
    };
  });
}

function buildResenas(paseadorId: string, seed: number): ResenaPaseador[] {
  if (seed % 5 === 0) {
    return [];
  }

  const baseDate = new Date();
  const resenas: ResenaPaseador[] = [
    {
      id: `${paseadorId}-resena-1`,
      tutorNombre: "Camila Rojas",
      calificacion: 5,
      comentario: "Muy puntual y mi perro volvio tranquilo del paseo.",
      fecha: new Date(baseDate.getTime() - DAY_MS).toISOString()
    },
    {
      id: `${paseadorId}-resena-2`,
      tutorNombre: "Diego Morales",
      calificacion: 4.8,
      comentario: "Mando actualizaciones durante el recorrido y respeto los horarios.",
      fecha: new Date(baseDate.getTime() - 2 * DAY_MS).toISOString()
    },
    {
      id: `${paseadorId}-resena-3`,
      tutorNombre: "Sofia Herrera",
      calificacion: 4.7,
      comentario: "Buena energia con perros grandes y muy amable.",
      fecha: new Date(baseDate.getTime() - 4 * DAY_MS).toISOString()
    },
    {
      id: `${paseadorId}-resena-4`,
      tutorNombre: "Matias Fuentes",
      calificacion: 5,
      comentario: "Excelente comunicacion antes y despues del paseo.",
      fecha: new Date(baseDate.getTime() - 6 * DAY_MS).toISOString()
    },
    {
      id: `${paseadorId}-resena-5`,
      tutorNombre: "Valentina Soto",
      calificacion: 4.9,
      comentario: "Se nota la experiencia, mi perrita quedo feliz.",
      fecha: new Date(baseDate.getTime() - 8 * DAY_MS).toISOString()
    },
    {
      id: `${paseadorId}-resena-6`,
      tutorNombre: "Nicolas Vega",
      calificacion: 4.6,
      comentario: "Todo bien coordinado desde el primer contacto.",
      fecha: new Date(baseDate.getTime() - 10 * DAY_MS).toISOString()
    }
  ];

  return resenas
    .sort((a, b) => new Date(b.fecha).getTime() - new Date(a.fecha).getTime())
    .slice(0, 5);
}

function buildTarifas(seed: number): TarifaPorTamano[] {
  const base = 6000 + (seed % 4) * 500;

  return [
    { tamano: "Pequeno", precio: base },
    { tamano: "Mediano", precio: base + 1500 },
    { tamano: "Grande", precio: base + 3000 }
  ];
}

function parseSeed(id: string): number {
  const parsed = Number.parseInt(id, 10);
  if (Number.isFinite(parsed)) {
    return parsed;
  }
  return id.split("").reduce((sum, char) => sum + char.charCodeAt(0), 0);
}

export function buildPaseadorPerfilMock(paseador: PaseadorHome): PaseadorPerfil {
  const seed = parseSeed(paseador.id);
  const resenas = buildResenas(paseador.id, seed);
  const fallbackRating =
    resenas.length > 0
      ? resenas.reduce((sum, resena) => sum + resena.calificacion, 0) / resenas.length
      : paseador.calificacionPromedio;

  return {
    ...paseador,
    nombreCompleto: paseador.nombre,
    calificacionPromedio: Number(fallbackRating.toFixed(1)),
    precioBase: paseador.precioBase || 6000 + (seed % 4) * 500,
    totalPaseosRealizados: 18 + seed * 3,
    tarifasPorTamano: buildTarifas(seed),
    disponibilidadProximos7Dias: buildDisponibilidadProximos7Dias(seed),
    resenas,
    verificado: seed % 2 === 0
  };
}
