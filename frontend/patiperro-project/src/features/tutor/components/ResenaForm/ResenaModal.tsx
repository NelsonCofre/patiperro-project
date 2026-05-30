import type { FC } from 'react';
import { useState } from 'react';
import type { ReservaTutorDetalleDTO } from '../../types/reservaTutor.types';
import { useResena } from '../../hooks/useResena';
import styles from './ResenaModal.module.css';

interface ResenaModalProps {
  reserva: ReservaTutorDetalleDTO;
  onClose: () => void;
}

export const ResenaModal: FC<ResenaModalProps> = ({ reserva, onClose }) => {
  const [estrellas, setEstrellas] = useState<number>(0);
  const [comentario, setComentario] = useState<string>('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isSuccess, setIsSuccess] = useState(false);
  
  const { enviarResena, error } = useResena();

  // Constantes de validación
  const MAX_CARACTERES = 250;
  const comentarioExcedido = comentario.length > MAX_CARACTERES;
  const botonDeshabilitado = estrellas === 0 || isSubmitting || comentarioExcedido;

  // Dentro de ResenaModal.tsx
const handleSend = async () => {
  if (botonDeshabilitado) return;

  // Escenario 3: Si el texto es muy corto (ej: "abc"), lo enviamos como vacío
  // para que solo se guarden las estrellas.
  const comentarioLimpio = comentario.trim();
  const comentarioFinal = comentarioLimpio.length < 5 ? "" : comentarioLimpio;

  setIsSubmitting(true);
  try {
    await enviarResena({
      idReserva: reserva.idReserva,
      idTutor: reserva.idTutorUsuario!,
      idPaseador: reserva.idPaseador!,
      estrellas: estrellas,
      comentario: comentarioFinal // Enviamos el texto procesado
    });
    
    setIsSuccess(true);
    setTimeout(() => onClose(), 2000);
  } catch (err) {
    // Error manejado por el hook
  } finally {
    setIsSubmitting(false);
  }
};

  return (
    <div className={styles.overlay}>
      <div className={styles.modalCard}>
        {isSuccess ? (
          <div className={styles.successContainer}>
            <div className={styles.successIcon}>✓</div>
            <h2 className={styles.title}>¡Reseña publicada!</h2>
          </div>
        ) : (
          <>
            <div className={styles.header}>
              <h2 className={styles.title}>¿Cómo estuvo el paseo de {reserva.mascotaNombre}?</h2>
            </div>

            {error && (
              <div className={styles.errorBox}>
                <span>⚠️</span>
                <p>{error}</p>
              </div>
            )}

            <div className={styles.starGroup}>
              {[1, 2, 3, 4, 5].map((star) => (
                <button
                  key={star}
                  type="button"
                  className={styles.starButton}
                  onClick={() => setEstrellas(star)}
                  style={{ color: star <= estrellas ? '#f2c45d' : '#e2e8f0' }}
                >
                  ★
                </button>
              ))}
            </div>

            <div className={styles.textareaContainer}>
              <textarea
                className={`${styles.textarea} ${comentarioExcedido ? styles.textareaError : ''}`}
                placeholder="Escribe un comentario opcional..."
                value={comentario}
                onChange={(e) => setComentario(e.target.value)}
              />
              {/* Contador de caracteres */}
              <div className={`${styles.charCounter} ${comentarioExcedido ? styles.charError : ''}`}>
                {comentario.length}/{MAX_CARACTERES}
              </div>
            </div>

            <div className={styles.footer}>
              <button className={styles.secondaryButton} onClick={onClose} disabled={isSubmitting}>
                Cancelar
              </button>
              <button 
                className={botonDeshabilitado ? styles.primaryButtonDisabled : styles.primaryButton} 
                onClick={handleSend}
                disabled={botonDeshabilitado}
              >
                {isSubmitting ? 'Publicando...' : 'Publicar Reseña'}
              </button>
            </div>
          </>
        )}
      </div>
    </div>
  );
};