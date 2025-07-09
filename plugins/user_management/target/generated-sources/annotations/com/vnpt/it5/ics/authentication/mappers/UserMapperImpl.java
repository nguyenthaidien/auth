package com.vnpt.it5.ics.authentication.mappers;

import com.vnpt.it5.ics.authentication.models.UserDto;
import java.util.ArrayList;
import java.util.Set;
import javax.annotation.processing.Generated;
import org.keycloak.models.UserModel;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-06-20T12:20:41+0700",
    comments = "version: 1.4.2.Final, compiler: Eclipse JDT (IDE) 3.42.0.v20250514-1000, environment: Java 21.0.7 (Eclipse Adoptium)"
)
@Component
public class UserMapperImpl extends UserMapper {

    @Override
    public UserDto modelToDto(UserModel userModel) {
        if ( userModel == null ) {
            return null;
        }

        UserDto userDto = new UserDto();

        userDto.setAdministrativeUnit( getAdministrativeUnit( userModel.getAttributes() ) );
        userDto.setCreatedTimestamp( userModel.getCreatedTimestamp() );
        userDto.setEmail( userModel.getEmail() );
        userDto.setEmailVerified( userModel.isEmailVerified() );
        userDto.setEnabled( userModel.isEnabled() );
        userDto.setFirstName( userModel.getFirstName() );
        userDto.setId( userModel.getId() );
        userDto.setLastName( userModel.getLastName() );
        Set<String> set = userModel.getRequiredActions();
        if ( set != null ) {
            userDto.setRequiredActions( new ArrayList<String>( set ) );
        }
        userDto.setUsername( userModel.getUsername() );

        return userDto;
    }
}
