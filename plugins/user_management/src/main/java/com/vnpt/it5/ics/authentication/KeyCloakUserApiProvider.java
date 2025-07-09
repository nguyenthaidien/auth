package com.vnpt.it5.ics.authentication;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openshift.restclient.authorization.IAuthorizationDetails;
import com.openshift.restclient.authorization.UnauthorizedException;
import com.vnpt.it5.ics.authentication.dto.*;
import com.vnpt.it5.ics.authentication.mappers.*;
import com.vnpt.it5.ics.authentication.models.GroupDto;
import com.vnpt.it5.ics.authentication.models.UserDto;
import org.apache.http.HttpException;
import org.jboss.resteasy.annotations.cache.NoCache;
import org.jboss.resteasy.spi.HttpRequest;
import org.keycloak.connections.httpclient.HttpClientProvider;
import org.keycloak.models.*;
import org.keycloak.services.managers.AppAuthManager;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.resource.RealmResourceProvider;
import org.keycloak.services.resources.Cors;
import org.keycloak.storage.user.UserQueryProvider;
import org.keycloak.utils.MediaType;
import org.springframework.web.bind.annotation.RequestBody;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

public class KeyCloakUserApiProvider implements RealmResourceProvider {
    private final KeycloakSession session;
    private final String defaultAttr = "administrativeUnit";

    private final UserMapper userMapper;
    private final GroupMapper groupMapper;

    private final RoleMapper roleMapper;
    private final AuthenticationManager.AuthResult auth;
    private Properties properties;

    public KeyCloakUserApiProvider(KeycloakSession session) {
        this.session = session;
        this.auth = this.resolveAuthentication(session);

        this.userMapper = new UserMapperImpl();
        this.roleMapper = new RoleMapperImpl();
        this.groupMapper = new GroupMapperImpl();

    }

    public void close() {
    }

    public Object getResource() {
        return this;
    }

    @OPTIONS
    @Path("{any:.*}")
    public Response preflight() {
        HttpRequest request = session.getContext().getContextObject(HttpRequest.class);
        return Cors.add(request, Response.ok()).preflight()
                .allowAllOrigins()
                .allowedMethods("GET", "PUT", "POST", "DELETE", "PATCH", "OPTIONS").auth().preflight().build();
    }

    @GET
    @Path("users/search-by-attr")
    @NoCache
    @Produces({MediaType.APPLICATION_JSON})
    @Encoded
    public List<UserDto> searchUsersByAttribute(@DefaultValue(defaultAttr) @QueryParam("attr") String attr,
                                                @QueryParam("value") String value) {
        return session.users().searchForUserByUserAttributeStream(session.getContext().getRealm(), attr, value)
                .map(userMapper::modelToDto)
                .collect(Collectors.toList());
    }

    @GET
    @Path("users/search")
    @NoCache
    @Produces({MediaType.APPLICATION_JSON})
    @Encoded
    public Response searchUsers(
            @DefaultValue("20") @QueryParam("pageSize") Integer pageSize,
            @DefaultValue("0") @QueryParam("pageNumber") Integer pageNumber,
            @QueryParam("searchKey") String searchKey

    ) {
        boolean admin = ensureAdmin();
        if(!admin){
            return Response.status(403)
                    .header("Access-Control-Allow-Origin", "*")

                    .build();
        }

        ReadPropertiesSingleton demo = ReadPropertiesSingleton.getInstance();
        Map<String, Object> jsonMap = new HashMap<>();
        HttpClientProvider provider = this.session.getProvider(HttpClientProvider.class);
        try {
            System.out.println(demo.getProperty("device-service.url"));
            InputStream inputStream = provider.get(demo.getProperty("device-service.url") + "/category/administrative" +
                    "-unit/" + this.auth.getUser()
                    .getFirstAttribute(defaultAttr) + "/children");
            ObjectMapper mapper = new ObjectMapper();
            jsonMap = mapper.readValue(inputStream, Map.class);

        } catch (IOException e) {
            e.printStackTrace();
        }
        List<Map<String, Object>> listAU = (List<Map<String, Object>>) jsonMap.get("data");
        List<String> list = listAU.stream().map(el -> (String) el.get("id")).collect(Collectors.toList());

        List<UserDto> collect =
                this.session.users().searchForUserStream(session.getContext().getRealm(), searchKey).filter(
                                userModel -> {
                                    if (userModel.getFirstAttribute(defaultAttr)
                                            .equals(this.auth.getUser()
                                                    .getFirstAttribute(defaultAttr))) {
                                        return true;
                                    }
                                    return list.contains(userModel.getFirstAttribute(defaultAttr));
                                })

                        .skip(pageSize * pageNumber).limit(pageSize)
                        .map(userMapper::modelToDto).collect(Collectors.toList());
        return Response.status(200)
                .header("Access-Control-Allow-Origin", "*")
                .entity(collect)
                .build();
    }


    @GET
    @Path("groups/search")
    @NoCache
    @Produces({MediaType.APPLICATION_JSON})
    @Encoded
    public Response searchGroups(
            @DefaultValue("20") @QueryParam("pageSize") Integer pageSize,
            @DefaultValue("0") @QueryParam("pageNumber") Integer pageNumber,
            @QueryParam("searchKey") String searchKey

    ) {
        boolean admin = ensureAdmin();
        if(!admin){
            return Response.status(403)
                    .header("Access-Control-Allow-Origin", "*")

                    .build();
        }

        ReadPropertiesSingleton demo = ReadPropertiesSingleton.getInstance();
        Map<String, Object> jsonMap = new HashMap<>();
        HttpClientProvider provider = this.session.getProvider(HttpClientProvider.class);
        try {
            InputStream inputStream = provider.get(demo.getProperty("device-service.url") + "/category/administrative" +
                    "-unit/" + this.auth.getUser()
                    .getFirstAttribute(defaultAttr) + "/children");
            ObjectMapper mapper = new ObjectMapper();
            jsonMap = mapper.readValue(inputStream, Map.class);

        } catch (IOException e) {
            e.printStackTrace();
        }
        List<Map<String, Object>> listAU = (List<Map<String, Object>>) jsonMap.get("data");
        List<String> list = listAU.stream().map(el -> (String) el.get("id")).collect(Collectors.toList());

        List<GroupDto> collect =
                this.session.groups().searchForGroupByNameStream(session.getContext().getRealm(), searchKey,pageSize * pageNumber,pageSize * pageNumber + pageSize).filter(
                                groupModel -> {
                                    String att = groupModel.getFirstAttribute(defaultAttr);
                                    if(att == null){
                                        return false;
                                    }
                                    if (
                                            att.equals(this.auth.getUser()
                                                    .getFirstAttribute(defaultAttr))) {
                                        return true;
                                    }
                                    return list.contains(groupModel.getFirstAttribute(defaultAttr));
                                })
                        .map(groupMapper::modelToDto).collect(Collectors.toList());
        return Response.status(200)
                .header("Access-Control-Allow-Origin", "*")
                .entity(collect)
                .build();
    }

    @GET
    @Path("users/{id}")
    @NoCache
    @Produces({MediaType.APPLICATION_JSON})
    @Encoded
    public Response getUserById(
            @PathParam("id") String id) {
        boolean admin = ensureAdmin();
        if(!admin){
            return Response.status(403)
                    .header("Access-Control-Allow-Origin", "*")

                    .build();
        }

        ReadPropertiesSingleton demo = ReadPropertiesSingleton.getInstance();
        Map<String, Object> jsonMap = new HashMap<>();
        HttpClientProvider provider = this.session.getProvider(HttpClientProvider.class);
        try {
            InputStream inputStream = provider.get(demo.getProperty("device-service.url") + "/category/administrative" +
                    "-unit/" + this.auth.getUser()
                    .getFirstAttribute(defaultAttr) + "/children");
            ObjectMapper mapper = new ObjectMapper();
            jsonMap = mapper.readValue(inputStream, Map.class);

        } catch (IOException e) {
            e.printStackTrace();
        }
        List<Map<String, Object>> listAU = (List<Map<String, Object>>) jsonMap.get("data");

        List<String> list = listAU.stream().map(el -> (String) el.get("id")).collect(Collectors.toList());
        list.add(this.auth.getUser().getFirstAttribute(defaultAttr));
        UserModel userById = this.session.users().getUserById(session.getContext().getRealm(), id);
        UserDto userDto = userMapper.modelToDto(userById);
        if (!list.contains(userDto.getAdministrativeUnit())) {
            throw new NotFoundException();
        }
        return Response.status(200)
                .header("Access-Control-Allow-Origin", "*")
                .entity(userDto)
                .build();
    }

    @GET
    @Path("groups/{id}")
    @NoCache
    @Produces({MediaType.APPLICATION_JSON})
    @Encoded
    public Response getGroupById(
            @PathParam("id") String id) {
        boolean admin = ensureAdmin();
        if(!admin){
            return Response.status(403)
                    .header("Access-Control-Allow-Origin", "*")

                    .build();
        }

        ReadPropertiesSingleton demo = ReadPropertiesSingleton.getInstance();
        Map<String, Object> jsonMap = new HashMap<>();
        HttpClientProvider provider = this.session.getProvider(HttpClientProvider.class);
        try {
            InputStream inputStream = provider.get(demo.getProperty("device-service.url") + "/category/administrative" +
                    "-unit/" + this.auth.getUser()
                    .getFirstAttribute(defaultAttr) + "/children");
            ObjectMapper mapper = new ObjectMapper();
            jsonMap = mapper.readValue(inputStream, Map.class);

        } catch (IOException e) {
            e.printStackTrace();
        }
        List<Map<String, Object>> listAU = (List<Map<String, Object>>) jsonMap.get("data");

        List<String> list = listAU.stream().map(el -> (String) el.get("id")).collect(Collectors.toList());
        list.add(this.auth.getUser().getFirstAttribute(defaultAttr));
        GroupModel groupById = this.session.groups().getGroupById(session.getContext().getRealm(), id);
        GroupDto groupDto = groupMapper.modelToDto(groupById);
        if (!list.contains(groupDto.getAdministrativeUnit())) {
            throw new NotFoundException();
        }
        return Response.status(200)
                .header("Access-Control-Allow-Origin", "*")
                .entity(groupDto)
                .build();
    }

    @DELETE
    @Path("users/{id}")
    @NoCache
    @Produces({MediaType.APPLICATION_JSON})
    @Encoded
    public Response deleteUserById(
            @PathParam("id") String id) {

        boolean admin = ensureAdmin();
        if(!admin){
            return Response.status(403)
                    .header("Access-Control-Allow-Origin", "*")

                    .build();
        }

        ReadPropertiesSingleton demo = ReadPropertiesSingleton.getInstance();
        Map<String, Object> jsonMap = new HashMap<>();
        HttpClientProvider provider = this.session.getProvider(HttpClientProvider.class);
        try {
            InputStream inputStream = provider.get(demo.getProperty("device-service.url") + "/category/administrative" +
                    "-unit/" + this.auth.getUser()
                    .getFirstAttribute(defaultAttr) + "/children");
            ObjectMapper mapper = new ObjectMapper();
            jsonMap = mapper.readValue(inputStream, Map.class);

        } catch (IOException e) {
            e.printStackTrace();
        }
        List<Map<String, Object>> listAU = (List<Map<String, Object>>) jsonMap.get("data");

        List<String> list = listAU.stream().map(el -> (String) el.get("id")).collect(Collectors.toList());
        list.add(this.auth.getUser().getFirstAttribute(defaultAttr));
        UserModel userById = this.session.users().getUserById(session.getContext().getRealm(), id);
        UserDto userDto = userMapper.modelToDto(userById);
        if (!list.contains(userDto.getAdministrativeUnit())) {
            throw new NotFoundException();
        }
        this.session.users().removeUser(this.session.getContext().getRealm(), userById);
        return Response.status(200)
                .header("Access-Control-Allow-Origin", "*")
                .entity(userDto)
                .build();
    }


    @DELETE
    @Path("groups/{id}")
    @NoCache
    @Produces({MediaType.APPLICATION_JSON})
    @Encoded
    public Response deleteGroupById(
            @PathParam("id") String id) {

        boolean admin = ensureAdmin();
        if(!admin){
            return Response.status(403)
                    .header("Access-Control-Allow-Origin", "*")

                    .build();
        }

        ReadPropertiesSingleton demo = ReadPropertiesSingleton.getInstance();
        Map<String, Object> jsonMap = new HashMap<>();
        HttpClientProvider provider = this.session.getProvider(HttpClientProvider.class);
        try {
            InputStream inputStream = provider.get(demo.getProperty("device-service.url") + "/category/administrative" +
                    "-unit/" + this.auth.getUser()
                    .getFirstAttribute(defaultAttr) + "/children");
            ObjectMapper mapper = new ObjectMapper();
            jsonMap = mapper.readValue(inputStream, Map.class);

        } catch (IOException e) {
            e.printStackTrace();
        }
        List<Map<String, Object>> listAU = (List<Map<String, Object>>) jsonMap.get("data");

        List<String> list = listAU.stream().map(el -> (String) el.get("id")).collect(Collectors.toList());
        list.add(this.auth.getUser().getFirstAttribute(defaultAttr));
        GroupModel groupById = this.session.groups().getGroupById(session.getContext().getRealm(), id);
        GroupDto groupDto = groupMapper.modelToDto(groupById);
        if (!list.contains(groupDto.getAdministrativeUnit())) {
            throw new NotFoundException();
        }
        this.session.groups().removeGroup(this.session.getContext().getRealm(), groupById);
        return Response.status(200)
                .header("Access-Control-Allow-Origin", "*")
                .entity(groupDto)
                .build();
    }

    @PUT
    @Path("users/{id}")
    @NoCache
    @Produces({MediaType.APPLICATION_JSON})
    @Encoded
    public Response updateUser(
            @RequestBody UpdateUserDto updateUserDto,
            @PathParam("id") String id
    ) {
        boolean admin = ensureAdmin();
        if(!admin){
            return Response.status(403)
                    .header("Access-Control-Allow-Origin", "*")

                    .build();
        }
        ReadPropertiesSingleton demo = ReadPropertiesSingleton.getInstance();
        Map<String, Object> jsonMap = new HashMap<>();
        HttpClientProvider provider = this.session.getProvider(HttpClientProvider.class);
        try {
            InputStream inputStream = provider.get(demo.getProperty("device-service.url") + "/category/administrative" +
                    "-unit/" + this.auth.getUser()
                    .getFirstAttribute(defaultAttr) + "/children");
            ObjectMapper mapper = new ObjectMapper();
            jsonMap = mapper.readValue(inputStream, Map.class);

        } catch (IOException e) {
            e.printStackTrace();
        }
        List<Map<String, Object>> listAU = (List<Map<String, Object>>) jsonMap.get("data");

        List<String> list = listAU.stream().map(el -> (String) el.get("id")).collect(Collectors.toList());
        list.add(this.auth.getUser().getFirstAttribute(defaultAttr));
        UserModel userById = this.session.users().getUserById(session.getContext().getRealm(), id);
        UserDto userDto = userMapper.modelToDto(userById);
        if (!list.contains(userDto.getAdministrativeUnit())) {
            throw new NotFoundException();
        }


        userById.setEmail(updateUserDto.getEmail());
        userById.setSingleAttribute(defaultAttr, (String) updateUserDto.getAttributes().get(defaultAttr));
        userById.setEnabled(updateUserDto.getEnabled());
        userById.setEmailVerified(updateUserDto.getEmailVerified());
        userById.setFirstName(updateUserDto.getFirstName());
        userById.setLastName(updateUserDto.getFirstName());
        updateUserDto.getRequiredActions().forEach(userById::addRequiredAction);
        return Response.status(200)
                .header("Access-Control-Allow-Origin", "*")
                .entity(userMapper.modelToDto(userById))
                .build();
    }

    @GET
    @Path("users/{id}/groups")
    @NoCache
    @Produces({MediaType.APPLICATION_JSON})
    @Encoded
    public Response userJoinedGroup(
            @PathParam("id") String id
    ) {
        boolean admin = ensureAdmin();
        if(!admin){
            return Response.status(403)
                    .header("Access-Control-Allow-Origin", "*")

                    .build();
        }
        ReadPropertiesSingleton demo = ReadPropertiesSingleton.getInstance();
        Map<String, Object> jsonMap = new HashMap<>();
        HttpClientProvider provider = this.session.getProvider(HttpClientProvider.class);
        try {
            InputStream inputStream = provider.get(demo.getProperty("device-service.url") + "/category/administrative" +
                    "-unit/" + this.auth.getUser()
                    .getFirstAttribute(defaultAttr) + "/children");
            ObjectMapper mapper = new ObjectMapper();
            jsonMap = mapper.readValue(inputStream, Map.class);

        } catch (IOException e) {
            e.printStackTrace();
        }
        List<Map<String, Object>> listAU = (List<Map<String, Object>>) jsonMap.get("data");

        List<String> list = listAU.stream().map(el -> (String) el.get("id")).collect(Collectors.toList());
        list.add(this.auth.getUser().getFirstAttribute(defaultAttr));
        UserModel userById = this.session.users().getUserById(session.getContext().getRealm(), id);
        UserDto userDto = userMapper.modelToDto(userById);
        if (!list.contains(userDto.getAdministrativeUnit())) {
            throw new NotFoundException();
        }


        List<GroupDto> groupDtos = userById.getGroupsStream().map(groupMapper::modelToDto).collect(Collectors.toList());
        return Response.status(200)
                .header("Access-Control-Allow-Origin", "*")
                .entity(groupDtos)
                .build();
    }

    @PUT
    @Path("users/{userId}/groups/{groupId}")
    @NoCache
    @Produces({MediaType.APPLICATION_JSON})
    @Encoded
    public Response addUserToGroup(
            @PathParam("userId") String userId,
            @PathParam("groupId") String groupId
    ) {
        boolean admin = ensureAdmin();
        if(!admin){
            return Response.status(403)
                    .header("Access-Control-Allow-Origin", "*")

                    .build();
        }
        ReadPropertiesSingleton demo = ReadPropertiesSingleton.getInstance();
        Map<String, Object> jsonMap = new HashMap<>();
        HttpClientProvider provider = this.session.getProvider(HttpClientProvider.class);
        try {
            InputStream inputStream = provider.get(demo.getProperty("device-service.url") + "/category/administrative" +
                    "-unit/" + this.auth.getUser()
                    .getFirstAttribute(defaultAttr) + "/children");
            ObjectMapper mapper = new ObjectMapper();
            jsonMap = mapper.readValue(inputStream, Map.class);

        } catch (IOException e) {
            e.printStackTrace();
        }
        List<Map<String, Object>> listAU = (List<Map<String, Object>>) jsonMap.get("data");

        List<String> list = listAU.stream().map(el -> (String) el.get("id")).collect(Collectors.toList());
        list.add(this.auth.getUser().getFirstAttribute(defaultAttr));
        UserModel userById = this.session.users().getUserById(session.getContext().getRealm(), userId);
        UserDto userDto = userMapper.modelToDto(userById);
        if (!list.contains(userDto.getAdministrativeUnit())) {
            throw new NotFoundException();
        }
        GroupModel groupById = this.session.groups().getGroupById(session.getContext().getRealm(), groupId);
        GroupDto groupDto = groupMapper.modelToDto(groupById);
        if (!list.contains(groupDto.getAdministrativeUnit())) {
            throw new NotFoundException();
        }


        userById.joinGroup(groupById);
        return Response.status(200)
                .header("Access-Control-Allow-Origin", "*")
                .entity(userDto)
                .build();
    }

    @DELETE
    @Path("users/{userId}/groups/{groupId}")
    @NoCache
    @Produces({MediaType.APPLICATION_JSON})
    @Encoded
    public Response removeUserToGroup(
            @PathParam("userId") String userId,
            @PathParam("groupId") String groupId
    ) {
        boolean admin = ensureAdmin();
        if(!admin){
            return Response.status(403)
                    .header("Access-Control-Allow-Origin", "*")

                    .build();
        }
        ReadPropertiesSingleton demo = ReadPropertiesSingleton.getInstance();
        Map<String, Object> jsonMap = new HashMap<>();
        HttpClientProvider provider = this.session.getProvider(HttpClientProvider.class);
        try {
            InputStream inputStream = provider.get(demo.getProperty("device-service.url") + "/category/administrative" +
                    "-unit/" + this.auth.getUser()
                    .getFirstAttribute(defaultAttr) + "/children");
            ObjectMapper mapper = new ObjectMapper();
            jsonMap = mapper.readValue(inputStream, Map.class);

        } catch (IOException e) {
            e.printStackTrace();
        }
        List<Map<String, Object>> listAU = (List<Map<String, Object>>) jsonMap.get("data");

        List<String> list = listAU.stream().map(el -> (String) el.get("id")).collect(Collectors.toList());
        list.add(this.auth.getUser().getFirstAttribute(defaultAttr));
        UserModel userById = this.session.users().getUserById(session.getContext().getRealm(), userId);
        UserDto userDto = userMapper.modelToDto(userById);
        if (!list.contains(userDto.getAdministrativeUnit())) {
            throw new NotFoundException();
        }
        GroupModel groupById = this.session.groups().getGroupById(session.getContext().getRealm(), groupId);
        GroupDto groupDto = groupMapper.modelToDto(groupById);
        if (!list.contains(groupDto.getAdministrativeUnit())) {
            throw new NotFoundException();
        }


        userById.leaveGroup(groupById);
        return Response.status(200)
                .header("Access-Control-Allow-Origin", "*")
                .entity(userDto)
                .build();
    }

    @PUT
    @Path("groups/{id}")
    @NoCache
    @Produces({MediaType.APPLICATION_JSON})
    @Encoded
    public Response updateGroup(
            @RequestBody UpdateGroupDto updateGroupDto,
            @PathParam("id") String id
    ) {
        boolean admin = ensureAdmin();
        if(!admin){
            return Response.status(403)
                    .header("Access-Control-Allow-Origin", "*")

                    .build();
        }
        ReadPropertiesSingleton demo = ReadPropertiesSingleton.getInstance();
        Map<String, Object> jsonMap = new HashMap<>();
        HttpClientProvider provider = this.session.getProvider(HttpClientProvider.class);
        try {
            InputStream inputStream = provider.get(demo.getProperty("device-service.url") + "/category/administrative" +
                    "-unit/" + this.auth.getUser()
                    .getFirstAttribute(defaultAttr) + "/children");
            ObjectMapper mapper = new ObjectMapper();
            jsonMap = mapper.readValue(inputStream, Map.class);

        } catch (IOException e) {
            e.printStackTrace();
        }
        List<Map<String, Object>> listAU = (List<Map<String, Object>>) jsonMap.get("data");

        List<String> list = listAU.stream().map(el -> (String) el.get("id")).collect(Collectors.toList());
        list.add(this.auth.getUser().getFirstAttribute(defaultAttr));
        GroupModel groupById = this.session.groups().getGroupById(session.getContext().getRealm(), id);
        GroupDto groupDto = groupMapper.modelToDto(groupById);
        if (!list.contains(groupDto.getAdministrativeUnit())) {
            throw new NotFoundException();
        }


        groupById.setName(updateGroupDto.getName());
        return Response.status(200)
                .header("Access-Control-Allow-Origin", "*")
                .entity(groupMapper.modelToDto(groupById))
                .build();
    }

    @GET
    @Path("groups/{id}/members")
    @NoCache
    @Produces({MediaType.APPLICATION_JSON})
    @Encoded
    public Response getGroupMembers(
            @PathParam("id") String id
    ) {
        boolean admin = ensureAdmin();
        if(!admin){
            return Response.status(403)
                    .header("Access-Control-Allow-Origin", "*")

                    .build();
        }
        ReadPropertiesSingleton demo = ReadPropertiesSingleton.getInstance();
        Map<String, Object> jsonMap = new HashMap<>();
        HttpClientProvider provider = this.session.getProvider(HttpClientProvider.class);
        try {
            InputStream inputStream = provider.get(demo.getProperty("device-service.url") + "/category/administrative" +
                    "-unit/" + this.auth.getUser()
                    .getFirstAttribute(defaultAttr) + "/children");
            ObjectMapper mapper = new ObjectMapper();
            jsonMap = mapper.readValue(inputStream, Map.class);

        } catch (IOException e) {
            e.printStackTrace();
        }
        List<Map<String, Object>> listAU = (List<Map<String, Object>>) jsonMap.get("data");

        List<String> list = listAU.stream().map(el -> (String) el.get("id")).collect(Collectors.toList());
        list.add(this.auth.getUser().getFirstAttribute(defaultAttr));
        GroupModel groupById = this.session.groups().getGroupById(session.getContext().getRealm(), id);
        GroupDto groupDto = groupMapper.modelToDto(groupById);
        if (!list.contains(groupDto.getAdministrativeUnit())) {
            throw new NotFoundException();
        }


        List<UserDto> userDtos = this.session.users().getGroupMembersStream(session.getContext().getRealm(),
                groupById).map(userMapper::modelToDto).collect(Collectors.toList());
        return Response.status(200)
                .header("Access-Control-Allow-Origin", "*")
                .entity(userDtos)
                .build();
    }

    @PUT
    @Path("users/{id}/reset-password")
    @NoCache
    @Produces({MediaType.APPLICATION_JSON})
    @Encoded
    public Response resetPassword(
            @PathParam("id") String id,
            @RequestBody ChangePasswordDto changePasswordDto
    ) {
        boolean admin = ensureAdmin();
        if(!admin){
            return Response.status(403)
                    .header("Access-Control-Allow-Origin", "*")

                    .build();
        }
        ReadPropertiesSingleton demo = ReadPropertiesSingleton.getInstance();
        Map<String, Object> jsonMap = new HashMap<>();
        HttpClientProvider provider = this.session.getProvider(HttpClientProvider.class);
        try {
            InputStream inputStream = provider.get(demo.getProperty("device-service.url") + "/category/administrative" +
                    "-unit/" + this.auth.getUser()
                    .getFirstAttribute(defaultAttr) + "/children");
            ObjectMapper mapper = new ObjectMapper();
            jsonMap = mapper.readValue(inputStream, Map.class);

        } catch (IOException e) {
            e.printStackTrace();
        }
        List<Map<String, Object>> listAU = (List<Map<String, Object>>) jsonMap.get("data");

        List<String> list = listAU.stream().map(el -> (String) el.get("id")).collect(Collectors.toList());
        list.add(this.auth.getUser().getFirstAttribute(defaultAttr));
        System.out.println(id);
        UserModel userById = this.session.users().getUserById(session.getContext().getRealm(), id);
        UserDto userDto = userMapper.modelToDto(userById);
        if (!list.contains(userDto.getAdministrativeUnit())) {
            throw new NotFoundException();
        }

        this.session.userCredentialManager().updateCredential(this.session.getContext().getRealm(), userById,
                UserCredentialModel.password(changePasswordDto.getValue(), false));
        if (changePasswordDto.getTemporary()) {
            userById.addRequiredAction(UserModel.RequiredAction.UPDATE_PASSWORD);
        }
        return Response.status(200)
                .header("Access-Control-Allow-Origin", "*")
                .entity(userDto)
                .build();
    }

    @POST
    @Path("users/{id}/role-mappings/realm")
    @NoCache
    @Produces({MediaType.APPLICATION_JSON})
    @Encoded
    public Response addRoles(
            @PathParam("id") String id,
            @RequestBody ChangeRolesDto changeRolesDto
    ) {
        boolean admin = ensureAdmin();
        if(!admin){
            return Response.status(403)
                    .header("Access-Control-Allow-Origin", "*")

                    .build();
        }
        ReadPropertiesSingleton demo = ReadPropertiesSingleton.getInstance();
        Map<String, Object> jsonMap = new HashMap<>();
        HttpClientProvider provider = this.session.getProvider(HttpClientProvider.class);
        try {
            InputStream inputStream = provider.get(demo.getProperty("device-service.url") + "/category/administrative" +
                    "-unit/" + this.auth.getUser()
                    .getFirstAttribute(defaultAttr) + "/children");
            ObjectMapper mapper = new ObjectMapper();
            jsonMap = mapper.readValue(inputStream, Map.class);

        } catch (IOException e) {
            e.printStackTrace();
        }
        List<Map<String, Object>> listAU = (List<Map<String, Object>>) jsonMap.get("data");

        List<String> list = listAU.stream().map(el -> (String) el.get("id")).collect(Collectors.toList());
        list.add(this.auth.getUser().getFirstAttribute(defaultAttr));
        UserModel userById = this.session.users().getUserById(session.getContext().getRealm(), id);
        UserDto userDto = userMapper.modelToDto(userById);
        if (!list.contains(userDto.getAdministrativeUnit())) {
            throw new NotFoundException();
        }
        changeRolesDto.getRoles().forEach(role -> {
            userById.grantRole(this.session.getContext().getRealm().getRole(role));
        });
        return Response.status(200)
                .header("Access-Control-Allow-Origin", "*")
                .entity(userDto)
                .build();

    }

    @POST
    @Path("groups/{id}/role-mappings/realm")
    @NoCache
    @Produces({MediaType.APPLICATION_JSON})
    @Encoded
    public Response addRolesToGroup(
            @PathParam("id") String id,
            @RequestBody ChangeRolesDto changeRolesDto
    ) {
        boolean admin = ensureAdmin();
        if(!admin){
            return Response.status(403)
                    .header("Access-Control-Allow-Origin", "*")

                    .build();
        }
        ReadPropertiesSingleton demo = ReadPropertiesSingleton.getInstance();
        Map<String, Object> jsonMap = new HashMap<>();
        HttpClientProvider provider = this.session.getProvider(HttpClientProvider.class);
        try {
            InputStream inputStream = provider.get(demo.getProperty("device-service.url") + "/category/administrative" +
                    "-unit/" + this.auth.getUser()
                    .getFirstAttribute(defaultAttr) + "/children");
            ObjectMapper mapper = new ObjectMapper();
            jsonMap = mapper.readValue(inputStream, Map.class);

        } catch (IOException e) {
            e.printStackTrace();
        }
        List<Map<String, Object>> listAU = (List<Map<String, Object>>) jsonMap.get("data");

        List<String> list = listAU.stream().map(el -> (String) el.get("id")).collect(Collectors.toList());
        list.add(this.auth.getUser().getFirstAttribute(defaultAttr));
        GroupModel groupModel = this.session.groups().getGroupById(session.getContext().getRealm(), id);
        GroupDto groupDto = groupMapper.modelToDto(groupModel);
        if (!list.contains(groupDto.getAdministrativeUnit())) {
            throw new NotFoundException();
        }
        changeRolesDto.getRoles().forEach(role -> {
            groupModel.grantRole(this.session.getContext().getRealm().getRole(role));
        });
        return Response.status(200)
                .header("Access-Control-Allow-Origin", "*")
                .entity(groupDto)
                .build();

    }

    @DELETE
    @Path("users/{id}/role-mappings/realm")
    @NoCache
    @Produces({MediaType.APPLICATION_JSON})
    @Encoded
    public Response removeRoles(
            @PathParam("id") String id,
            @RequestBody ChangeRolesDto changeRolesDto
    ) {
        boolean admin = ensureAdmin();
        if(!admin){
            return Response.status(403)
                    .header("Access-Control-Allow-Origin", "*")

                    .build();
        }
        ReadPropertiesSingleton demo = ReadPropertiesSingleton.getInstance();
        Map<String, Object> jsonMap = new HashMap<>();
        HttpClientProvider provider = this.session.getProvider(HttpClientProvider.class);
        try {
            InputStream inputStream = provider.get(demo.getProperty("device-service.url") + "/category/administrative" +
                    "-unit/" + this.auth.getUser()
                    .getFirstAttribute(defaultAttr) + "/children");
            ObjectMapper mapper = new ObjectMapper();
            jsonMap = mapper.readValue(inputStream, Map.class);

        } catch (IOException e) {
            e.printStackTrace();
        }
        List<Map<String, Object>> listAU = (List<Map<String, Object>>) jsonMap.get("data");

        List<String> list = listAU.stream().map(el -> (String) el.get("id")).collect(Collectors.toList());
        list.add(this.auth.getUser().getFirstAttribute(defaultAttr));
        GroupModel groupModel = this.session.groups().getGroupById(session.getContext().getRealm(), id);
        GroupDto groupDto = groupMapper.modelToDto(groupModel);
        if (!list.contains(groupDto.getAdministrativeUnit())) {
            throw new NotFoundException();
        }
        changeRolesDto.getRoles().forEach(role -> {
            groupModel.deleteRoleMapping(this.session.getContext().getRealm().getRole(role));
        });
        return Response.status(200)
                .header("Access-Control-Allow-Origin", "*")
                .entity(groupDto)
                .build();

    }

    @DELETE
    @Path("groups/{id}/role-mappings/realm")
    @NoCache
    @Produces({MediaType.APPLICATION_JSON})
    @Encoded
    public Response removeRolesFromGroup(
            @PathParam("id") String id,
            @RequestBody ChangeRolesDto changeRolesDto
    ) {
        boolean admin = ensureAdmin();
        if(!admin){
            return Response.status(403)
                    .header("Access-Control-Allow-Origin", "*")

                    .build();
        }
        ReadPropertiesSingleton demo = ReadPropertiesSingleton.getInstance();
        Map<String, Object> jsonMap = new HashMap<>();
        HttpClientProvider provider = this.session.getProvider(HttpClientProvider.class);
        try {
            InputStream inputStream = provider.get(demo.getProperty("device-service.url") + "/category/administrative" +
                    "-unit/" + this.auth.getUser()
                    .getFirstAttribute(defaultAttr) + "/children");
            ObjectMapper mapper = new ObjectMapper();
            jsonMap = mapper.readValue(inputStream, Map.class);

        } catch (IOException e) {
            e.printStackTrace();
        }
        List<Map<String, Object>> listAU = (List<Map<String, Object>>) jsonMap.get("data");

        List<String> list = listAU.stream().map(el -> (String) el.get("id")).collect(Collectors.toList());
        list.add(this.auth.getUser().getFirstAttribute(defaultAttr));
        GroupModel groupById = this.session.groups().getGroupById(session.getContext().getRealm(), id);
        GroupDto groupDto = groupMapper.modelToDto(groupById);
        if (!list.contains(groupDto.getAdministrativeUnit())) {
            throw new NotFoundException();
        }
        changeRolesDto.getRoles().forEach(role -> {
            groupById.deleteRoleMapping(this.session.getContext().getRealm().getRole(role));
        });
        return Response.status(200)
                .header("Access-Control-Allow-Origin", "*")
                .entity(groupDto)
                .build();

    }

    @GET
    @Path("users/{id}/role-mappings/realm/available")
    @NoCache
    @Produces({MediaType.APPLICATION_JSON})
    @Encoded
    public Response getAvailableRoles(
            @PathParam("id") String id
    ) {
        boolean admin = ensureAdmin();
        if(!admin){
            return Response.status(403)
                    .header("Access-Control-Allow-Origin", "*")

                    .build();
        }
        ReadPropertiesSingleton demo = ReadPropertiesSingleton.getInstance();
        Map<String, Object> jsonMap = new HashMap<>();
        HttpClientProvider provider = this.session.getProvider(HttpClientProvider.class);
        try {
            InputStream inputStream = provider.get(demo.getProperty("device-service.url") + "/category/administrative" +
                    "-unit/" + this.auth.getUser()
                    .getFirstAttribute(defaultAttr) + "/children");
            ObjectMapper mapper = new ObjectMapper();
            jsonMap = mapper.readValue(inputStream, Map.class);

        } catch (IOException e) {
            e.printStackTrace();
        }
        List<Map<String, Object>> listAU = (List<Map<String, Object>>) jsonMap.get("data");

        List<String> list = listAU.stream().map(el -> (String) el.get("id")).collect(Collectors.toList());
        list.add(this.auth.getUser().getFirstAttribute(defaultAttr));
        UserModel userById = this.session.users().getUserById(session.getContext().getRealm(), id);
        UserDto userDto = userMapper.modelToDto(userById);
        if (!list.contains(userDto.getAdministrativeUnit())) {
            throw new NotFoundException();
        }
        List<RoleDto> collect =
                this.session.getContext().getRealm().getRolesStream().filter(role -> !userById.hasRole(role)).map(roleMapper::modelToDto).collect(Collectors.toList());

        return Response.status(200)
                .header("Access-Control-Allow-Origin", "*")
                .entity(collect)
                .build();
    }

    @GET
    @Path("groups/{id}/role-mappings/realm/available")
    @NoCache
    @Produces({MediaType.APPLICATION_JSON})
    @Encoded
    public Response getAvailableRolesOfGroup(
            @PathParam("id") String id
    ) {
        boolean admin = ensureAdmin();
        if(!admin){
            return Response.status(403)
                    .header("Access-Control-Allow-Origin", "*")

                    .build();
        }
        ReadPropertiesSingleton demo = ReadPropertiesSingleton.getInstance();
        Map<String, Object> jsonMap = new HashMap<>();
        HttpClientProvider provider = this.session.getProvider(HttpClientProvider.class);
        try {
            InputStream inputStream = provider.get(demo.getProperty("device-service.url") + "/category/administrative" +
                    "-unit/" + this.auth.getUser()
                    .getFirstAttribute(defaultAttr) + "/children");
            ObjectMapper mapper = new ObjectMapper();
            jsonMap = mapper.readValue(inputStream, Map.class);

        } catch (IOException e) {
            e.printStackTrace();
        }
        List<Map<String, Object>> listAU = (List<Map<String, Object>>) jsonMap.get("data");

        List<String> list = listAU.stream().map(el -> (String) el.get("id")).collect(Collectors.toList());
        list.add(this.auth.getUser().getFirstAttribute(defaultAttr));
        GroupModel groupById = this.session.groups().getGroupById(session.getContext().getRealm(), id);
        GroupDto groupDto = groupMapper.modelToDto(groupById);
        if (!list.contains(groupDto.getAdministrativeUnit())) {
            throw new NotFoundException();
        }
        List<RoleDto> collect =
                this.session.getContext().getRealm().getRolesStream().filter(role -> !groupById.hasRole(role)).map(roleMapper::modelToDto).collect(Collectors.toList());

        return Response.status(200)
                .header("Access-Control-Allow-Origin", "*")
                .entity(collect)
                .build();

    }

    @GET
    @Path("users/{id}/role-mappings/realm")
    @NoCache
    @Produces({MediaType.APPLICATION_JSON})
    @Encoded
    public Response getUserRoles(
            @PathParam("id") String id
    ) {
        boolean admin = ensureAdmin();
        if(!admin){
            return Response.status(403)
                    .header("Access-Control-Allow-Origin", "*")

                    .build();
        }
        ReadPropertiesSingleton demo = ReadPropertiesSingleton.getInstance();
        Map<String, Object> jsonMap = new HashMap<>();
        HttpClientProvider provider = this.session.getProvider(HttpClientProvider.class);
        try {
            InputStream inputStream = provider.get(demo.getProperty("device-service.url") + "/category/administrative" +
                    "-unit/" + this.auth.getUser()
                    .getFirstAttribute(defaultAttr) + "/children");
            ObjectMapper mapper = new ObjectMapper();
            jsonMap = mapper.readValue(inputStream, Map.class);

        } catch (IOException e) {
            e.printStackTrace();
        }
        List<Map<String, Object>> listAU = (List<Map<String, Object>>) jsonMap.get("data");

        List<String> list = listAU.stream().map(el -> (String) el.get("id")).collect(Collectors.toList());
        list.add(this.auth.getUser().getFirstAttribute(defaultAttr));
        UserModel userById = this.session.users().getUserById(session.getContext().getRealm(), id);
        UserDto userDto = userMapper.modelToDto(userById);
        if (!list.contains(userDto.getAdministrativeUnit())) {
            throw new NotFoundException();
        }
        List<RoleDto> collect =
                userById.getRoleMappingsStream().map(roleMapper::modelToDto).collect(Collectors.toList());

        return Response.status(200)
                .header("Access-Control-Allow-Origin", "*")
                .entity(collect)
                .build();

    }

    @GET
    @Path("groups/{id}/role-mappings/realm")
    @NoCache
    @Produces({MediaType.APPLICATION_JSON})
    @Encoded
    public Response getGroupRoles(
            @PathParam("id") String id
    ) {
        boolean admin = ensureAdmin();
        if(!admin){
            return Response.status(403)
                    .header("Access-Control-Allow-Origin", "*")

                    .build();
        }
        ReadPropertiesSingleton demo = ReadPropertiesSingleton.getInstance();
        Map<String, Object> jsonMap = new HashMap<>();
        HttpClientProvider provider = this.session.getProvider(HttpClientProvider.class);
        try {
            InputStream inputStream = provider.get(demo.getProperty("device-service.url") + "/category/administrative" +
                    "-unit/" + this.auth.getUser()
                    .getFirstAttribute(defaultAttr) + "/children");
            ObjectMapper mapper = new ObjectMapper();
            jsonMap = mapper.readValue(inputStream, Map.class);

        } catch (IOException e) {
            e.printStackTrace();
        }
        List<Map<String, Object>> listAU = (List<Map<String, Object>>) jsonMap.get("data");

        List<String> list = listAU.stream().map(el -> (String) el.get("id")).collect(Collectors.toList());
        list.add(this.auth.getUser().getFirstAttribute(defaultAttr));
        GroupModel groupById = this.session.groups().getGroupById(session.getContext().getRealm(), id);
        GroupDto groupDto = groupMapper.modelToDto(groupById);
        if (!list.contains(groupDto.getAdministrativeUnit())) {
            throw new NotFoundException();
        }
        List<RoleDto> collect =
                groupById.getRoleMappingsStream().map(roleMapper::modelToDto).collect(Collectors.toList());

        return Response.status(200)
                .header("Access-Control-Allow-Origin", "*")
                .entity(collect)
                .build();

    }


    @POST
    @Path("users")
    @NoCache
    @Produces({MediaType.APPLICATION_JSON})
    @Encoded
    public Response createNewUser(
            @RequestBody CreateNewUserDto createNewUserDto

    ) {
        boolean admin = ensureAdmin();
        if(!admin){
            return Response.status(403)
                    .header("Access-Control-Allow-Origin", "*")

                    .build();
        }
//        Map<String, Object> jsonMap = new HashMap<>();
//
//        HttpClientProvider provider = this.session.getProvider(HttpClientProvider.class);
//        try {
//            InputStream inputStream = provider.get("https://dev-api.vnptics" +
//                    ".vn/device-service/v1/category/administrative-unit/" + this.auth.getUser()
//                    .getFirstAttribute(defaultAttr) + "/children");
//            ObjectMapper mapper = new ObjectMapper();
//            jsonMap = mapper.readValue(inputStream, Map.class);
//
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        List<Map<String,Object>> listAU = (List<Map<String, Object>>) jsonMap.get("data");
//        List<String> list = listAU.stream().map(el->(String) el.get("id")).collect(Collectors.toList());

        UserModel userModel = this.session.users().addUser(session.getContext().getRealm(),
                createNewUserDto.getUsername());
        userModel.setEmail(createNewUserDto.getEmail());
        userModel.setSingleAttribute(defaultAttr, (String) createNewUserDto.getAttributes().get(defaultAttr));
        userModel.setEnabled(createNewUserDto.getEnabled());
        userModel.setEmailVerified(createNewUserDto.getEmailVerified());
        userModel.setFirstName(createNewUserDto.getFirstName());
        userModel.setLastName(createNewUserDto.getFirstName());
        createNewUserDto.getRequiredActions().forEach(userModel::addRequiredAction);


        return Response.status(200)
                .header("Access-Control-Allow-Origin", "*")
                .entity(createNewUserDto)
                .build();
    }

    @POST
    @Path("groups")
    @NoCache
    @Produces({MediaType.APPLICATION_JSON})
    @Encoded
    public Response createNewGroup(
            @RequestBody CreateNewGroupDto createNewGroupDto

    ) {
        boolean admin = ensureAdmin();
        if(!admin){
            return Response.status(403)
                    .header("Access-Control-Allow-Origin", "*")

                    .build();
        }
        GroupModel groupModel = this.session.groups().createGroup(session.getContext().getRealm(),
                createNewGroupDto.getName());
        groupModel.setSingleAttribute(defaultAttr, (String) createNewGroupDto.getAttributes().get(defaultAttr));



        return Response.status(200)
                .header("Access-Control-Allow-Origin", "*")
                .entity(createNewGroupDto)
                .build();
    }

    @GET
    @Path("required-action")
    @NoCache
    @Produces({MediaType.APPLICATION_JSON})
    @Encoded
    public Response getRequiredAction() {
        boolean admin = ensureAdmin();
        if(!admin){
            return Response.status(403)
                    .header("Access-Control-Allow-Origin", "*")

                    .build();
        }
        List<RequiredActionProviderModel> collect =
                this.session.getContext().getRealm().getRequiredActionProvidersStream().collect(Collectors.toList());
        return Response.status(200)
                .header("Access-Control-Allow-Origin", "*")
                .entity(collect)
                .build();
    }


    private AuthenticationManager.AuthResult resolveAuthentication(KeycloakSession session) {
        AuthenticationManager.AuthResult authResult =
                new AppAuthManager.BearerTokenAuthenticator(session).authenticate();
        ClientModel realManagementModel = session.getContext().getClient();

        return authResult;
    }

    private boolean ensureAdmin() {
        if(this.auth == null){
            return false;
        }
        List<RoleDto> collect =
                this.auth.getUser().getRoleMappingsStream().map(roleMapper::modelToDto).collect(Collectors.toList());
        boolean admin = collect.stream().anyMatch(roleDto -> roleDto.getName().equals("custom-user-management"));
        return admin;


    }



}