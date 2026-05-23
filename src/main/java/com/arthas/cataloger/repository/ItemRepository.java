package com.arthas.cataloger.repository;

import com.arthas.cataloger.model.Item;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ItemRepository {

    private final Connection connection;

    public ItemRepository() {
        try {
            this.connection = DatabaseManager.getConnection();
        } catch (SQLException e) {
            throw new RuntimeException("Falha ao conectar ao banco de dados", e);
        }
    }

    // Constructor para injeção de conexão em testes
    public ItemRepository(Connection connection) {
        this.connection = connection;
    }

    public List<String> findDistinctCategories() throws SQLException {
        List<String> categories = new ArrayList<>();
        String sql = "SELECT DISTINCT category FROM items WHERE category != '' ORDER BY category COLLATE NOCASE";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) categories.add(rs.getString(1));
        }
        return categories;
    }

    public List<Item> findByCategory(String category) throws SQLException {
        List<Item> items = new ArrayList<>();
        String sql = "SELECT * FROM items WHERE category = ? ORDER BY name COLLATE NOCASE";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, category);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) items.add(mapRow(rs));
            }
        }
        return items;
    }

    public List<Item> findAll() throws SQLException {
        List<Item> items = new ArrayList<>();
        String sql = "SELECT * FROM items ORDER BY name COLLATE NOCASE";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                items.add(mapRow(rs));
            }
        }
        return items;
    }

    public List<Item> search(String query) throws SQLException {
        List<Item> items = new ArrayList<>();
        String sql = """
                SELECT * FROM items
                WHERE name LIKE ? OR category LIKE ? OR description LIKE ?
                ORDER BY name COLLATE NOCASE
                """;
        String pattern = "%" + query + "%";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, pattern);
            stmt.setString(2, pattern);
            stmt.setString(3, pattern);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    items.add(mapRow(rs));
                }
            }
        }
        return items;
    }

    public Item save(Item item) throws SQLException {
        return item.getId() == 0 ? insert(item) : update(item);
    }

    private Item insert(Item item) throws SQLException {
        String sql = """
                INSERT INTO items (name, category, description, condition, acquisition_date, value, notes, image_path)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bindParams(stmt, item);
            stmt.executeUpdate();
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    item.setId(keys.getInt(1));
                }
            }
        }
        return item;
    }

    private Item update(Item item) throws SQLException {
        String sql = """
                UPDATE items
                SET name = ?, category = ?, description = ?, condition = ?,
                    acquisition_date = ?, value = ?, notes = ?, image_path = ?
                WHERE id = ?
                """;
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            bindParams(stmt, item);
            stmt.setInt(9, item.getId());
            stmt.executeUpdate();
        }
        return item;
    }

    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM items WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        }
    }

    private void bindParams(PreparedStatement stmt, Item item) throws SQLException {
        stmt.setString(1, nullSafe(item.getName()));
        stmt.setString(2, nullSafe(item.getCategory()));
        stmt.setString(3, nullSafe(item.getDescription()));
        stmt.setString(4, nullSafe(item.getCondition()));
        stmt.setString(5, item.getAcquisitionDate() != null ? item.getAcquisitionDate().toString() : null);
        stmt.setDouble(6, item.getValue());
        stmt.setString(7, nullSafe(item.getNotes()));
        stmt.setString(8, nullSafe(item.getImagePath()));
    }

    private Item mapRow(ResultSet rs) throws SQLException {
        Item item = new Item();
        item.setId(rs.getInt("id"));
        item.setName(rs.getString("name"));
        item.setCategory(rs.getString("category"));
        item.setDescription(rs.getString("description"));
        item.setCondition(rs.getString("condition"));
        String dateStr = rs.getString("acquisition_date");
        if (dateStr != null && !dateStr.isBlank()) {
            item.setAcquisitionDate(LocalDate.parse(dateStr));
        }
        item.setValue(rs.getDouble("value"));
        item.setNotes(rs.getString("notes"));
        item.setImagePath(rs.getString("image_path"));
        return item;
    }

    private String nullSafe(String value) {
        return value != null ? value : "";
    }
}
