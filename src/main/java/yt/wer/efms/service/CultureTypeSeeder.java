package yt.wer.efms.service;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import yt.wer.efms.model.CultureType;
import yt.wer.efms.repository.CultureTypeRepository;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Component
public class CultureTypeSeeder {

    private final CultureTypeRepository repo;

    public CultureTypeSeeder(CultureTypeRepository repo) {
        this.repo = repo;
    }

    private static final List<String[]> ENTRIES = Arrays.asList(
            new String[] { "100", "Froment d'hiver", "Wintertarwe", "cereals" },
            new String[] { "101", "Froment de printemps", "Lentetarwe", "cereals" },
            new String[] { "102", "Épeautre", "Spelt", "cereals" },
            new String[] { "103", "Froment dur (blé durum)", "Durumtarwe", "cereals" },
            new String[] { "110", "Seigle d'hiver", "Winterrogge", "cereals" },
            new String[] { "111", "Seigle de printemps", "Lenteogge", "cereals" },
            new String[] { "112", "Triticale", "Triticale", "cereals" },
            new String[] { "120", "Orge d'hiver", "Wintergerst", "cereals" },
            new String[] { "121", "Orge de printemps", "Lentegerst", "cereals" },
            new String[] { "122", "Escourgeon", "Wintergerst (6-rijen)", "cereals" },
            new String[] { "130", "Avoine d'été", "Zomerhaver", "cereals" },
            new String[] { "131", "Avoine d'hiver", "Winterhaver", "cereals" },
            new String[] { "140", "Maïs grain", "Korrelmaïs", "cereals" },
            new String[] { "141", "Maïs ensilage", "Silomais", "cereals" },
            new String[] { "142", "Maïs doux", "Suikermaïs", "cereals" },
            new String[] { "150", "Sorgho grain", "Graansorghum", "cereals" },
            new String[] { "151", "Sorgho ensilage", "Silsorghum", "cereals" },
            new String[] { "160", "Sarrasin", "Boekweit", "cereals" },
            new String[] { "200", "Betterave sucrière", "Suikerbiet", "industrial" },
            new String[] { "201", "Chicorée à café", "Cichorei", "industrial" },
            new String[] { "210", "Colza d'hiver", "Winterkoolzaad", "industrial" },
            new String[] { "211", "Colza de printemps", "Lentekoolzaad", "industrial" },
            new String[] { "212", "Tournesol", "Zonnebloem", "industrial" },
            new String[] { "213", "Soja", "Soja", "industrial" },
            new String[] { "220", "Lin textile", "Vezelvlas", "industrial" },
            new String[] { "221", "Lin oléagineux", "Olievlas", "industrial" },
            new String[] { "222", "Chanvre industriel", "Industriehennep", "industrial" },
            new String[] { "230", "Tabac", "Tabak", "industrial" },
            new String[] { "231", "Houblon", "Hop", "industrial" },
            new String[] { "240", "Miscanthus", "Miscanthus", "industrial" },
            new String[] { "241", "Saule à courte rotation", "Wilg (KOR)", "industrial" },
            new String[] { "242", "Peuplier à courte rotation", "Populier (KOR)", "industrial" },
            new String[] { "300", "Pomme de terre féculière", "Zetmeelaardappel", "potatoes" },
            new String[] { "301", "Pomme de terre consommation", "Consumptieaardappel", "potatoes" },
            new String[] { "302", "Pomme de terre de semence", "Pootaardappel", "potatoes" },
            new String[] { "303", "Pomme de terre primeur", "Vroege aardappel", "potatoes" },
            new String[] { "400", "Légumes de plein champ", "Groenten open lucht", "vegetables" },
            new String[] { "401", "Asperges", "Asperges", "vegetables" },
            new String[] { "402", "Chicons / Witloof", "Witloof", "vegetables" },
            new String[] { "403", "Poireaux", "Prei", "vegetables" },
            new String[] { "404", "Carottes", "Wortelen", "vegetables" },
            new String[] { "405", "Oignons", "Uien", "vegetables" },
            new String[] { "406", "Choux de Bruxelles", "Spruitjes", "vegetables" },
            new String[] { "407", "Chou-fleur", "Bloemkool", "vegetables" },
            new String[] { "408", "Brocolis", "Broccoli", "vegetables" },
            new String[] { "409", "Chou blanc / rouge", "Witte/rode kool", "vegetables" },
            new String[] { "410", "Épinards", "Spinazie", "vegetables" },
            new String[] { "411", "Haricots verts", "Snijbonen", "vegetables" },
            new String[] { "412", "Petits pois", "Doperwten", "vegetables" },
            new String[] { "413", "Céleri", "Selder", "vegetables" },
            new String[] { "414", "Tomates de plein champ", "Tomaten open lucht", "vegetables" },
            new String[] { "415", "Concombres", "Komkommers", "vegetables" },
            new String[] { "416", "Courgettes", "Courgettes", "vegetables" },
            new String[] { "417", "Betterave potagère", "Rode biet", "vegetables" },
            new String[] { "418", "Radis", "Radijzen", "vegetables" },
            new String[] { "419", "Navets", "Rapen", "vegetables" },
            new String[] { "420", "Endives de plein champ", "Cichorei (bladgroente)", "vegetables" },
            new String[] { "500", "Prairie permanente", "Blijvend grasland", "grasslands" },
            new String[] { "501", "Prairie temporaire ≤ 5 ans", "Tijdelijk grasland ≤ 5 jaar", "grasslands" },
            new String[] { "502", "Prairie temporaire > 5 ans", "Tijdelijk grasland > 5 jaar", "grasslands" },
            new String[] { "503", "Luzerne", "Luzerne", "grasslands" },
            new String[] { "504", "Trèfle", "Klaver", "grasslands" },
            new String[] { "505", "Mélange graminées-légumineuses", "Grassen-vlinderbloemigen", "grasslands" },
            new String[] { "506", "Fétuque", "Zwenkgras", "grasslands" },
            new String[] { "507", "Ray-grass", "Raaigras", "grasslands" },
            new String[] { "508", "Dactyle", "Kropaar", "grasslands" },
            new String[] { "509", "Betterave fourragère", "Voederbiet", "grasslands" },
            new String[] { "510", "Navet fourrager", "Voederraps", "grasslands" },
            new String[] { "600", "Pois protéagineux", "Erwten", "legumes" },
            new String[] { "601", "Féveroles d'hiver", "Winterveldbonen", "legumes" },
            new String[] { "602", "Féveroles de printemps", "Lentevaldbonen", "legumes" },
            new String[] { "603", "Lupins doux", "Zoete lupinen", "legumes" },
            new String[] { "604", "Vesces", "Wikken", "legumes" },
            new String[] { "605", "Pois de conserve", "Conservenerwten", "legumes" },
            new String[] { "700", "Fraises", "Aardbeien", "fruits" },
            new String[] { "701", "Pommes et poires", "Appelen en peren", "fruits" },
            new String[] { "702", "Cerises et prunes", "Kersen en pruimen", "fruits" },
            new String[] { "703", "Petits fruits (cassis, etc.)", "Kleinfruit", "fruits" },
            new String[] { "704", "Pépinières fruitières", "Boomkwekerijen", "fruits" },
            new String[] { "800", "Jachère nue", "Kaal braakland", "ecology" },
            new String[] { "801", "Jachère miellifère", "Bijenweide", "ecology" },
            new String[] { "802", "Bande aménagée", "Ingerichte strook", "ecology" },
            new String[] { "803", "Surface d'intérêt écologique", "Ecologisch aandachtsgebied", "ecology" },
            new String[] { "804", "MAEC - mesure agro-env.", "Agro-milieumaatregel", "ecology" },
            new String[] { "805", "Haie / zone tampon", "Haag / bufferzone", "ecology" },
            new String[] { "806", "Mare / fossé", "Poel / sloot", "ecology" },
            new String[] { "900", "Boisement (agroforesterie)", "Bebossing", "forestry" },
            new String[] { "901", "Taillis à courte rotation", "Korte omlooptijd", "forestry" },
            new String[] { "950", "Usage non agricole", "Niet-agrarisch gebruik", "other" });

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void seed() {
        if (repo.countByFarmIsNull() > 0)
            return; // already seeded

        LocalDateTime now = LocalDateTime.now();
        for (String[] row : ENTRIES) {
            CultureType ct = new CultureType();
            ct.setCode(row[0]);
            ct.setName(row[1]);
            ct.setNameNl(row[2]);
            ct.setCategory(row[3]);
            ct.setCreatedAt(now);
            ct.setModifiedAt(now);
            repo.save(ct);
        }
    }
}
