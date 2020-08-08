package br.com.siecola.aws_lambda_products_api.model;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class Product {

    private int id;
    private String name;
    private String code;
    private String model;
    private double price;

    public Product(String json) {
        Gson gson = new Gson();
        Product request = gson.fromJson(json, Product.class);
        this.id = request.getId();
        this.name = request.getName();
        this.code = request.getCode();
        this.model = request.getModel();
        this.price = request.getPrice();
    }

    public String toString() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(this);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }
}