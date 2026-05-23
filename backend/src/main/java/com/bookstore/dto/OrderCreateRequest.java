package com.bookstore.dto;

import jakarta.validation.constraints.NotBlank;

public class OrderCreateRequest {

    @NotBlank(message = "收货人不能为空")
    private String receiver;

    @NotBlank(message = "手机号不能为空")
    private String phone;

    private String province;
    private String city;

    @NotBlank(message = "详细地址不能为空")
    private String address;

    private String note;

    private String payment;

    public String getReceiver() { return receiver; }
    public void setReceiver(String receiver) { this.receiver = receiver; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getProvince() { return province; }
    public void setProvince(String province) { this.province = province; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public String getPayment() { return payment; }
    public void setPayment(String payment) { this.payment = payment; }
}
