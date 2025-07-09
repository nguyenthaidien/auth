package com.vnpt.it5.ics.authentication.mappers;

import com.vnpt.it5.ics.authentication.models.UserDto;
import org.keycloak.models.UserModel;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;
import java.util.Map;

@Mapper(componentModel = "spring")
public abstract class UserMapper {

    @Mapping(target = "administrativeUnit", source = "attributes")
    public abstract UserDto modelToDto(UserModel userModel);

    public String getAdministrativeUnit(Map<String, List<String>> attr){
        return attr.get("administrativeUnit").get(0);
    }
}
