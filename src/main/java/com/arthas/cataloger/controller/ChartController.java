package com.arthas.cataloger.controller;

import com.arthas.cataloger.model.Item;
import com.arthas.cataloger.service.ItemService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.*;
import javafx.scene.control.Label;

import java.net.URL;
import java.util.*;

public class ChartController implements Initializable {

    @FXML private PieChart valueChart;
    @FXML private BarChart<String, Number> countChart;
    @FXML private Label totalValueLabel;
    @FXML private Label totalItemsLabel;

    private final ItemService itemService = new ItemService();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        loadCharts();
    }

    private void loadCharts() {
        List<Item> items = itemService.getAllItems();

        if (items.isEmpty()) {
            valueChart.setTitle("Nenhum item cadastrado");
            countChart.setTitle("Nenhum item cadastrado");
            totalValueLabel.setText("Total: R$ 0,00");
            totalItemsLabel.setText("0 itens");
            return;
        }

        // Agrupar por categoria
        Map<String, Double>  valueByCategory = new LinkedHashMap<>();
        Map<String, Integer> countByCategory = new LinkedHashMap<>();

        for (Item item : items) {
            String cat = (item.getCategory() != null && !item.getCategory().isBlank())
                       ? item.getCategory() : "Sem categoria";
            valueByCategory.merge(cat, item.getValue(), Double::sum);
            countByCategory.merge(cat, 1, Integer::sum);
        }

        // PieChart — valor por categoria
        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
        valueByCategory.forEach((cat, val) ->
            pieData.add(new PieChart.Data(
                cat + "\nR$ " + String.format("%,.2f", val), val)));
        valueChart.setData(pieData);
        valueChart.setTitle("Valor Total por Categoria");
        valueChart.setLegendVisible(false);

        // BarChart — quantidade por categoria
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Itens");
        countByCategory.forEach((cat, count) ->
            series.getData().add(new XYChart.Data<>(cat, count)));
        countChart.getData().clear();
        countChart.getData().add(series);
        countChart.setTitle("Itens por Categoria");
        countChart.setLegendVisible(false);

        // Totalizadores
        double total = items.stream().mapToDouble(Item::getValue).sum();
        totalValueLabel.setText("Valor total da coleção: R$ " + String.format("%,.2f", total));
        totalItemsLabel.setText(items.size() + (items.size() == 1 ? " item" : " itens"));
    }
}
