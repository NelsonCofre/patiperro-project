import { MapContainer, Marker, Popup, TileLayer } from "react-leaflet";
import L from "leaflet";
import icon from "leaflet/dist/images/marker-icon.png";
import iconShadow from "leaflet/dist/images/marker-shadow.png";
import "leaflet/dist/leaflet.css";
import styles from "./MapaEncuentroModal.module.css";

const DefaultIcon = L.icon({
  iconUrl: icon,
  shadowUrl: iconShadow,
  iconSize: [25, 41],
  iconAnchor: [12, 41]
});

L.Marker.prototype.options.icon = DefaultIcon;

type Props = {
  lat: number;
  lng: number;
  direccion: string;
  onClose: () => void;
  title?: string;
};

export default function MapaEncuentroModal({
  lat,
  lng,
  direccion,
  onClose,
  title = "Ubicación del encuentro"
}: Props) {
  return (
    <div className={styles.overlay} onClick={onClose} role="presentation">
      <div
        className={styles.card}
        role="dialog"
        aria-modal="true"
        aria-label={title}
        onClick={(event) => event.stopPropagation()}
      >
        <div className={styles.header}>
          <h3>{title}</h3>
          <button type="button" className={styles.closeButton} onClick={onClose} aria-label="Cerrar mapa">
            ×
          </button>
        </div>

        <div className={styles.mapWrap}>
          <MapContainer
            center={[lat, lng]}
            zoom={16}
            scrollWheelZoom
            style={{ height: "100%", width: "100%" }}
          >
            <TileLayer url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png" />
            <Marker position={[lat, lng]}>
              <Popup>{direccion}</Popup>
            </Marker>
          </MapContainer>
        </div>

        <p className={styles.address}>
          <strong>Dirección:</strong> {direccion}
        </p>
      </div>
    </div>
  );
}
