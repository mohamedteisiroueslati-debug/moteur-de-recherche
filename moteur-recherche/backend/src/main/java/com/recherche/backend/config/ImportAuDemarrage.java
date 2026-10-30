package com.recherche.backend.config;

import com.recherche.backend.service.PdfImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;

@Component
@RequiredArgsConstructor
public class ImportAuDemarrage implements CommandLineRunner {

    private final PdfImportService service;

    @Override
    public void run(String... args) throws Exception {
        Path dossier = Path.of("D:/pdfTest");
        if (!Files.exists(dossier)) return;
        try (var fichiers = Files.list(dossier)) {
            for (Path p : fichiers.filter(f -> f.toString().toLowerCase().endsWith(".pdf")).toList()) {
                service.importer(p.getFileName().toString(), Files.readAllBytes(p));
                System.out.println("Importé : " + p.getFileName());
            }
        }
    }
}