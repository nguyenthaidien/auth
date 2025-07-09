package com.vnpt.it5.ics.authentication.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Pagination {
    private Integer pageSize;
    private Integer pageNumber;
}
