import { useEffect, useState } from "react";
import L from "leaflet";
import "leaflet/dist/leaflet.css";
import { MapContainer, Marker, Popup, TileLayer, useMap, useMapEvents } from "react-leaflet";
import type { PaseadorHome } from "../../types/paseadorHome.types";
import PaseadorCard from "../PaseadorCard/PaseadorCard";
import styles from "./PaseadoresMap.module.css";

import markerIcon from "leaflet/dist/images/marker-icon.png";
import markerShadow from "leaflet/dist/images/marker-shadow.png";

const DefaultIcon = L.icon({
  iconUrl: markerIcon,
  shadowUrl: markerShadow,
  iconSize: [25, 41],
  iconAnchor: [12, 41],
  popupAnchor: [1, -34]
});

L.Marker.prototype.options.icon = DefaultIcon;

interface PaseadoresMapProps {
  centroLat: number;
  centroLng: number;
  paseadores: PaseadorHome[];
  onVerPerfil: (p: PaseadorHome) => void;
  emptyTitle?: string;
  emptyMessage?: string;
}

function MapController({
  lat,
  lng,
  onZoomChange
}: {
  lat: number;
  lng: number;
  onZoomChange: (z: number) => void;
}) {
  const map = useMap();

  useEffect(() => {
    const timer = window.setTimeout(() => {
      map.invalidateSize();
    }, 250);
    map.setView([lat, lng], map.getZoom());
    return () => window.clearTimeout(timer);
  }, [lat, lng, map]);

  useMapEvents({
    zoomend: (event) => {
      onZoomChange(event.target.getZoom());
    }
  });

  return null;
}

function createWalkerIcon(fotoUrl: string, currentZoom: number, isSelected: boolean) {
  const scale = (currentZoom / 14) * (isSelected ? 1.3 : 1);
  const borderColor = isSelected ? "#2ecc71" : "#f1c40f";

  return L.divIcon({
    className: "custom-walker-icon",
    html: `
      <div style="transform: scale(${scale}); transform-origin: bottom center; display: flex; flex-direction: column; align-items: center;">
        <div style="
            background-image: url('${fotoUrl}');
            width: 45px;
            height: 45px;
            border-radius: 50%;
            border: 3px solid ${borderColor};
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
            border-top: 12px solid ${borderColor};
            margin-top: -4px;
        "></div>
      </div>
    `,
    iconSize: [45 * scale, 60 * scale],
    iconAnchor: [(45 * scale) / 2, 60 * scale],
    popupAnchor: [0, -50 * scale]
  });
}

export default function PaseadoresMap({
  centroLat,
  centroLng,
  paseadores,
  onVerPerfil,
  emptyTitle = "No hay paseadores en esta zona",
  emptyMessage = "Prueba ampliando el radio o limpiando los filtros de busqueda."
}: PaseadoresMapProps) {
  const [currentZoom, setCurrentZoom] = useState(14);
  const [selectedId, setSelectedId] = useState<string | null>(null);

  useEffect(() => {
    if (!selectedId) return;
    const cardElement = document.getElementById(`card-${selectedId}`);
    cardElement?.scrollIntoView({ behavior: "smooth", block: "nearest" });
  }, [selectedId]);

  return (
    <div className={styles.mainWrapper}>
      <div className={styles.mapContainer}>
        <MapContainer center={[centroLat, centroLng]} zoom={14} style={{ height: "100%", width: "100%" }}>
          <TileLayer url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png" />
          <MapController lat={centroLat} lng={centroLng} onZoomChange={setCurrentZoom} />

          <Marker position={[centroLat, centroLng]}>
            <Popup>Estas aqui</Popup>
          </Marker>

          {paseadores.map((p) => (
            <Marker
              key={`marker-${p.id}-${currentZoom}-${selectedId === p.id}`}
              position={[Number(p.latitud), Number(p.longitud)]}
              icon={createWalkerIcon(p.fotoUrl || "", currentZoom, selectedId === p.id)}
              eventHandlers={{
                click: () => setSelectedId(p.id)
              }}
            >
              <Popup>
                <div style={{ textAlign: "center" }}>
                  <strong>{p.nombre}</strong>
                  <br />
                  <button
                    onClick={() => onVerPerfil(p)}
                    style={{ marginTop: "5px", cursor: "pointer" }}
                  >
                    Ver Perfil Completo
                  </button>
                </div>
              </Popup>
            </Marker>
          ))}
        </MapContainer>
      </div>
      <div className={styles.cardsContainer}>
        {paseadores.length > 0 ? (
          paseadores.map((p) => (
            <div
              id={`card-${p.id}`}
              key={p.id}
              className={`${styles.cardWrapper} ${selectedId === p.id ? styles.selectedCard : ""}`}
              onClick={() => setSelectedId(p.id)}
            >
              <PaseadorCard paseador={p} onVerPerfil={() => onVerPerfil(p)} />
            </div>
          ))
        ) : (
          <div className={styles.noWalkersMessage}>
            <p className={styles.noWalkersIcon}>?</p>
            <p>
              <strong>{emptyTitle}</strong>
            </p>
            <p>{emptyMessage}</p>
          </div>
        )}
      </div>
    </div>
  );
}
