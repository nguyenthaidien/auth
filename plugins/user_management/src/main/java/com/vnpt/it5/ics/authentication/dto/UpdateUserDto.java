package com.vnpt.it5.ics.authentication.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.keycloak.models.UserModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateUserDto {
    private String id;
    private String username;
    private Boolean enabled=true;
    private Boolean emailVerified=false;
    private String firstName;
    private String lastName;
    private Map<String,Object> attributes;
    private String email;
    private List<UserModel.RequiredAction> requiredActions=new ArrayList<>();
}
