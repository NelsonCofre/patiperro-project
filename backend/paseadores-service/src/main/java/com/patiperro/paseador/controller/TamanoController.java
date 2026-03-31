package com.patiperro.paseador.controller;

import com.patiperro.paseador.repository.TamanoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/paseadores/public")
@RequiredArgsConstructor
public class TamanoController {

    private final TamanoRepository tamanoRepository;

    @GetMapping("/tamanos")
    public ResponseEntity<List<Map<String, Object>>> listar() {
        List<Map<String, Object>> data = tamanoRepository.findAll().stream()
                .sorted(Comparator.comparing(t -> t.getId() == null ? Long.MAX_VALUE : t.getId()))
                .map(t -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("id", t.getId());
                    item.put("nombre", t.getNombre());
                    item.put("descripcion", t.getDescripcion());
                    return item;
                })
                .toList();
        return ResponseEntity.ok(data);
    }
}
