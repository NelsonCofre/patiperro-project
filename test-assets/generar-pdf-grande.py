"""Genera un PDF válido (>5 MB) para probar rechazo en verificación paseador."""
import os

OUT_DIR = os.path.dirname(os.path.abspath(__file__))
OUT_PATH = os.path.join(OUT_DIR, "verificacion-caso2-mas-de-5mb.pdf")
TARGET_BYTES = 6 * 1024 * 1024  # 6 MB

# Contenido de stream PDF (texto dibujable repetido)
line = b"0 0 0 rg BT /F1 12 Tf 50 750 Td (CU2 PatiPerro - PDF prueba >5MB) Tj ET "
padding = line * (TARGET_BYTES // len(line) + 1)
padding = padding[:TARGET_BYTES]

objects = []
objects.append(b"1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n")
objects.append(b"2 0 obj\n<< /Type /Pages /Kids [3 0 R] /Count 1 >>\nendobj\n")
objects.append(
    b"3 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] "
    b"/Contents 4 0 R /Resources << /Font << /F1 5 0 R >> >> >>\nendobj\n"
)
stream_header = f"4 0 obj\n<< /Length {len(padding)} >>\nstream\n".encode("ascii")
stream_footer = b"\nendstream\nendobj\n"
objects.append(stream_header + padding + stream_footer)
objects.append(
    b"5 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>\nendobj\n"
)

body = b"%PDF-1.4\n"
offsets = [0]
for obj in objects:
    offsets.append(len(body))
    body += obj

xref_pos = len(body)
xref = b"xref\n0 6\n"
xref += b"0000000000 65535 f \n"
for off in offsets[1:]:
    xref += f"{off:010d} 00000 n \n".encode("ascii")

trailer = (
    b"trailer\n<< /Size 6 /Root 1 0 R >>\n"
    + b"startxref\n"
    + str(xref_pos).encode("ascii")
    + b"\n%%EOF\n"
)

with open(OUT_PATH, "wb") as f:
    f.write(body)
    f.write(xref)
    f.write(trailer)

size = os.path.getsize(OUT_PATH)
print(f"Creado: {OUT_PATH}")
print(f"Tamanio: {size / (1024 * 1024):.2f} MB ({size:,} bytes)")
