package com.arthas.cataloger.service;

import com.arthas.cataloger.model.Item;
import org.junit.jupiter.api.*;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SpreadsheetServiceTest {

    private SpreadsheetService service;
    private File tempFile;

    @BeforeEach
    void setup() throws IOException {
        service  = new SpreadsheetService();
        tempFile = File.createTempFile("test-catalog-", ".xlsx");
        tempFile.deleteOnExit();
    }

    @Test
    void deveExportarArquivoNaoVazioComListaVazia() throws IOException {
        service.exportToXlsx(List.of(), tempFile);

        assertTrue(tempFile.exists(), "Arquivo deve existir após export");
        assertTrue(tempFile.length() > 0, "Arquivo deve conter ao menos o cabeçalho");
    }

    @Test
    void deveExportarEReimportarItensComFidelidade() throws IOException {
        List<Item> original = List.of(
            item("Livro Drácula",       "Livros",  "Clássico do terror",   "Excelente", LocalDate.of(2023, 6, 10), 45.0,  "Bram Stoker"),
            item("Funko Pop Pennywise", "Funko",   "IT o palhaço",         "Muito Bom", LocalDate.of(2024, 1,  5), 249.0, "Edição limitada"),
            item("HQ Sandman Vol.1",    "HQ",      "Neil Gaiman",          "Bom",       null,                      89.90, "")
        );

        service.exportToXlsx(original, tempFile);
        List<Item> imported = service.importFromXlsx(tempFile);

        assertEquals(3, imported.size(), "Deve importar os 3 itens exportados");
        assertEquals("Livro Drácula",       imported.get(0).getName());
        assertEquals("Funko Pop Pennywise", imported.get(1).getName());
        assertEquals("HQ Sandman Vol.1",    imported.get(2).getName());
        assertEquals(45.0,  imported.get(0).getValue(), 0.001);
        assertEquals(249.0, imported.get(1).getValue(), 0.001);
        assertEquals(LocalDate.of(2023, 6, 10), imported.get(0).getAcquisitionDate());
    }

    @Test
    void deveIgnorarLinhasComNomeVazio() throws IOException {
        // apenas 1 item válido; a segunda linha ficará vazia no arquivo de origem
        service.exportToXlsx(List.of(item("Item Válido", "Cat", "", "Bom", null, 10.0, "")), tempFile);

        List<Item> imported = service.importFromXlsx(tempFile);

        assertEquals(1, imported.size(), "Linha vazia não deve gerar item");
    }

    @Test
    void devePreservarCategoriaCondicaoEObservacoes() throws IOException {
        Item original = item("Action Figure Spawn", "Action Figure", "Todd McFarlane", "Excelente",
                LocalDate.of(2022, 11, 20), 350.0, "Na caixa original");

        service.exportToXlsx(List.of(original), tempFile);
        Item imported = service.importFromXlsx(tempFile).get(0);

        assertEquals("Action Figure",    imported.getCategory());
        assertEquals("Excelente",        imported.getCondition());
        assertEquals("Na caixa original",imported.getNotes());
        assertEquals("Todd McFarlane",   imported.getDescription());
    }

    // --- helpers ---

    private Item item(String name, String category, String description, String condition,
                      LocalDate date, double value, String notes) {
        Item i = new Item();
        i.setName(name);
        i.setCategory(category);
        i.setDescription(description);
        i.setCondition(condition);
        i.setAcquisitionDate(date);
        i.setValue(value);
        i.setNotes(notes);
        return i;
    }
}
