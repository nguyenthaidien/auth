package com.vnpt.it5.ics.authentication.mappers;

import com.vnpt.it5.ics.authentication.models.GroupDto;
import javax.annotation.processing.Generated;
import org.keycloak.models.GroupModel;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-06-20T12:20:41+0700",
    comments = "version: 1.4.2.Final, compiler: Eclipse JDT (IDE) 3.42.0.v20250514-1000, environment: Java 21.0.7 (Eclipse Adoptium)"
)
@Component
public class GroupMapperImpl extends GroupMapper {

    @Override
    public GroupDto modelToDto(GroupModel userModel) {
        if ( userModel == null ) {
            return null;
        }

        GroupDto groupDto = new GroupDto();

        groupDto.setAdministrativeUnit( getAdministrativeUnit( userModel.getAttributes() ) );
        groupDto.setId( userModel.getId() );
        groupDto.setName( userModel.getName() );

        return groupDto;
    }
}
