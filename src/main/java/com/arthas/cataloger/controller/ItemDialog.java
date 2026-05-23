package com.arthas.cataloger.controller;

import com.arthas.cataloger.model.Item;
import com.arthas.cataloger.service.BookLookupService;
import com.arthas.cataloger.service.PriceLookupService;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;

import java.awt.Desktop;
import java.io.File;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;

public class ItemDialog extends Dialog<Item> {

    private final TextField        nameField        = new TextField();
    private final TextField        categoryField    = new TextField();
    private final TextArea         descriptionArea  = new TextArea();
    private final ComboBox<String> conditionBox     = new ComboBox<>();
    private final DatePicker       datePicker       = new DatePicker();
    private final TextField        valueField       = new TextField("0,00");
    private final TextArea         notesArea        = new TextArea();
    private final ImageView        imagePreview     = new ImageView();
    private final Label            imagePathLabel   = new Label("Nenhuma foto selecionada");
    private String                 selectedImagePath = "";

    // Campos de livro
    private final TextField isbnField        = new TextField();
    private final TextField authorField      = new TextField();
    private final TextField publisherField   = new TextField();
    private final TextField yearField        = new TextField();
    private final TextField marketPriceField = new TextField("0,00");
    private final Label     lookupStatusLabel = new Label();

    public ItemDialog(Item item) {
        boolean isNew = item == null;
        setTitle(isNew ? "Novo Item" : "Editar Item");
        setHeaderText(isNew ? "Adicione um novo item à sua coleção" : "Edite os dados do item selecionado");

        ButtonType saveButtonType = new ButtonType("Salvar", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        conditionBox.getItems().addAll("Excelente", "Muito Bom", "Bom", "Regular", "Ruim");

        descriptionArea.setPrefRowCount(3);
        descriptionArea.setWrapText(true);
        notesArea.setPrefRowCount(2);
        notesArea.setWrapText(true);

        imagePreview.setFitWidth(80);
        imagePreview.setFitHeight(80);
        imagePreview.setPreserveRatio(true);
        imagePreview.setStyle("-fx-border-color: #bdc3c7; -fx-border-width: 1;");

        lookupStatusLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #7f8c8d;");

        GridPane grid = buildGrid();
        getDialogPane().setContent(grid);
        getDialogPane().setPrefWidth(560);

        if (!isNew) {
            populateFields(item);
        } else {
            conditionBox.setValue("Bom");
        }

        Button saveBtn = (Button) getDialogPane().lookupButton(saveButtonType);
        saveBtn.setDisable(true);
        nameField.textProperty().addListener((obs, old, val) -> saveBtn.setDisable(val.isBlank()));

        setResultConverter(btn -> btn == saveButtonType ? buildItem(item) : null);
    }

    private GridPane buildGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 20, 10, 20));

        nameField.setPromptText("Nome do item (obrigatório)");
        categoryField.setPromptText("Ex: Livros, Funko, Quadrinhos...");
        isbnField.setPromptText("Ex: 9788532511010");
        authorField.setPromptText("Nome do autor");
        publisherField.setPromptText("Nome da editora");
        yearField.setPromptText("Ex: 2020");
        marketPriceField.setPromptText("0,00");

        GridPane.setHgrow(nameField, Priority.ALWAYS);
        GridPane.setHgrow(categoryField, Priority.ALWAYS);
        GridPane.setHgrow(authorField, Priority.ALWAYS);
        GridPane.setHgrow(publisherField, Priority.ALWAYS);

        // Botão auto-preencher via ISBN
        Button autoFillBtn = new Button("🔍 Auto-preencher");
        autoFillBtn.setStyle("-fx-background-color: #2980b9; -fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 4;");
        autoFillBtn.setOnAction(e -> doIsbnLookup(autoFillBtn));
        HBox isbnBox = new HBox(8, isbnField, autoFillBtn);
        HBox.setHgrow(isbnField, Priority.ALWAYS);
        isbnBox.setAlignment(Pos.CENTER_LEFT);

        // Autor + Ano na mesma linha
        HBox authorYearBox = new HBox(10, authorField, new Label("Ano:"), yearField);
        HBox.setHgrow(authorField, Priority.ALWAYS);
        yearField.setMaxWidth(80);

        // Botões de preço: Mercado Livre + Amazon
        Button priceBtn = new Button("💰 Mercado Livre");
        priceBtn.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 4;");
        priceBtn.setOnAction(e -> doPriceLookup(priceBtn));

        Button amazonBtn = new Button("🛒 Amazon");
        amazonBtn.setStyle("-fx-background-color: #e67e22; -fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 4;");
        amazonBtn.setOnAction(e -> openAmazonSearch());

        HBox priceBox = new HBox(8, marketPriceField, priceBtn, amazonBtn);
        HBox.setHgrow(marketPriceField, Priority.ALWAYS);
        priceBox.setAlignment(Pos.CENTER_LEFT);

        // Botões de foto
        Button selectImageBtn = new Button("Selecionar foto...");
        selectImageBtn.setOnAction(e -> selectImage());
        Button clearImageBtn = new Button("Remover");
        clearImageBtn.setOnAction(e -> clearImage());
        HBox imageButtons = new HBox(6, selectImageBtn, clearImageBtn);
        imagePathLabel.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 11px;");
        imagePathLabel.setMaxWidth(280);
        imagePathLabel.setWrapText(false);
        imagePathLabel.setEllipsisString("...");
        HBox imageBox = new HBox(10, imagePreview, new VBox(4, imageButtons, imagePathLabel));
        imageBox.setAlignment(Pos.CENTER_LEFT);

        int row = 0;
        grid.add(label("Nome *"),          0, row); grid.add(nameField,       1, row++);
        grid.add(label("ISBN"),            0, row); grid.add(isbnBox,         1, row++);
        grid.add(label("Autor / Ano"),     0, row); grid.add(authorYearBox,   1, row++);
        grid.add(label("Editora"),         0, row); grid.add(publisherField,  1, row++);
        grid.add(label("Categoria"),       0, row); grid.add(categoryField,   1, row++);
        grid.add(label("Descrição"),       0, row); grid.add(descriptionArea, 1, row++);
        grid.add(label("Condição"),        0, row); grid.add(conditionBox,    1, row++);
        grid.add(label("Aquisição"),       0, row); grid.add(datePicker,      1, row++);
        grid.add(label("Valor (R$)"),      0, row); grid.add(valueField,      1, row++);
        grid.add(label("Preço ML (R$)"),   0, row); grid.add(priceBox,        1, row++);
        grid.add(new Label(),              0, row); grid.add(lookupStatusLabel, 1, row++);
        grid.add(label("Observações"),     0, row); grid.add(notesArea,       1, row++);
        grid.add(label("Foto"),            0, row); grid.add(imageBox,        1, row);

        return grid;
    }

    private void doIsbnLookup(Button btn) {
        String isbn = isbnField.getText().trim();
        if (isbn.isBlank()) {
            lookupStatusLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 11px;");
            lookupStatusLabel.setText("Digite um ISBN antes de buscar.");
            return;
        }

        btn.setDisable(true);
        lookupStatusLabel.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 11px;");
        lookupStatusLabel.setText("Buscando dados na Open Library...");

        Task<BookLookupService.BookInfo> task = new Task<>() {
            @Override
            protected BookLookupService.BookInfo call() throws Exception {
                return new BookLookupService().lookup(isbn);
            }
        };

        task.setOnSucceeded(e -> {
            BookLookupService.BookInfo info = task.getValue();
            if (!info.title.isBlank() && nameField.getText().isBlank()) {
                nameField.setText(info.title);
            }
            if (!info.author.isBlank())    authorField.setText(info.author);
            if (!info.publisher.isBlank()) publisherField.setText(info.publisher);
            if (!info.publishYear.isBlank()) yearField.setText(info.publishYear);
            lookupStatusLabel.setStyle("-fx-text-fill: #27ae60; -fx-font-size: 11px;");
            lookupStatusLabel.setText("Dados preenchidos com sucesso!");
            btn.setDisable(false);
        });

        task.setOnFailed(e -> {
            lookupStatusLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 11px;");
            lookupStatusLabel.setText("Erro: " + task.getException().getMessage());
            btn.setDisable(false);
        });

        new Thread(task, "isbn-lookup").start();
    }

    private void doPriceLookup(Button btn) {
        String query = nameField.getText().trim();
        if (query.isBlank()) {
            lookupStatusLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 11px;");
            lookupStatusLabel.setText("Preencha o Nome do item antes de buscar o preço.");
            return;
        }

        btn.setDisable(true);
        lookupStatusLabel.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 11px;");
        lookupStatusLabel.setText("Consultando preços no Mercado Livre...");

        Task<PriceLookupService.PriceResult> task = new Task<>() {
            @Override
            protected PriceLookupService.PriceResult call() throws Exception {
                return new PriceLookupService().fetchPrice(query);
            }
        };

        task.setOnSucceeded(e -> {
            PriceLookupService.PriceResult result = task.getValue();
            marketPriceField.setText(String.format("%.2f", result.average).replace('.', ','));
            lookupStatusLabel.setStyle("-fx-text-fill: #27ae60; -fx-font-size: 11px;");
            lookupStatusLabel.setText(String.format(
                "Média: R$ %.2f | Menor: R$ %.2f | %d anúncios",
                result.average, result.lowest, result.sampleSize));
            btn.setDisable(false);
        });

        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            if (ex instanceof PriceLookupService.PriceApiAuthException authEx) {
                lookupStatusLabel.setStyle("-fx-text-fill: #e67e22; -fx-font-size: 11px;");
                lookupStatusLabel.setText("Abrindo busca no navegador...");
                openBrowser(authEx.browserUrl);
            } else {
                lookupStatusLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 11px;");
                lookupStatusLabel.setText("Erro: " + ex.getMessage());
            }
            btn.setDisable(false);
        });

        new Thread(task, "price-lookup").start();
    }

    private void openBrowser(String url) {
        try {
            Desktop.getDesktop().browse(new URI(url));
        } catch (Exception ignored) {}
    }

    private void openAmazonSearch() {
        String query = nameField.getText().trim();
        if (query.isBlank()) {
            lookupStatusLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 11px;");
            lookupStatusLabel.setText("Preencha o Nome do item antes de buscar na Amazon.");
            return;
        }
        try {
            String encoded = java.net.URLEncoder.encode(query, java.nio.charset.StandardCharsets.UTF_8);
            openBrowser("https://www.amazon.com.br/s?k=" + encoded);
            lookupStatusLabel.setStyle("-fx-text-fill: #e67e22; -fx-font-size: 11px;");
            lookupStatusLabel.setText("Busca Amazon aberta no navegador.");
        } catch (Exception ignored) {}
    }

    private void selectImage() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Selecionar foto do item");
        chooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Imagens", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp", "*.webp"));
        File file = chooser.showOpenDialog(getDialogPane().getScene().getWindow());
        if (file != null) {
            selectedImagePath = file.getAbsolutePath();
            loadImagePreview(selectedImagePath);
            imagePathLabel.setText(file.getName());
        }
    }

    private void clearImage() {
        selectedImagePath = "";
        imagePreview.setImage(null);
        imagePathLabel.setText("Nenhuma foto selecionada");
    }

    private void loadImagePreview(String path) {
        if (path == null || path.isBlank()) return;
        try {
            if (Files.exists(Paths.get(path))) {
                imagePreview.setImage(new Image("file:" + path, 80, 80, true, true));
            }
        } catch (Exception ignored) {}
    }

    private void populateFields(Item item) {
        nameField.setText(item.getName());
        categoryField.setText(nullSafe(item.getCategory()));
        descriptionArea.setText(nullSafe(item.getDescription()));
        conditionBox.setValue(item.getCondition() != null ? item.getCondition() : "Bom");
        datePicker.setValue(item.getAcquisitionDate());
        valueField.setText(String.format("%.2f", item.getValue()).replace('.', ','));
        notesArea.setText(nullSafe(item.getNotes()));
        selectedImagePath = nullSafe(item.getImagePath());
        if (!selectedImagePath.isBlank()) {
            loadImagePreview(selectedImagePath);
            imagePathLabel.setText(Paths.get(selectedImagePath).getFileName().toString());
        }
        isbnField.setText(nullSafe(item.getIsbn()));
        authorField.setText(nullSafe(item.getAuthor()));
        publisherField.setText(nullSafe(item.getPublisher()));
        yearField.setText(nullSafe(item.getPublishYear()));
        if (item.getMarketPrice() > 0) {
            marketPriceField.setText(String.format("%.2f", item.getMarketPrice()).replace('.', ','));
        }
    }

    private Item buildItem(Item existing) {
        Item result = existing != null ? existing : new Item();
        result.setName(nameField.getText().trim());
        result.setCategory(categoryField.getText().trim());
        result.setDescription(descriptionArea.getText().trim());
        result.setCondition(conditionBox.getValue());
        result.setAcquisitionDate(datePicker.getValue());
        result.setValue(parseDouble(valueField.getText()));
        result.setNotes(notesArea.getText().trim());
        result.setImagePath(selectedImagePath);
        result.setIsbn(isbnField.getText().trim());
        result.setAuthor(authorField.getText().trim());
        result.setPublisher(publisherField.getText().trim());
        result.setPublishYear(yearField.getText().trim());
        double mp = parseDouble(marketPriceField.getText());
        result.setMarketPrice(mp);
        if (mp > 0) {
            result.setMarketPriceDate(LocalDate.now().toString());
        }
        return result;
    }

    private double parseDouble(String text) {
        try {
            return Double.parseDouble(text.replace(",", ".").replaceAll("[^\\d.]", ""));
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private Label label(String text) {
        Label lbl = new Label(text);
        lbl.setStyle("-fx-font-weight: bold;");
        return lbl;
    }

    private String nullSafe(String val) {
        return val != null ? val : "";
    }
}
