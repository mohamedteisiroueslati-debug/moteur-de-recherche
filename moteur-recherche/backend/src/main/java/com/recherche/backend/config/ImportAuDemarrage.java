package com.recherche.backend.config;

import com.recherche.backend.service.IndexationService;
import com.recherche.backend.service.PdfImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Au démarrage : ① collecte des PDF du dossier d'import, ②③ import,
 * puis ④→⑦ indexation si de nouveaux documents ont été ajoutés.
 */
@Component
@RequiredArgsConstructor
public class ImportAuDemarrage implements CommandLineRunner {

    private final PdfImportService importService;
    private final IndexationService indexationService;

    @Value("${app.import.dossier}")
    private String dossierImport;

    @Override
    public void run(String... args) throws Exception {
        int nouveaux = 0;
        Path dossier = Path.of(dossierImport);

        if (Files.isDirectory(dossier)) {
            List<Path> pdfs;
            try (var fichiers = Files.list(dossier)) {
                pdfs = fichiers.filter(f -> f.toString().toLowerCase().endsWith(".pdf")).toList();
            }
            for (Path p : pdfs) {
                try {
                    if (importService.importer(p.getFileName().toString(), Files.readAllBytes(p)).isPresent()) {
                        nouveaux++;
                        System.out.println("Importé : " + p.getFileName());
                    }
                } catch (Exception e) {
                    // un PDF illisible ne bloque pas les autres
                    System.err.println("Échec de l'import de " + p.getFileName() + " : " + e.getMessage());
                }
            }
        } else {
            System.out.println("Dossier d'import introuvable : " + dossier);
        }

        if (nouveaux > 0 || indexationService.indexVide()) {
            indexationService.reconstruire();
        }
    }
}
