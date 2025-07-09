package com.vnpt.it5.ics.authentication.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserDto {
    private String username;
    private String firstName;
    private String lastName;
    private String id;
    private String email;
    private boolean emailVerified;
    private String locale;
    private boolean enabled;
    private String administrativeUnit;
    private List<String> requiredActions;
    private Long createdTimestamp;

}
