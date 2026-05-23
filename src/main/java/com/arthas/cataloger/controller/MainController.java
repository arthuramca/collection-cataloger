package com.arthas.cataloger.controller;

import com.arthas.cataloger.model.Item;
import com.arthas.cataloger.service.ItemService;
import com.arthas.cataloger.service.SpreadsheetService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class MainController implements Initializable {

    @FXML private TableView<Item> itemTable;
    @FXML private TableColumn<Item, Integer> colId;
    @FXML private TableColumn<Item, String>  colName;
    @FXML private TableColumn<Item, String>  colCategory;
    @FXML private TableColumn<Item, String>  colCondition;
    @FXML private TableColumn<Item, String>  colDate;
    @FXML private TableColumn<Item, Double>  colValue;
    @FXML private TableColumn<Item, String>  colNotes;
    @FXML private TextField searchField;
    @FXML private Label statusLabel;
    @FXML private Label countLabel;

    private final ItemService itemService = new ItemService();
    private final SpreadsheetService spreadsheetService = new SpreadsheetService();
    private final ObservableList<Item> items = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        configureColumns();
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

        colValue.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Double val, boolean empty) {
                super.updateItem(val, empty);
                setText(empty || val == null ? null : String.format("R$ %.2f", val));
            }
        });

        // duplo clique na linha abre o formulário de edição
        itemTable.setRowFactory(tv -> {
            TableRow<Item> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty()) {
                    openEditDialog(row.getItem());
                }
            });
            return row;
        });
    }

    private void loadAllItems() {
        try {
            items.setAll(itemService.getAllItems());
            setStatus("Pronto", items.size());
        } catch (Exception e) {
            showError("Erro ao carregar itens: " + e.getMessage());
        }
    }

    @FXML
    private void onSearch() {
        String query = searchField.getText().trim();
        try {
            List<Item> results = itemService.searchItems(query);
            items.setAll(results);
            setStatus(query.isEmpty() ? "Pronto" : "Busca: \"" + query + "\"", results.size());
        } catch (Exception e) {
            showError("Erro na busca: " + e.getMessage());
        }
    }

    @FXML
    private void onClearSearch() {
        searchField.clear();
        loadAllItems();
    }

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
        if (selected == null) {
            showInfo("Selecione um item na tabela para editar.");
            return;
        }
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
        if (selected == null) {
            showInfo("Selecione um item na tabela para excluir.");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmar exclusão");
        confirm.setHeaderText("Excluir item?");
        confirm.setContentText("\"" + selected.getName() + "\" será removido permanentemente.");
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
        FileChooser chooser = buildChooser("Exportar Coleção", "colecao.xlsx");
        File file = chooser.showSaveDialog(itemTable.getScene().getWindow());
        if (file == null) return;
        try {
            spreadsheetService.exportToXlsx(List.copyOf(items), file);
            setStatus("Exportado: " + file.getName() + " (" + items.size() + " itens)", items.size());
        } catch (IOException e) {
            showError("Falha ao exportar: " + e.getMessage());
        }
    }

    @FXML
    private void onImport() {
        FileChooser chooser = buildChooser("Importar Coleção", null);
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

    private FileChooser buildChooser(String title, String initialFileName) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(title);
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Planilha Excel (.xlsx)", "*.xlsx"));
        if (initialFileName != null) chooser.setInitialFileName(initialFileName);
        return chooser;
    }

    private void setStatus(String message, int count) {
        statusLabel.setText(message);
        countLabel.setText(count + (count == 1 ? " item" : " itens"));
    }

    private void showError(String msg) {
        new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK).showAndWait();
    }

    private void showInfo(String msg) {
        new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK).showAndWait();
    }
}
