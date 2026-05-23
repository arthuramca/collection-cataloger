package com.arthas.cataloger.service;

import com.arthas.cataloger.model.Item;
import com.arthas.cataloger.repository.ItemRepository;

import java.sql.SQLException;
import java.util.List;

public class ItemService {

    private final ItemRepository repository;

    public ItemService() {
        this.repository = new ItemRepository();
    }

    public ItemService(ItemRepository repository) {
        this.repository = repository;
    }

    public List<Item> getAllItems() {
        try {
            return repository.findAll();
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao buscar todos os itens", e);
        }
    }

    public List<Item> searchItems(String query) {
        if (query == null || query.isBlank()) {
            return getAllItems();
        }
        try {
            return repository.search(query.trim());
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao pesquisar itens: " + query, e);
        }
    }

    public Item saveItem(Item item) {
        if (item.getName() == null || item.getName().isBlank()) {
            throw new IllegalArgumentException("Nome do item é obrigatório");
        }
        try {
            return repository.save(item);
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao salvar item: " + item.getName(), e);
        }
    }

    public void deleteItem(int id) {
        try {
            repository.delete(id);
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao excluir item com id: " + id, e);
        }
    }
}
