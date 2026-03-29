// Punto de entrada del frontend: monta la app React dentro del nodo root del HTML.
import ReactDOM from "react-dom/client";
import { BrowserRouter } from "react-router-dom";
import App from "./app/App";

// BrowserRouter habilita la navegacion por rutas sin recargar toda la pagina.
ReactDOM.createRoot(document.getElementById("root")!).render(
  <BrowserRouter>
    <App />
  </BrowserRouter>
);
