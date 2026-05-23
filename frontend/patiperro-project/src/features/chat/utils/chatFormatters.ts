export function formatChatTimestamp(value: string): string {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;

  return date.toLocaleString("es-CL", {
    day: "2-digit",
    month: "short",
    hour: "2-digit",
    minute: "2-digit"
  });
}

export function buildMessageSnippet(content: string, limit = 72): string {
  const normalized = content.replace(/\s+/g, " ").trim();
  if (normalized.length <= limit) return normalized;
  return `${normalized.slice(0, Math.max(0, limit - 1)).trimEnd()}...`;
}

export function buildChatMessageSnippet(
  message: { tipo?: string; content: string },
  limit = 72
): string {
  const tipo = String(message.tipo ?? "TEXTO").toUpperCase();
  if (tipo === "IMAGEN") {
    const cap = message.content.replace(/\s+/g, " ").trim();
    return cap ? `📷 ${buildMessageSnippet(cap, limit - 2)}` : "📷 Foto del paseo";
  }
  return buildMessageSnippet(message.content, limit);
}

export function isNearBottom(
  element: HTMLElement,
  thresholdPx = 120
): boolean {
  const distance = element.scrollHeight - element.scrollTop - element.clientHeight;
  return distance <= thresholdPx;
}

