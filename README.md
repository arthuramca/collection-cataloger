# Catalogador de Coleções

Aplicação desktop em **JavaFX** para catalogar itens de qualquer tipo de coleção (livros, Funko Pops, HQs, action figures, etc.) com persistência local via **SQLite** e suporte a **import/export de planilhas `.xlsx`**.

---

## Funcionalidades

- **Cadastro completo** de itens: nome, categoria, descrição, condição, data de aquisição, valor e observações
- **Listagem** com busca por nome, categoria e descrição
- **Edição** direta (clique duplo na linha ou botão Editar)
- **Exclusão** com confirmação
- **Export para `.xlsx`** — gera planilha formatada com todos os itens da coleção
- **Import de `.xlsx`** — importa novos itens a partir de uma planilha existente
- **Persistência automática** em SQLite local (sem necessidade de instalar banco de dados)

---

## Pré-requisitos

| Software  | Versão mínima | Download |
|-----------|--------------|----------|
| Java JDK  | 17           | [Adoptium (Temurin)](https://adoptium.net/) |
| Apache Maven | 3.9       | [maven.apache.org](https://maven.apache.org/download.cgi) |

> **Windows:** Após instalar o JDK, confirme com `java -version` no terminal.

---

## Como rodar

```bash
# 1. Clone o repositório
git clone https://github.com/SEU_USUARIO/collection-cataloger.git
cd collection-cataloger

# 2. Execute a aplicação
mvn javafx:run
```

---

## Como buildar um JAR executável

```bash
mvn clean package
java -jar target/collection-cataloger-1.0.0-shaded.jar
```

> O JAR gerado inclui todas as dependências (fat JAR via Maven Shade).

---

## Rodar os testes

```bash
mvn test
```

Os testes utilizam banco SQLite em memória — sem efeito colateral no banco local.

---

## Estrutura do projeto

```
collection-cataloger/
├── src/
│   ├── main/
│   │   ├── java/com/arthas/cataloger/
│   │   │   ├── App.java                        # Ponto de entrada JavaFX
│   │   │   ├── Launcher.java                   # Wrapper necessário para JAR executável
│   │   │   ├── model/
│   │   │   │   └── Item.java                   # Entidade de domínio
│   │   │   ├── repository/
│   │   │   │   ├── DatabaseManager.java        # Conexão e schema SQLite
│   │   │   │   └── ItemRepository.java         # CRUD via JDBC
│   │   │   ├── service/
│   │   │   │   ├── ItemService.java            # Regras de negócio
│   │   │   │   └── SpreadsheetService.java     # Import/export .xlsx (Apache POI)
│   │   │   └── controller/
│   │   │       ├── MainController.java         # Controlador da tela principal
│   │   │       └── ItemDialog.java             # Diálogo de cadastro/edição
│   │   └── resources/com/arthas/cataloger/
│   │       ├── main-view.fxml                  # Layout declarativo da tela
│   │       └── styles.css                      # Estilo visual da aplicação
│   └── test/
│       └── java/com/arthas/cataloger/
│           ├── repository/ItemRepositoryTest.java
│           └── service/SpreadsheetServiceTest.java
├── pom.xml
├── .gitignore
└── README.md
```

---

## Banco de dados

O banco SQLite é criado automaticamente em `~/collection-cataloger/catalog.db` na primeira execução. Nenhuma configuração adicional é necessária.

**Schema da tabela `items`:**

| Coluna            | Tipo    | Descrição                        |
|-------------------|---------|----------------------------------|
| `id`              | INTEGER | Chave primária (auto)            |
| `name`            | TEXT    | Nome do item (obrigatório)       |
| `category`        | TEXT    | Categoria (Livros, Funko, etc.)  |
| `description`     | TEXT    | Descrição livre                  |
| `condition`       | TEXT    | Estado de conservação            |
| `acquisition_date`| TEXT    | Data no formato ISO (YYYY-MM-DD) |
| `value`           | REAL    | Valor estimado em R$             |
| `notes`           | TEXT    | Observações adicionais           |

---

## Import/Export via planilha

### Export
Clique em **⬆ Exportar .xlsx** para salvar todos os itens visíveis em uma planilha Excel formatada.

### Import
Clique em **⬇ Importar .xlsx** para importar itens de uma planilha. A primeira linha deve ser o cabeçalho (será ignorada). As colunas esperadas são:

| Coluna | Campo         |
|--------|---------------|
| B      | Nome          |
| C      | Categoria     |
| D      | Descrição     |
| E      | Condição      |
| F      | Data (ISO)    |
| G      | Valor         |
| H      | Observações   |

> **Dica:** exporte primeiro para ver o formato esperado antes de criar uma planilha de importação.

---

## Tecnologias

| Biblioteca       | Versão  | Uso                              |
|------------------|---------|----------------------------------|
| JavaFX           | 21.0.2  | Interface gráfica                |
| SQLite JDBC      | 3.45.1  | Persistência local               |
| Apache POI       | 5.2.5   | Leitura e geração de `.xlsx`     |
| JUnit 5          | 5.10.2  | Testes automatizados             |
| Mockito          | 5.10.0  | Mocks em testes unitários        |

---

## Licença

MIT — livre para uso pessoal e comercial.
