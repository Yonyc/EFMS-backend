package yt.wer.efms.migration;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import yt.wer.efms.model.OperationType;
import yt.wer.efms.model.ProductType;
import yt.wer.efms.model.Unit;
import yt.wer.efms.repository.OperationTypeRepository;
import yt.wer.efms.repository.ProductTypeRepository;
import yt.wer.efms.repository.UnitRepository;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class ReferenceDataInitializer implements CommandLineRunner {

    private static final List<String> STANDARD_UNITS = List.of(
            // Mass
            "kg", "g", "t", "mg",
            // Volume
            "L", "mL",
            // Area / length
            "ha", "m²", "m",
            // Density / seeding rates
            "graines/m²", "plantes/ha", "plantes/m²", "kg/ha", "L/ha", "g/L",
            // Concentration
            "%", "mg/kg", "g/kg",
            // Generic
            "dose", "unité", "U/ha");

    private static final List<String> STANDARD_OPERATION_TYPES = List.of(
            "Semis",
            "Traitement phytosanitaire",
            "Fertilisation",
            "Récolte",
            "Travail du sol",
            "Irrigation",
            "Taille",
            "Surveillance",
            "Traitement fongique",
            "Traitement insecticide");

    /** Default global product types. Only "Semences" is a seed type. */
    private static final List<String> STANDARD_PRODUCT_TYPES = List.of(
            "Engrais",
            "Produit phytosanitaire",
            "Amendement",
            "Herbicide",
            "Fongicide",
            "Insecticide",
            "Autre");

    private final UnitRepository unitRepository;
    private final OperationTypeRepository operationTypeRepository;
    private final ProductTypeRepository productTypeRepository;

    public ReferenceDataInitializer(UnitRepository unitRepository,
            OperationTypeRepository operationTypeRepository,
            ProductTypeRepository productTypeRepository) {
        this.unitRepository = unitRepository;
        this.operationTypeRepository = operationTypeRepository;
        this.productTypeRepository = productTypeRepository;
    }

    @Override
    public void run(String... args) {
        seedUnits();
        seedOperationTypes();
        seedProductTypes();
    }

    private void seedProductTypes() {
        if (!productTypeRepository.existsByNameAndFarmIsNull("Semences")) {
            ProductType seed = new ProductType();
            seed.setName("Semences");
            seed.setSeedType(true);
            seed.setCreatedAt(LocalDateTime.now());
            seed.setModifiedAt(LocalDateTime.now());
            productTypeRepository.save(seed);
        }
        for (String name : STANDARD_PRODUCT_TYPES) {
            if (!productTypeRepository.existsByNameAndFarmIsNull(name)) {
                ProductType type = new ProductType();
                type.setName(name);
                type.setCreatedAt(LocalDateTime.now());
                type.setModifiedAt(LocalDateTime.now());
                productTypeRepository.save(type);
            }
        }
    }

    private void seedUnits() {
        for (String value : STANDARD_UNITS) {
            if (!unitRepository.existsByValueAndFarmIsNull(value)) {
                Unit unit = new Unit();
                unit.setValue(value);
                unit.setCreatedAt(LocalDateTime.now());
                unit.setModifiedAt(LocalDateTime.now());
                unitRepository.save(unit);
            }
        }
    }

    private void seedOperationTypes() {
        for (String name : STANDARD_OPERATION_TYPES) {
            if (!operationTypeRepository.existsByNameAndFarmIsNull(name)) {
                OperationType type = new OperationType();
                type.setName(name);
                type.setCreatedAt(LocalDateTime.now());
                type.setModifiedAt(LocalDateTime.now());
                operationTypeRepository.save(type);
            }
        }
    }
}
