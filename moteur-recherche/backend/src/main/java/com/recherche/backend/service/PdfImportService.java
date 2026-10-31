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
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

/**
 * Étapes ② et ③ du pipeline : extraction du texte puis stockage du document.
 * L'indexation (étapes ④ à ⑦) est faite ensuite par IndexationService.
 */
@Service
@RequiredArgsConstructor
public class PdfImportService {

    private final DocPdfRepository docRepo;
    private final PagePdfRepository pageRepo;
    private final GridFsTemplate gridFs;

    /**
     * Importe un PDF.
     * @return le document créé, ou Optional.empty() s'il était déjà importé
     */
    public Optional<DocPdf> importer(String nomFichier, byte[] octets) throws Exception {
        // 1. empreinte SHA-256 : évite d'importer deux fois le même fichier
        String empreinte = HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(octets));

        if (docRepo.findByEmpreinte(empreinte).isPresent()) {
            System.out.println("Déjà importé : " + nomFichier);
            return Optional.empty();
        }

        // 2. extraction du texte page par page, en mémoire, AVANT toute sauvegarde :
        //    si le PDF est illisible, rien n'est écrit dans la base
        List<String> textes = new ArrayList<>();
        try (PDDocument pdf = Loader.loadPDF(octets)) {
            PDFTextStripper stripper = new PDFTextStripper();
            for (int i = 1; i <= pdf.getNumberOfPages(); i++) {
                stripper.setStartPage(i);
                stripper.setEndPage(i);
                textes.add(stripper.getText(pdf).trim());
            }
        }

        // 3. stockage : PDF original (GridFS), document, pages
        ObjectId fichierId = gridFs.store(
                new ByteArrayInputStream(octets), nomFichier, "application/pdf");
        DocPdf doc = new DocPdf();
        try {
            doc.setNomFichier(nomFichier);
            doc.setEmpreinte(empreinte);
            doc.setNombrePages(textes.size());
            doc.setFichierId(fichierId.toString());
            doc.setDateIndexation(LocalDateTime.now());
            doc = docRepo.save(doc);

            List<PagePdf> pages = new ArrayList<>();
            for (int i = 0; i < textes.size(); i++) {
                if (textes.get(i).isBlank()) continue;   // page vide ou scannée (pas d'OCR)
                PagePdf page = new PagePdf();
                page.setDocId(doc.getId());
                page.setNomFichier(nomFichier);
                page.setNumeroPage(i + 1);
                page.setContenu(textes.get(i));
                pages.add(page);
            }
            pageRepo.saveAll(pages);
            return Optional.of(doc);

        } catch (Exception e) {
            // annulation : on supprime ce qui a déjà été écrit,
            // sinon l'empreinte bloquerait toute nouvelle tentative
            gridFs.delete(Query.query(Criteria.where("_id").is(fichierId)));
            if (doc.getId() != null) {
                pageRepo.deleteByDocId(doc.getId());
                docRepo.deleteById(doc.getId());
            }
            throw e;
        }
    }
}
