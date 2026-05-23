package com.arthas.cataloger.model;

import java.time.LocalDate;

public class Item {

    private int id;
    private String name;
    private String category;
    private String description;
    private String condition;
    private LocalDate acquisitionDate;
    private double value;
    private String notes;
    private String imagePath;
    private String isbn;
    private String author;
    private String publisher;
    private String publishYear;
    private double marketPrice;
    private String marketPriceDate;

    public Item() {}

    public Item(String name, String category, String description, String condition,
                LocalDate acquisitionDate, double value, String notes) {
        this.name = name;
        this.category = category;
        this.description = description;
        this.condition = condition;
        this.acquisitionDate = acquisitionDate;
        this.value = value;
        this.notes = notes;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCondition() { return condition; }
    public void setCondition(String condition) { this.condition = condition; }

    public LocalDate getAcquisitionDate() { return acquisitionDate; }
    public void setAcquisitionDate(LocalDate acquisitionDate) { this.acquisitionDate = acquisitionDate; }

    public double getValue() { return value; }
    public void setValue(double value) { this.value = value; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }

    public String getIsbn() { return isbn; }
    public void setIsbn(String isbn) { this.isbn = isbn; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public String getPublisher() { return publisher; }
    public void setPublisher(String publisher) { this.publisher = publisher; }

    public String getPublishYear() { return publishYear; }
    public void setPublishYear(String publishYear) { this.publishYear = publishYear; }

    public double getMarketPrice() { return marketPrice; }
    public void setMarketPrice(double marketPrice) { this.marketPrice = marketPrice; }

    public String getMarketPriceDate() { return marketPriceDate; }
    public void setMarketPriceDate(String marketPriceDate) { this.marketPriceDate = marketPriceDate; }

    @Override
    public String toString() {
        return "Item{id=" + id + ", name='" + name + "', category='" + category + "'}";
    }
}
