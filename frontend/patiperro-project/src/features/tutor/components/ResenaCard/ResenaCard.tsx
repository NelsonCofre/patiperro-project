// src/features/tutor/components/ResenaCard/ResenaCard.tsx
import { useState } from 'react';
import type { ReservaTutorDetalleDTO } from '../../types/reservaTutor.types'; 
import { ResenaModal } from '../ResenaForm/ResenaModal'; 

interface ResenaCardProps {
  reserva: ReservaTutorDetalleDTO;
}

export const ResenaCard = ({ reserva }: ResenaCardProps) => {
  const [openResenaModal, setOpenResenaModal] = useState(false);

  return (
    <div className="border p-4 rounded-lg shadow-sm bg-white mb-4">
      <div className="flex justify-between items-center">
        <div>
          <h3 className="font-bold text-lg">{reserva.mascotaNombre}</h3>
          <p className="text-sm text-gray-500">Paseo con {reserva.paseadorNombre}</p>
          <p className="text-xs text-gray-400">{reserva.fecha}</p>
        </div>
        
        {reserva.nombreEstado === 'FINALIZADA' && !reserva.calificada && (
          <button 
            onClick={() => setOpenResenaModal(true)}
            className="bg-orange-500 hover:bg-orange-600 text-white px-4 py-2 rounded-md transition-colors text-sm font-medium"
          >
            Calificar Paseo
          </button>
        )}
        
        {reserva.calificada && (
          <span className="text-green-600 text-sm font-medium italic">✓ Calificada</span>
        )}
      </div>

      {/* Ahora ResenaModal solo pide reserva y onClose */}
      {openResenaModal && (
        <ResenaModal 
          reserva={reserva} 
          onClose={() => setOpenResenaModal(false)} 
        />
      )}
    </div>
  );
};