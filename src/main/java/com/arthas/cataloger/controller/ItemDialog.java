package com.arthas.cataloger.controller;

import com.arthas.cataloger.model.Item;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;

public class ItemDialog extends Dialog<Item> {

    private final TextField     nameField        = new TextField();
    private final TextField     categoryField    = new TextField();
    private final TextArea      descriptionArea  = new TextArea();
    private final ComboBox<String> conditionBox  = new ComboBox<>();
    private final DatePicker    datePicker       = new DatePicker();
    private final TextField     valueField       = new TextField("0,00");
    private final TextArea      notesArea        = new TextArea();

    public ItemDialog(Item item) {
        boolean isNew = item == null;
        setTitle(isNew ? "Novo Item" : "Editar Item");
        setHeaderText(isNew ? "Adicione um novo item à sua coleção" : "Edite os dados do item selecionado");

        ButtonType saveButtonType = new ButtonType("Salvar", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        conditionBox.getItems().addAll("Excelente", "Muito Bom", "Bom", "Regular", "Ruim");

        descriptionArea.setPrefRowCount(3);
        descriptionArea.setWrapText(true);
        notesArea.setPrefRowCount(3);
        notesArea.setWrapText(true);

        GridPane grid = buildGrid();
        getDialogPane().setContent(grid);
        getDialogPane().setPrefWidth(480);

        if (!isNew) {
            populateFields(item);
        } else {
            conditionBox.setValue("Bom");
        }

        // desabilita o botão salvar enquanto o nome estiver vazio
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
        GridPane.setHgrow(nameField, Priority.ALWAYS);
        GridPane.setHgrow(categoryField, Priority.ALWAYS);

        int row = 0;
        grid.add(label("Nome *"),            0, row); grid.add(nameField,       1, row++);
        grid.add(label("Categoria"),         0, row); grid.add(categoryField,   1, row++);
        grid.add(label("Descrição"),         0, row); grid.add(descriptionArea, 1, row++);
        grid.add(label("Condição"),          0, row); grid.add(conditionBox,    1, row++);
        grid.add(label("Data de aquisição"), 0, row); grid.add(datePicker,      1, row++);
        grid.add(label("Valor (R$)"),        0, row); grid.add(valueField,      1, row++);
        grid.add(label("Observações"),       0, row); grid.add(notesArea,       1, row);

        return grid;
    }

    private void populateFields(Item item) {
        nameField.setText(item.getName());
        categoryField.setText(nullSafe(item.getCategory()));
        descriptionArea.setText(nullSafe(item.getDescription()));
        conditionBox.setValue(item.getCondition() != null ? item.getCondition() : "Bom");
        datePicker.setValue(item.getAcquisitionDate());
        valueField.setText(String.format("%.2f", item.getValue()).replace('.', ','));
        notesArea.setText(nullSafe(item.getNotes()));
    }

    private Item buildItem(Item existing) {
        Item result = existing != null ? existing : new Item();
        result.setName(nameField.getText().trim());
        result.setCategory(categoryField.getText().trim());
        result.setDescription(descriptionArea.getText().trim());
        result.setCondition(conditionBox.getValue());
        result.setAcquisitionDate(datePicker.getValue());
        result.setValue(parseValue(valueField.getText()));
        result.setNotes(notesArea.getText().trim());
        return result;
    }

    private double parseValue(String text) {
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
