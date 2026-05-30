import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import TutorNavbar from "../../../tutor/components/TutorNavbar/TutorNavbar";
import { fetchMisMascotas } from "../../services/mascotaService";
import type { MascotaListItem } from "../../types/mascota.types";
import styles from "./MisMascotas.module.css";

export default function MisMascotas() {
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState("");
  const [mascotas, setMascotas] = useState<MascotaListItem[]>([]);

  useEffect(() => {
    let cancelled = false;
    setIsLoading(true);
    setError("");

    fetchMisMascotas()
      .then((response) => {
        if (!cancelled) {
          setMascotas(response);
        }
      })
      .catch((err: unknown) => {
        if (!cancelled) {
          setError(err instanceof Error ? err.message : "No se pudieron cargar tus mascotas.");
        }
      })
      .finally(() => {
        if (!cancelled) {
          setIsLoading(false);
        }
      });

    return () => {
      cancelled = true;
    };
  }, []);

  return (
    <main className={styles.page}>
      <TutorNavbar />

      <section className={styles.shell}>
        <header className={styles.header}>
          <div>
            <p className={styles.eyebrow}>Mis mascotas</p>
            <h1 className={styles.title}>Revisa y actualiza la ficha de tus companeros</h1>
            <p className={styles.description}>
              Desde aqui puedes confirmar su foto principal, revisar sus datos y editar la
              informacion cuando cambie.
            </p>
          </div>

          <Link to="/tutor/mascota/nueva" className={styles.primaryButton}>
            Anadir mascota
          </Link>
        </header>

        {error ? (
          <p className={styles.errorBanner} role="alert">
            {error}
          </p>
        ) : null}

        {isLoading ? (
          <div className={styles.grid}>
            {Array.from({ length: 3 }).map((_, index) => (
              <article key={`skeleton-${index}`} className={styles.skeletonCard}>
                <div className={styles.skeletonPhoto} />
                <div className={styles.skeletonLine} />
                <div className={styles.skeletonLineShort} />
              </article>
            ))}
          </div>
        ) : mascotas.length === 0 ? (
          <article className={styles.emptyState}>
            <h2>Aun no has registrado mascotas</h2>
            <p>
              Cuando agregues una ficha aqui podras reemplazar su foto rapidamente antes de
              reservar un paseo.
            </p>
            <Link to="/tutor/mascota/nueva" className={styles.secondaryButton}>
              Crear mi primera mascota
            </Link>
          </article>
        ) : (
          <div className={styles.grid}>
            {mascotas.map((mascota) => (
              <article key={mascota.idMascota} className={styles.card}>
                {mascota.fotoPerfilUrl ? (
                  <img
                    className={styles.photo}
                    src={mascota.fotoPerfilUrl}
                    alt={`Foto de ${mascota.nombre}`}
                  />
                ) : (
                  <div className={styles.placeholder}>Sin foto</div>
                )}

                <div className={styles.cardBody}>
                  <div className={styles.cardHeading}>
                    <h2>{mascota.nombre}</h2>
                    {mascota.edadFormateada ? (
                      <span className={styles.pill}>{mascota.edadFormateada}</span>
                    ) : null}
                  </div>

                  <p className={styles.meta}>
                    {[mascota.especieNombre, mascota.razaNombre, mascota.tamanoNombre]
                      .filter(Boolean)
                      .join(" · ")}
                  </p>

                  <p className={styles.metaMuted}>
                    {mascota.sexo ? `Sexo: ${mascota.sexo}` : "Completa la ficha para ver mas detalles."}
                  </p>

                  <div className={styles.actions}>
                    <Link
                      to={`/tutor/mascotas/${mascota.idMascota}/editar`}
                      className={styles.primaryButton}
                    >
                      Editar ficha
                    </Link>
                    <Link
                      to={`/tutor/mascotas/${mascota.idMascota}/editar`}
                      className={styles.secondaryButton}
                    >
                      Reemplazar foto
                    </Link>
                  </div>
                </div>
              </article>
            ))}
          </div>
        )}
      </section>
    </main>
  );
}
