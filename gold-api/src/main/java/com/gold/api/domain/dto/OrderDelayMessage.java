package com.gold.api.domain.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class OrderDelayMessage implements Serializable {
    private String orderNo;
}