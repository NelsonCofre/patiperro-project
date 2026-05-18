import { MapContainer, TileLayer, Marker, Popup } from 'react-leaflet';
import 'leaflet/dist/leaflet.css';
import L from 'leaflet';

// Fix para el icono de Leaflet que a veces desaparece en React
import icon from 'leaflet/dist/images/marker-icon.png';
import iconShadow from 'leaflet/dist/images/marker-shadow.png';

let DefaultIcon = L.icon({
    iconUrl: icon,
    shadowUrl: iconShadow,
    iconSize: [25, 41],
    iconAnchor: [12, 41]
});
L.Marker.prototype.options.icon = DefaultIcon;

interface MapaModalProps {
  lat: number;
  lng: number;
  direccion: string;
  onClose: () => void;
}

export default function MapaModal({ lat, lng, direccion, onClose }: MapaModalProps) {
  return (
    <div style={{
      position: 'fixed', top: 0, left: 0, width: '100%', height: '100%',
      backgroundColor: 'rgba(0,0,0,0.5)', display: 'flex', justifyContent: 'center', alignItems: 'center', zIndex: 1000
    }}>
      <div style={{ backgroundColor: 'white', padding: '20px', borderRadius: '12px', width: '90%', maxWidth: '600px' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '10px' }}>
          <h3>Ubicación del Encuentro</h3>
          <button onClick={onClose} style={{ cursor: 'pointer', border: 'none', background: 'none', fontSize: '20px' }}>&times;</button>
        </div>
        
        <div style={{ height: '400px', width: '100%', borderRadius: '8px', overflow: 'hidden' }}>
          <MapContainer center={[lat, lng]} zoom={15} scrollWheelZoom={false} style={{ height: '100%', width: '100%' }}>
            <TileLayer url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png" />
            <Marker position={[lat, lng]}>
              <Popup>{direccion}</Popup>
            </Marker>
          </MapContainer>
        </div>
        
        <p style={{ marginTop: '10px', fontSize: '14px', color: '#666' }}>{direccion}</p>
      </div>
    </div>
  );
}