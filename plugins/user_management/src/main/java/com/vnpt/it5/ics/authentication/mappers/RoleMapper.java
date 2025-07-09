package com.vnpt.it5.ics.authentication.mappers;

import com.vnpt.it5.ics.authentication.dto.RoleDto;
import org.keycloak.models.RoleModel;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface RoleMapper {

    RoleDto modelToDto(RoleModel roleModel);
}
