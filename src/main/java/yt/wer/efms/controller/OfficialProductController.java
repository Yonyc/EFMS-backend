package yt.wer.efms.controller;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import yt.wer.efms.service.OfficialProductService;

@RestController
public class OfficialProductController {
    private final OfficialProductService officialProductService;

    public OfficialProductController(OfficialProductService officialProductService) {
        this.officialProductService = officialProductService;
    }

    @GetMapping("/products/official")
    public ResponseEntity<?> listOfficialProducts(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String decisionCode,
            @RequestParam(required = false) String userGroupCode,
            @RequestParam(required = false) String productTypeCode,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        if (page == null) page = 0;
        int pageSize = size != null ? size : 25;
        return ResponseEntity.ok(
                officialProductService.listOfficialProducts(query, decisionCode, userGroupCode, productTypeCode,
                        PageRequest.of(page, pageSize)));
    }

    @GetMapping("/products/official/meta")
    public ResponseEntity<?> getFilterMeta() {
        return ResponseEntity.ok(officialProductService.getFilterOptions());
    }
}
