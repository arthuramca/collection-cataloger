package com.arthas.cataloger.repository;

import com.arthas.cataloger.model.Item;
import org.junit.jupiter.api.*;

import java.sql.*;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ItemRepositoryTest {

    private static Connection connection;
    private ItemRepository repository;

    @BeforeAll
    static void setupInMemoryDatabase() throws SQLException {
        // banco em memória: isolado, sem efeito colateral entre execuções de teste
        connection = DriverManager.getConnection("jdbc:sqlite::memory:");
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("""
                    CREATE TABLE items (
                        id               INTEGER PRIMARY KEY AUTOINCREMENT,
                        name             TEXT    NOT NULL,
                        category         TEXT    DEFAULT '',
                        description      TEXT    DEFAULT '',
                        condition        TEXT    DEFAULT '',
                        acquisition_date TEXT,
                        value            REAL    DEFAULT 0.0,
                        notes            TEXT    DEFAULT ''
                    )
                    """);
        }
    }

    @BeforeEach
    void setup() {
        repository = new ItemRepository(connection);
    }

    @AfterEach
    void limparTabela() throws SQLException {
        connection.createStatement().execute("DELETE FROM items");
    }

    @AfterAll
    static void fecharConexao() throws SQLException {
        connection.close();
    }

    @Test
    @Order(1)
    void deveSalvarItemEAtribuirIdGerado() throws SQLException {
        Item item = new Item("Funko Batman", "Funko Pop", "Edição especial", "Excelente",
                LocalDate.of(2024, 3, 10), 189.90, "Box lacrado");

        Item saved = repository.save(item);

        assertTrue(saved.getId() > 0, "ID deve ser gerado após inserção");
        assertEquals("Funko Batman", saved.getName());
    }

    @Test
    @Order(2)
    void deveBuscarTodosOsItensOrdenadosPorNome() throws SQLException {
        repository.save(new Item("Zorro", "Livros", "", "Bom", null, 0, ""));
        repository.save(new Item("Aragorn", "Action Figure", "", "Excelente", null, 0, ""));
        repository.save(new Item("Batman", "Funko", "", "Muito Bom", null, 0, ""));

        List<Item> all = repository.findAll();

        assertEquals(3, all.size());
        assertEquals("Aragorn", all.get(0).getName());
        assertEquals("Batman",  all.get(1).getName());
        assertEquals("Zorro",   all.get(2).getName());
    }

    @Test
    @Order(3)
    void deveAtualizarItemExistente() throws SQLException {
        Item item = new Item("Item Original", "Cat", "", "Bom", null, 50.0, "");
        repository.save(item);

        item.setName("Item Atualizado");
        item.setValue(299.0);
        repository.save(item);

        List<Item> all = repository.findAll();
        assertEquals(1, all.size(), "Não deve criar duplicata ao atualizar");
        assertEquals("Item Atualizado", all.get(0).getName());
        assertEquals(299.0, all.get(0).getValue(), 0.001);
    }

    @Test
    @Order(4)
    void deveDeletarItemPorId() throws SQLException {
        Item item = new Item("Para deletar", "Cat", "", "Bom", null, 0, "");
        repository.save(item);

        repository.delete(item.getId());

        assertTrue(repository.findAll().isEmpty(), "Lista deve estar vazia após deletar");
    }

    @Test
    @Order(5)
    void deveBuscarPorNomeCategoriaeDescricao() throws SQLException {
        repository.save(new Item("Funko Pop Pennywise", "Funko", "IT o palhaço", "Excelente", null, 250.0, ""));
        repository.save(new Item("Funko Pop Joker",    "Funko", "Batman villain", "Muito Bom",null, 220.0, ""));
        repository.save(new Item("Livro Drácula",      "Livros", "Clássico terror","Bom",     null, 45.0,  ""));

        assertEquals(2, repository.search("Funko").size(), "Busca por categoria");
        assertEquals(1, repository.search("Drácula").size(), "Busca por nome");
        assertEquals(1, repository.search("Clássico").size(), "Busca por descrição");
        assertEquals(0, repository.search("XYZ999").size(), "Busca sem resultado");
    }

    @Test
    @Order(6)
    void devePersistirDataDeAquisicaoCorretamente() throws SQLException {
        LocalDate data = LocalDate.of(2023, 12, 25);
        repository.save(new Item("Presente de Natal", "Geral", "", "Excelente", data, 0, ""));

        Item recovered = repository.findAll().get(0);
        assertEquals(data, recovered.getAcquisitionDate());
    }

    @Test
    @Order(7)
    void deveRetornarListaVaziaQuandoNaoHaItens() throws SQLException {
        assertTrue(repository.findAll().isEmpty());
    }
}
