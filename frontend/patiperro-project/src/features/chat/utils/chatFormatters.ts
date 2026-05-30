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

export function isNearBottom(
  element: HTMLElement,
  thresholdPx = 120
): boolean {
  const distance = element.scrollHeight - element.scrollTop - element.clientHeight;
  return distance <= thresholdPx;
}

