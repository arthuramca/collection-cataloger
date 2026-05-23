package com.arthas.cataloger.controller;

import com.arthas.cataloger.model.Item;
import com.arthas.cataloger.service.BackupService;
import com.arthas.cataloger.service.ItemService;
import com.arthas.cataloger.service.SpreadsheetService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class MainController implements Initializable {

    @FXML private TableView<Item>            itemTable;
    @FXML private TableColumn<Item, Integer> colId;
    @FXML private TableColumn<Item, String>  colImage;
    @FXML private TableColumn<Item, String>  colName;
    @FXML private TableColumn<Item, String>  colCategory;
    @FXML private TableColumn<Item, String>  colCondition;
    @FXML private TableColumn<Item, String>  colDate;
    @FXML private TableColumn<Item, Double>  colValue;
    @FXML private TableColumn<Item, String>  colNotes;
    @FXML private TextField                  searchField;
    @FXML private ComboBox<String>           categoryFilter;
    @FXML private Label                      statusLabel;
    @FXML private Label                      countLabel;

    private final ItemService       itemService       = new ItemService();
    private final SpreadsheetService spreadsheetService = new SpreadsheetService();
    private final BackupService     backupService     = new BackupService();
    private final ObservableList<Item> items          = FXCollections.observableArrayList();

    private static final String ALL_CATEGORIES = "Todas as categorias";

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        configureColumns();
        configureCategoryFilter();
        itemTable.setItems(items);
        loadAllItems();
    }

    private void configureColumns() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colCategory.setCellValueFactory(new PropertyValueFactory<>("category"));
        colCondition.setCellValueFactory(new PropertyValueFactory<>("condition"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("acquisitionDate"));
        colValue.setCellValueFactory(new PropertyValueFactory<>("value"));
        colNotes.setCellValueFactory(new PropertyValueFactory<>("notes"));
        colImage.setCellValueFactory(new PropertyValueFactory<>("imagePath"));

        // Coluna de imagem: mostra thumbnail 40x40
        colImage.setCellFactory(col -> new TableCell<>() {
            private final ImageView imageView = new ImageView();
            { imageView.setFitWidth(36); imageView.setFitHeight(36); imageView.setPreserveRatio(true); }

            @Override
            protected void updateItem(String path, boolean empty) {
                super.updateItem(path, empty);
                if (empty || path == null || path.isBlank() || !Files.exists(Paths.get(path))) {
                    setGraphic(null);
                } else {
                    try {
                        imageView.setImage(new Image("file:" + path, 36, 36, true, true));
                        setGraphic(imageView);
                    } catch (Exception e) {
                        setGraphic(null);
                    }
                }
            }
        });

        // Formatar coluna de valor
        colValue.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Double val, boolean empty) {
                super.updateItem(val, empty);
                setText(empty || val == null ? null : String.format("R$ %.2f", val));
            }
        });

        // Duplo clique abre edição
        itemTable.setRowFactory(tv -> {
            TableRow<Item> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty()) openEditDialog(row.getItem());
            });
            return row;
        });
    }

    private void configureCategoryFilter() {
        categoryFilter.getItems().add(ALL_CATEGORIES);
        categoryFilter.setValue(ALL_CATEGORIES);
        categoryFilter.valueProperty().addListener((obs, old, val) -> applyFilter());
    }

    private void refreshCategoryFilter() {
        String current = categoryFilter.getValue();
        List<String> cats = new ArrayList<>();
        cats.add(ALL_CATEGORIES);
        cats.addAll(itemService.getDistinctCategories());
        categoryFilter.getItems().setAll(cats);
        categoryFilter.setValue(cats.contains(current) ? current : ALL_CATEGORIES);
    }

    private void applyFilter() {
        String cat = categoryFilter.getValue();
        String query = searchField.getText().trim();

        try {
            List<Item> result;
            if (ALL_CATEGORIES.equals(cat)) {
                result = query.isEmpty() ? itemService.getAllItems() : itemService.searchItems(query);
            } else {
                result = itemService.getItemsByCategory(cat);
                if (!query.isEmpty()) {
                    String q = query.toLowerCase();
                    result = result.stream()
                        .filter(i -> i.getName().toLowerCase().contains(q)
                                  || nullSafe(i.getDescription()).toLowerCase().contains(q))
                        .toList();
                }
            }
            items.setAll(result);
            setStatus(buildStatusMessage(cat, query), result.size());
        } catch (Exception e) {
            showError("Erro ao filtrar: " + e.getMessage());
        }
    }

    private void loadAllItems() {
        try {
            items.setAll(itemService.getAllItems());
            refreshCategoryFilter();
            setStatus("Pronto", items.size());
        } catch (Exception e) {
            showError("Erro ao carregar itens: " + e.getMessage());
        }
    }

    @FXML private void onSearch()      { applyFilter(); }
    @FXML private void onClearSearch() { searchField.clear(); categoryFilter.setValue(ALL_CATEGORIES); }

    @FXML
    private void onNewItem() {
        new ItemDialog(null).showAndWait().ifPresent(item -> {
            itemService.saveItem(item);
            loadAllItems();
            setStatus("Item adicionado: " + item.getName(), items.size());
        });
    }

    @FXML
    private void onEditItem() {
        Item selected = itemTable.getSelectionModel().getSelectedItem();
        if (selected == null) { showInfo("Selecione um item para editar."); return; }
        openEditDialog(selected);
    }

    private void openEditDialog(Item item) {
        new ItemDialog(item).showAndWait().ifPresent(updated -> {
            itemService.saveItem(updated);
            loadAllItems();
            setStatus("Item atualizado: " + updated.getName(), items.size());
        });
    }

    @FXML
    private void onDeleteItem() {
        Item selected = itemTable.getSelectionModel().getSelectedItem();
        if (selected == null) { showInfo("Selecione um item para excluir."); return; }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmar exclusão");
        confirm.setHeaderText("Excluir \"" + selected.getName() + "\"?");
        confirm.setContentText("Esta ação não pode ser desfeita.");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                itemService.deleteItem(selected.getId());
                loadAllItems();
                setStatus("Item excluído", items.size());
            }
        });
    }

    @FXML
    private void onExport() {
        FileChooser chooser = xlsxChooser("Exportar Coleção", "colecao.xlsx");
        File file = chooser.showSaveDialog(itemTable.getScene().getWindow());
        if (file == null) return;
        try {
            spreadsheetService.exportToXlsx(List.copyOf(items), file);
            setStatus("Exportado: " + file.getName(), items.size());
        } catch (IOException e) {
            showError("Falha ao exportar: " + e.getMessage());
        }
    }

    @FXML
    private void onImport() {
        FileChooser chooser = xlsxChooser("Importar Coleção", null);
        File file = chooser.showOpenDialog(itemTable.getScene().getWindow());
        if (file == null) return;
        try {
            List<Item> imported = spreadsheetService.importFromXlsx(file);
            imported.forEach(itemService::saveItem);
            loadAllItems();
            setStatus("Importados " + imported.size() + " itens de " + file.getName(), items.size());
        } catch (IOException e) {
            showError("Falha ao importar: " + e.getMessage());
        }
    }

    @FXML
    private void onShowChart() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/arthas/cataloger/chart-view.fxml"));
            Stage stage = new Stage();
            stage.setTitle("Gráficos da Coleção");
            stage.setScene(new Scene(loader.load(), 700, 520));
            stage.show();
        } catch (IOException e) {
            showError("Não foi possível abrir os gráficos: " + e.getMessage());
        }
    }

    @FXML
    private void onBackup() {
        // Tenta OneDrive primeiro; se não disponível, pede diretório
        if (backupService.isOneDriveAvailable()) {
            BackupService.BackupResult result = backupService.backupToOneDrive();
            if (result == BackupService.BackupResult.SUCCESS) {
                showInfo("Backup salvo no OneDrive com sucesso!\nPasta: OneDrive/collection-cataloger/");
                return;
            }
        }
        // Fallback: escolher pasta manualmente
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Escolher pasta para backup");
        File dir = chooser.showDialog(itemTable.getScene().getWindow());
        if (dir == null) return;
        try {
            String path = backupService.backupToDirectory(dir);
            showInfo("Backup salvo com sucesso!\n" + path);
        } catch (IOException e) {
            showError("Falha ao fazer backup: " + e.getMessage());
        }
    }

    // --- Utilitários ---

    private FileChooser xlsxChooser(String title, String initialName) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(title);
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel (.xlsx)", "*.xlsx"));
        if (initialName != null) chooser.setInitialFileName(initialName);
        return chooser;
    }

    private String buildStatusMessage(String cat, String query) {
        if (!ALL_CATEGORIES.equals(cat) && !query.isEmpty()) return "Filtro: " + cat + " | Busca: \"" + query + "\"";
        if (!ALL_CATEGORIES.equals(cat)) return "Categoria: " + cat;
        if (!query.isEmpty()) return "Busca: \"" + query + "\"";
        return "Pronto";
    }

    private void setStatus(String message, int count) {
        statusLabel.setText(message);
        countLabel.setText(count + (count == 1 ? " item" : " itens"));
    }

    private void showError(String msg) { new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK).showAndWait(); }
    private void showInfo(String msg)  { new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK).showAndWait(); }
    private String nullSafe(String v)  { return v != null ? v : ""; }
}
