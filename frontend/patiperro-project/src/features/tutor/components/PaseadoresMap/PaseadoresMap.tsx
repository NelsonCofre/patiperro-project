import { useState } from "react";
import L from "leaflet";
import "leaflet/dist/leaflet.css";
import { MapContainer, Marker, Popup, TileLayer, useMapEvents } from "react-leaflet";
import type { PaseadorHome } from "../../types/paseadorHome.types";
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

type PaseadoresMapProps = {
  centroLat: number;
  centroLng: number;
  paseadores: PaseadorHome[];
};

type MapEventsHandlerProps = {
  onZoomChange: (zoom: number) => void;
};

function MapEventsHandler({ onZoomChange }: MapEventsHandlerProps) {
  useMapEvents({
    zoomend: (event) => {
      onZoomChange(event.target.getZoom());
    }
  });

  return null;
}

function createWalkerIcon(fotoUrl: string, currentZoom: number) {
  const scale = currentZoom / 14;

  return L.divIcon({
    className: "custom-walker-icon",
    html: `
      <div class="marker-container" style="transform: scale(${scale}); transform-origin: bottom center;">
        <div class="marker-avatar" style="background-image: url('${fotoUrl}')"></div>
        <div class="marker-pin"></div>
      </div>
    `,
    iconSize: [82 * scale, 94 * scale],
    iconAnchor: [(82 * scale) / 2, 94 * scale],
    popupAnchor: [0, -40 * scale]
  });
}

export default function PaseadoresMap({ centroLat, centroLng, paseadores }: PaseadoresMapProps) {
  const [currentZoom, setCurrentZoom] = useState(14);

  return (
    <div className={styles.mapContainer}>
      <MapContainer center={[centroLat, centroLng]} zoom={14} style={{ height: "100%", width: "100%" }}>
        <TileLayer
          attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
          url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
        />

        <MapEventsHandler onZoomChange={setCurrentZoom} />

        <Marker position={[centroLat, centroLng]}>
          <Popup>Estas aqui</Popup>
        </Marker>

        {paseadores.map((paseador) => {
          const lat = Number(paseador.latitud);
          const lon = Number(paseador.longitud);

          if (!Number.isFinite(lat) || !Number.isFinite(lon)) {
            return null;
          }

          return (
            <Marker
              key={`walker-${paseador.id}-zoom-${currentZoom}`}
              position={[lat, lon]}
              icon={createWalkerIcon(paseador.fotoUrl || "https://via.placeholder.com/100", currentZoom)}
            >
              <Popup>
                <div style={{ textAlign: "center" }}>
                  <strong>{paseador.nombre}</strong>
                  <br />
                  {paseador.distanciaKm.toFixed(2)} km
                </div>
              </Popup>
            </Marker>
          );
        })}
      </MapContainer>
    </div>
  );
}
