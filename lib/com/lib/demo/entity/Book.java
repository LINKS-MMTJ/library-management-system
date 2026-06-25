package com.lib.demo.entity;

import java.io.Serializable;
import java.time.LocalDate;

public class Book implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long bookId;
    private String isbn;
    private String title;
    private String author;
    private String publisher;
    private LocalDate publishDate;
    private String category;
    private String location;
    private Integer totalCopies;
    private Integer availableCopies;

    public Long getBookId() { return bookId; }
    public void setBookId(Long bookId) { this.bookId = bookId; }
    public String getIsbn() { return isbn; }
    public void setIsbn(String isbn) { this.isbn = isbn; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }
    public String getPublisher() { return publisher; }
    public void setPublisher(String publisher) { this.publisher = publisher; }
    public LocalDate getPublishDate() { return publishDate; }
    public void setPublishDate(LocalDate publishDate) { this.publishDate = publishDate; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public Integer getTotalCopies() { return totalCopies; }
    public void setTotalCopies(Integer totalCopies) { this.totalCopies = totalCopies; }
    public Integer getAvailableCopies() { return availableCopies; }
    public void setAvailableCopies(Integer availableCopies) { this.availableCopies = availableCopies; }

    public void updateInfo(Book source) {
        if (source.getIsbn() != null) this.isbn = source.getIsbn();
        if (source.getTitle() != null) this.title = source.getTitle();
        if (source.getAuthor() != null) this.author = source.getAuthor();
        if (source.getPublisher() != null) this.publisher = source.getPublisher();
        if (source.getPublishDate() != null) this.publishDate = source.getPublishDate();
        if (source.getCategory() != null) this.category = source.getCategory();
        if (source.getLocation() != null) this.location = source.getLocation();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Book)) return false;
        Book book = (Book) o;
        return isbn != null ? isbn.equals(book.isbn) : book.isbn == null;
    }

    @Override
    public int hashCode() {
        return isbn != null ? isbn.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "Book{bookId=" + bookId + ", isbn='" + isbn + "', title='" + title +
                "', author='" + author + "', availableCopies=" + availableCopies + "}";
    }
}
