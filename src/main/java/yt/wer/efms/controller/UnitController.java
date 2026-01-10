package yt.wer.efms.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import yt.wer.efms.dto.UnitDto;
import yt.wer.efms.repository.UnitRepository;

import java.util.List;
import java.util.stream.Collectors;

@RestController
public class UnitController {
    private final UnitRepository unitRepository;

    public UnitController(UnitRepository unitRepository) {
        this.unitRepository = unitRepository;
    }

    @GetMapping("/units")
    public ResponseEntity<List<UnitDto>> listUnits() {
        List<UnitDto> units = unitRepository.findAll().stream()
                .map(u -> new UnitDto(u.getId(), u.getValue()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(units);
    }
}
