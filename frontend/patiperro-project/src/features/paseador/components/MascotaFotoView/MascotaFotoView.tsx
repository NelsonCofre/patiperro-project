import { useState } from "react";
import styles from "./MascotaFotoView.module.css";

type MascotaFotoViewProps = {
  url?: string | null;
  alt: string;
  className?: string;
};

export default function MascotaFotoView({ url, alt, className }: MascotaFotoViewProps) {
  const [failed, setFailed] = useState(false);
  const resolved = url?.trim();

  if (!resolved || failed) {
    return <div className={`${styles.placeholder} ${className ?? ""}`.trim()}>Sin foto</div>;
  }

  return (
    <img
      src={resolved}
      alt={alt}
      className={`${styles.photo} ${className ?? ""}`.trim()}
      onError={() => setFailed(true)}
    />
  );
}
