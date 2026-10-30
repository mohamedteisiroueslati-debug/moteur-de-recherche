package com.recherche.backend.service;

import com.recherche.backend.model.DocPdf;
import com.recherche.backend.model.PagePdf;
import com.recherche.backend.repository.DocPdfRepository;
import com.recherche.backend.repository.PagePdfRepository;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class PdfImportService {

    private final DocPdfRepository docRepo;
    private final PagePdfRepository pageRepo;
    private final GridFsTemplate gridFs;

    public DocPdf importer(String nomFichier, byte[] octets) throws Exception {
        // 1. calcul de l'empreinte
        String empreinte = HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(octets));

        // 2. doublon ? on s'arrête
        var existant = docRepo.findByEmpreinte(empreinte);
        if (existant.isPresent()) {
            System.out.println("Déjà importé : " + nomFichier);
            return existant.get();
        }

        try (PDDocument pdf = Loader.loadPDF(octets)) {
            // 3. sauvegarde du PDF original dans GridFS
            ObjectId fichierId = gridFs.store(
                    new ByteArrayInputStream(octets), nomFichier, "application/pdf");

            // 4. sauvegarde du document
            DocPdf doc = new DocPdf();
            doc.setNomFichier(nomFichier);
            doc.setEmpreinte(empreinte);
            doc.setNombrePages(pdf.getNumberOfPages());
            doc.setFichierId(fichierId.toString());
            doc.setDateIndexation(LocalDateTime.now());
            doc = docRepo.save(doc);

            // 5. extraction du texte page par page
            PDFTextStripper stripper = new PDFTextStripper();
            for (int i = 1; i <= pdf.getNumberOfPages(); i++) {
                stripper.setStartPage(i);
                stripper.setEndPage(i);
                PagePdf page = new PagePdf();
                page.setDocId(doc.getId());
                page.setNomFichier(nomFichier);
                page.setNumeroPage(i);
                page.setContenu(stripper.getText(pdf).trim());
                pageRepo.save(page);
            }
            return doc;
        }
    }
}