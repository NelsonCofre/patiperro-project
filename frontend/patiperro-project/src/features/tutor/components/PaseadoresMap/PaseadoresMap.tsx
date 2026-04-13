import React, { useState } from 'react';
import { MapContainer, TileLayer, Marker, Popup, useMapEvents } from 'react-leaflet';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import styles from './PaseadoresMap.module.css';

// Iconos estándar para el tutor
import markerIcon from 'leaflet/dist/images/marker-icon.png';
import markerShadow from 'leaflet/dist/images/marker-shadow.png';

const DefaultIcon = L.icon({
  iconUrl: markerIcon,
  shadowUrl: markerShadow,
  iconSize: [25, 41],
  iconAnchor: [12, 41],
  popupAnchor: [1, -34],
});
L.Marker.prototype.options.icon = DefaultIcon;

interface PaseadoresMapProps {
  centroLat: number;
  centroLng: number;
  paseadores: any[];
}

const PaseadoresMap: React.FC<PaseadoresMapProps> = ({ centroLat, centroLng, paseadores }) => {
  // 1. Estado para rastrear el zoom y ajustar el tamaño del pin
  const [currentZoom, setCurrentZoom] = useState(14);

  // 2. Componente interno para capturar eventos del mapa
  const MapEventsHandler = () => {
    useMapEvents({
      zoomend: (e) => {
        setCurrentZoom(e.target.getZoom());
      },
    });
    return null;
  };

  // 3. Función Pro corregida para tamaño dinámico
  const createWalkerIcon = (fotoUrl: string) => {
    // Calculamos una escala basada en el zoom actual
    // Si el zoom es 14, la escala es 1. Si es 16, es 1.4, etc.
    const scale = currentZoom / 14;

    return L.divIcon({
      className: 'custom-walker-icon',
      html: `
        <div class="marker-container" style="transform: scale(${scale}); transform-origin: bottom center;">
          <div class="marker-avatar" style="background-image: url('${fotoUrl}')"></div>
          <div class="marker-pin"></div>
        </div>
      `,
      // Ajustamos el área de colisión del icono para que el popup no salga mal
      iconSize: [82 * scale, 94 * scale],
      iconAnchor: [(82 * scale) / 2, 94 * scale],
      popupAnchor: [0, -40 * scale],
    });
  };

  return (
    <div className={styles.mapContainer}>
      <MapContainer 
        center={[centroLat, centroLng]} 
        zoom={14} 
        style={{ height: '100%', width: '100%' }}
      >
        <TileLayer
          attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
          url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
        />

        {/* Manejador de eventos de zoom */}
        <MapEventsHandler />

        {/* Marcador del Tutor */}
        <Marker position={[centroLat, centroLng]}>
          <Popup>📍 Estás aquí</Popup>
        </Marker>

        {/* Marcadores de Paseadores con Foto Pro y escala dinámica */}
        {/* Marcadores de Paseadores */}
        {paseadores.map((p) => {
          const lat = parseFloat(p.latitud);
          const lon = parseFloat(p.longitud);

          if (isNaN(lat) || isNaN(lon)) return null;

          // Creamos el icono DIRECTAMENTE aquí adentro para usar el currentZoom
          const dynamicScale = currentZoom / 14;
          const size = 45 * dynamicScale;

          const dynamicIcon = L.divIcon({
            className: 'custom-walker-icon',
            // Usamos estilos en línea agresivos para ganar al CSS
            html: `
              <div style="display: flex; flex-direction: column; align-items: center; transform: scale(${dynamicScale}); transform-origin: bottom;">
                <div style="
                  width: 45px; 
                  height: 45px; 
                  border-radius: 50%; 
                  border: 3px solid #f1c40f; 
                  background-image: url('${p.fotoUrl || 'https://via.placeholder.com/100'}'); 
                  background-size: cover; 
                  background-position: center;
                  box-shadow: 0 2px 6px rgba(0,0,0,0.4);
                  background-color: #fff;
                "></div>
                <div style="
                  width: 0; 
                  height: 0; 
                  border-left: 9px solid transparent; 
                  border-right: 9px solid transparent; 
                  border-top: 12px solid #f1c40f; 
                  margin-top: -4px;
                "></div>
              </div>
            `,
            iconSize: [size, size + 15],
            iconAnchor: [size / 2, size + 15],
            popupAnchor: [0, -size],
          });

          return (
            <Marker 
              // VIGILANCIA: Al incluir currentZoom en la key, forzamos el re-render total
              key={`walker-${p.id}-zoom-${currentZoom}`} 
              position={[lat, lon]}
              icon={dynamicIcon}
            >
              <Popup>
                <div style={{ textAlign: 'center' }}>
                  <strong>{p.nombre}</strong><br/>
                  {p.distanciaKm.toFixed(2)} km
                </div>
              </Popup>
            </Marker>
          );
        })}
      </MapContainer>
    </div>
  );
};

export default PaseadoresMap;