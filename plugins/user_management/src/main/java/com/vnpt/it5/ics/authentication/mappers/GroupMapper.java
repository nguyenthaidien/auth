package com.vnpt.it5.ics.authentication.mappers;

import com.vnpt.it5.ics.authentication.models.GroupDto;
import com.vnpt.it5.ics.authentication.models.UserDto;
import org.keycloak.models.GroupModel;
import org.keycloak.models.UserModel;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;
import java.util.Map;

@Mapper(componentModel = "spring")
public abstract class GroupMapper {
    @Mapping(target = "administrativeUnit", source = "attributes")

    public abstract GroupDto modelToDto(GroupModel userModel);

    public String getAdministrativeUnit(Map<String, List<String>> attr){
        return attr.get("administrativeUnit").get(0);
    }
}
