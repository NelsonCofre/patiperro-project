import styles from "./VerifiedBadge.module.css";

type VerifiedBadgeProps = {
  verified: boolean;
  variant?: "list" | "profile";
};

export default function VerifiedBadge({
  verified,
  variant = "list"
}: VerifiedBadgeProps) {
  const className = [
    styles.badge,
    verified ? styles.verified : styles.unverified,
    variant === "profile" ? styles.profile : styles.list
  ].join(" ");

  return (
    <span
      className={className}
      aria-label={
        verified
          ? "Paseador con identidad verificada"
          : "Paseador sin verificación de identidad"
      }
    >
      {verified ? (
        <>
          <span className={styles.icon} aria-hidden="true">
            ✓
          </span>
          {variant === "profile" ? "Identidad verificada" : "Verificado"}
        </>
      ) : variant === "profile" ? (
        "Identidad no verificada"
      ) : (
        "No verificado"
      )}
    </span>
  );
}
