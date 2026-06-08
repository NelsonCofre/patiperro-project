import badgeStyles from "../../styles/estadoBadge.module.css";

type Props = {
  label: string;
  badgeClass: string;
  showDot?: boolean;
};

export default function EstadoBadge({ label, badgeClass, showDot = true }: Props) {
  const toneClass = badgeStyles[badgeClass as keyof typeof badgeStyles] ?? badgeStyles.statusDesconocida;

  return (
    <span className={`${badgeStyles.badge} ${toneClass}`}>
      {showDot ? <span className={badgeStyles.dot} aria-hidden="true" /> : null}
      {label}
    </span>
  );
}
