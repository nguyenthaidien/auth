package com.vnpt.it5.ics.authentication.mappers;

import com.vnpt.it5.ics.authentication.dto.RoleDto;
import javax.annotation.processing.Generated;
import org.keycloak.models.RoleModel;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-06-20T12:20:40+0700",
    comments = "version: 1.4.2.Final, compiler: Eclipse JDT (IDE) 3.42.0.v20250514-1000, environment: Java 21.0.7 (Eclipse Adoptium)"
)
@Component
public class RoleMapperImpl implements RoleMapper {

    @Override
    public RoleDto modelToDto(RoleModel roleModel) {
        if ( roleModel == null ) {
            return null;
        }

        RoleDto roleDto = new RoleDto();

        roleDto.setDescription( roleModel.getDescription() );
        roleDto.setId( roleModel.getId() );
        roleDto.setName( roleModel.getName() );

        return roleDto;
    }
}
